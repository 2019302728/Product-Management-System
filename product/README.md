# 商品管理后端（L0 简单分片版）

按照技术文档实现的商品发布和管理后端服务。只实现商品发布、查询、编辑、软删除、上架/下架/处罚等基础功能；支持实物、虚拟、商业化素材三种商品类型；按 `creator_id % 2` 将商品主表数据路由到 `product_0` 或 `product_1`。

## 技术栈

- **Spring Boot 3.2.5** / Java 17
- **MyBatis 3.0.3**（XML Mapper）
- **MySQL 8.x**（应用层分表，不引入分布式中间件）
- **Caffeine 3.1.8**（本地缓存商品元数据）

按文档要求不引入 Redis、MQ、Elasticsearch 或分布式数据库中间件。

## 工程结构

```
product
├── controller/ProductController       # 8 个 REST 端点
├── service
│   ├── ProductService                # 核心业务逻辑（事务 + 缓存）
│   └── ProductValidator              # 字段校验、HTML 净化、图片校验
├── sharding
│   ├── ProductShard                  # 分片枚举
│   ├── ProductShardRouter            # creator_id % 2 路由
│   └── SnowflakeIdGenerator          # 全局唯一 product_id
├── repository
│   ├── ProductShard0Mapper           # product_0 分表
│   ├── ProductShard1Mapper           # product_1 分表
│   ├── ProductImageMapper            # 共享副图表
│   ├── ProductExtensionMapper        # 三张扩展表统一入口
│   └── typehandler/LongListJsonTypeHandler  # List<Long> <-> JSON
├── config/CaffeineConfig             # 缓存配置与 key 构造
├── dto/                              # 请求/响应 DTO
├── model/                            # 实体
├── common/                           # 错误码、异常、响应包装、HTML 净化、图片校验
└── context/LoginContext              # 登录上下文（L0 模拟）
```

## 路由规则

```
creator_id 为偶数 -> product_0
creator_id 为奇数 -> product_1
```

路由函数：

```java
ProductShard route(long creatorId) {
    return Math.floorMod(creatorId, 2) == 0
            ? ProductShard.PRODUCT_0
            : ProductShard.PRODUCT_1;
}
```

路由必须使用 `creator_id`，不能用 `product_id`。Snowflake product_id 的奇偶不保证与创建人一致。

**安全约束**：SQL 表名不动态拼接。每张分表对应独立的 Mapper 与 XML 语句，Service 根据枚举选择具体方法调用，避免 SQL 注入。

## API 一览

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/products` | 发布商品，creatorId 从登录上下文取得 |
| GET | `/api/v1/creators/{creatorId}/products` | 查询某创建人的商品，只查一张分表 |
| GET | `/api/v1/creators/{creatorId}/products/{productId}` | 查询商品元数据和类型扩展 |
| GET | `/api/v1/creators/{creatorId}/products/{productId}/images` | 单独查询主图和副图 Base64 |
| PUT | `/api/v1/creators/{creatorId}/products/{productId}` | 编辑商品 |
| PATCH | `/api/v1/creators/{creatorId}/products/{productId}/status` | 修改状态 |
| DELETE | `/api/v1/creators/{creatorId}/products/{productId}` | 软删除商品 |
| GET | `/api/v1/admin/products` | 管理员跨创建人列表，UNION ALL |

### 登录上下文（L0 模拟）

L0 通过请求头模拟登录态：

- `X-User-Id`：当前登录用户 ID（creatorId）
- `X-Admin: true`：标识当前请求是管理员

管理员才能访问 `/api/v1/admin/products` 和设置 `status=3`（处罚）。

### 发布请求示例

```json
{
  "productType": 1,
  "title": "无线蓝牙耳机 Pro",
  "shortTitle": "旗舰降噪耳机",
  "price": 29900,
  "detail": "<p>商品详情</p>",
  "status": 1,
  "mainImageBase64": "data:image/jpeg;base64,/9j/4AAQ...",
  "subImageBase64List": [
    "data:image/jpeg;base64,/9j/4AAQ..."
  ],
  "physicalExt": {
    "stock": 100,
    "deliveryType": 2,
    "refundRule": 1
  }
}
```

`productType=2` 时使用 `virtualExt`，`productType=3` 时使用 `materialExt`。请求只能携带与商品类型匹配的一组扩展字段。

### 字段校验规则

| 字段 | 规则 |
|------|------|
| productType | 必填，1/2/3；创建后不允许修改 |
| title | 必填，trim 后 1-60 字 |
| shortTitle | 选填，最多 120 字 |
| price | 必填，以分为单位的整数，> 0 |
| detail | 选填，去除 HTML 标签后最多 2000 字；过滤 script/style |
| status | 1=上架、2=下架；3=处罚仅管理员 |
| mainImageBase64 | 必填，解码后 ≤ 2MB |
| subImageBase64List | 选填，最多 5 张，每张 ≤ 2MB |

类型扩展：

- 实物：`stock >= 0`，`deliveryType` ∈ {1,2,3}，`refundRule` ∈ {1,2}
- 虚拟：`validDays >= 0`，`verificationType` ∈ {1,2,3}，`storeIds` 非空数组
- 素材：`mediaType` ∈ {1,2,3}，`mediaUrl` 必填，`licenseEndTime` 为空或 > 当前时间

## Base64 图片规则

- 主图必填，副图最多 5 张
- 仅允许 JPEG / PNG / WebP，data URL 形式：`data:image/...;base64,...`
- 后端必须解码 Base64，按解码后真实字节数判断 2MB 上限
- 检查文件 magic bytes，不只信任 Data URL 中声明的 MIME
- 列表查询禁止选择 `image_base64`，图片通过独立接口获取

## Caffeine 缓存

```java
Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .recordStats()
        .build();
```

- 缓存 key：`product:metadata:{creatorId}:{productId}`
- 缓存值：`ProductMetadata`（元数据 + 类型扩展，不含图片 Base64）
- 命中流程：缓存命中直接返回；未命中则按 `creatorId` 路由到 MySQL，回填缓存
- 失效：编辑、状态修改、软删除事务**提交后**触发 `cache.invalidate()`；事务回滚时不失效缓存

L0 默认单实例部署；多实例间的缓存失效不在本方案范围。

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_HOST` | MySQL 主机 | 127.0.0.1 |
| `DB_PORT` | MySQL 端口 | 3306 |
| `DB_NAME` | 数据库名 | gifshow |
| `DB_USER` | MySQL 用户名 | root |
| `DB_PASSWORD` | MySQL 密码 | root |
| `SNOWFLAKE_WORKER_ID` | Snowflake workerId | 1 |
| `SNOWFLAKE_DATACENTER_ID` | Snowflake datacenterId | 1 |

密码不提交到 Git。

## 准备数据库

1. 创建数据库 `gifshow`：

   ```sql
   CREATE DATABASE gifshow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 执行 DDL（位于 `src/main/resources/schema.sql`）：

   ```bash
   mysql -h 127.0.0.1 -u root -p gifshow < product/src/main/resources/schema.sql
   ```

3. 调整 MySQL `max_allowed_packet` 为至少 32MB：

   ```sql
   SET GLOBAL max_allowed_packet = 32 * 1024 * 1024;
   ```

## 本地启动

```bash
cd product
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=gifshow
export DB_USER=root
export DB_PASSWORD=your_password
mvn spring-boot:run
```

服务监听 `http://localhost:8080`。

## 调用示例

发布商品（creator_id=1002 会路由到 `product_0`）：

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1002" \
  -d '{
    "productType": 1,
    "title": "无线蓝牙耳机 Pro",
    "shortTitle": "旗舰降噪耳机",
    "price": 29900,
    "detail": "<p>商品详情</p>",
    "status": 1,
    "mainImageBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmBIQAAAABJRU5ErkJggg==",
    "physicalExt": { "stock": 100, "deliveryType": 2, "refundRule": 1 }
  }'
```

查询创建人的商品列表：

```bash
curl http://localhost:8080/api/v1/creators/1002/products?page=1&size=20
```

管理员跨创建人列表：

```bash
curl -H "X-Admin: true" http://localhost:8080/api/v1/admin/products?page=1&size=20
```

## 测试清单

以下场景由代码结构保证，建议补充自动化测试覆盖：

- `creator_id=1002` 只写入/查询 `product_0`
- `creator_id=1001` 只写入/查询 `product_1`
- 使用不同奇偶的 `product_id` 验证路由仍只由 `creator_id` 决定
- 主表成功、副图或扩展表写入失败时，整个事务回滚
- 实物/虚拟/素材商品只写入对应扩展表
- Base64 损坏、MIME 伪造、2MB 边界、副图 5/6 张
- 列表 SQL 不读取 Base64，跨分表列表正确合并并排序
- 元数据连续查询第二次命中 Caffeine，不查询 MySQL
- 编辑、状态修改和软删除后缓存正确失效
- 所有查询均排除 `deleted_at IS NOT NULL` 的商品

## 待确认项（与原文档一致）

- 原始需求将库存、发货方式和退货规则视为公共字段，当前 SQL 只存在于实物扩展表。如果虚拟和素材也要求这三项，需要调整 DDL。
- `creator_id` 是否始终能从登录上下文或管理端请求中取得。如果只知道 `product_id`，当前分片规则无法直接路由。
