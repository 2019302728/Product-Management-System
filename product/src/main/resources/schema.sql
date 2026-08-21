-- ============================================================================
-- 商品管理后端（L0 简单分片版）DDL
-- 数据库：gifshow
-- 说明：product_0 / product_1 仍在同一 MySQL 库中，属于"应用层分表"，
--      主表、副图表、扩展表可在同一个 MySQL 本地事务中保证一致性。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- product_0：creator_id 为偶数时路由到此表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `product_0`;
CREATE TABLE `product_0` (
    `product_id`   BIGINT       NOT NULL COMMENT '商品 ID（Snowflake 全局唯一）',
    `creator_id`   BIGINT       NOT NULL COMMENT '创建人 ID，路由依据 creator_id % 2',
    `product_type` TINYINT      NOT NULL COMMENT '商品类型：1=实物 2=虚拟 3=素材',
    `title`        VARCHAR(60)  NOT NULL COMMENT '商品标题（1-60 字）',
    `short_title`  VARCHAR(120) NULL COMMENT '商品短标题（最多 120 字）',
    `price`        BIGINT       NOT NULL COMMENT '价格（以分为单位的整数）',
    `detail`       TEXT         NULL COMMENT '商品详情富文本，最多 2000 字',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=上架 2=下架 3=处罚',
    `created_at`   DATETIME     NOT NULL COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL COMMENT '更新时间',
    `deleted_at`   DATETIME     NULL COMMENT '软删除时间，NULL 表示未删除',
    PRIMARY KEY (`product_id`),
    KEY `idx_creator_updated` (`creator_id`, `updated_at`),
    KEY `idx_creator_status` (`creator_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品主表分片 0（creator_id 为偶数）';

-- ----------------------------------------------------------------------------
-- product_1：creator_id 为奇数时路由到此表
-- 通过 CREATE TABLE product_1 LIKE product_0 复制结构
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `product_1`;
CREATE TABLE `product_1` LIKE `product_0`;
ALTER TABLE `product_1` COMMENT='商品主表分片 1（creator_id 为奇数）';

-- ----------------------------------------------------------------------------
-- product_image：副图共享表，按 Snowflake product_id 关联（不再分片）
-- 主图与副图统一存此表，type=1 为主图，type=2 为副图
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `product_id`     BIGINT       NOT NULL COMMENT '关联商品 ID',
    `image_type`     TINYINT      NOT NULL COMMENT '图片类型：1=主图 2=副图',
    `mime_type`      VARCHAR(32)  NOT NULL COMMENT '实际 MIME 类型（由 magic bytes 判定）',
    `image_base64`   MEDIUMTEXT   NOT NULL COMMENT 'Base64 编码图片，含 data:image/...;base64, 前缀',
    `sort`           INT          NOT NULL DEFAULT 0 COMMENT '副图排序，主图固定 0',
    `created_at`     DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_type` (`product_id`, `image_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表（共享，不分片）';

-- ----------------------------------------------------------------------------
-- product_physical_ext：product_type=1 实物商品扩展表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `product_physical_ext`;
CREATE TABLE `product_physical_ext` (
    `product_id`    BIGINT      NOT NULL COMMENT '关联商品 ID',
    `stock`        INT         NOT NULL DEFAULT 0 COMMENT '库存数量，>= 0',
    `delivery_type` TINYINT    NOT NULL COMMENT '发货方式：1=快递 2=EMS 3=自配送',
    `refund_rule`  TINYINT     NOT NULL COMMENT '退货规则：1=支持 2=不支持',
    `extension_json` JSON     NULL COMMENT '低频临时扩展字段，L0 保持 NULL',
    `created_at`   DATETIME    NOT NULL COMMENT '创建时间',
    `updated_at`   DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实物商品扩展表';

-- ----------------------------------------------------------------------------
-- product_virtual_ext：product_type=2 虚拟商品扩展表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `product_virtual_ext`;
CREATE TABLE `product_virtual_ext` (
    `product_id`        BIGINT      NOT NULL COMMENT '关联商品 ID',
    `valid_days`        INT         NOT NULL DEFAULT 0 COMMENT '有效期天数，>= 0',
    `verification_type` TINYINT    NOT NULL COMMENT '核销方式：1=扫码 2=密码 3=链接',
    `store_ids`         JSON        NULL COMMENT '适用门店 ID 数组',
    `extension_json`    JSON        NULL COMMENT '低频临时扩展字段，L0 保持 NULL',
    `created_at`        DATETIME    NOT NULL COMMENT '创建时间',
    `updated_at`        DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟商品扩展表';

-- ----------------------------------------------------------------------------
-- product_material_ext：product_type=3 商业化素材扩展表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `product_material_ext`;
CREATE TABLE `product_material_ext` (
    `product_id`      BIGINT       NOT NULL COMMENT '关联商品 ID',
    `media_type`      TINYINT      NOT NULL COMMENT '媒体类型：1=图片 2=视频 3=音频',
    `media_url`       VARCHAR(512) NOT NULL COMMENT '素材文件 URL',
    `license_end_time` DATETIME    NULL COMMENT '授权结束时间，NULL 表示永久',
    `extension_json`  JSON         NULL COMMENT '低频临时扩展字段，L0 保持 NULL',
    `created_at`      DATETIME     NOT NULL COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商业化素材扩展表';

-- ----------------------------------------------------------------------------
-- 建议将 MySQL max_allowed_packet 调整为至少 32MB：
--   SET GLOBAL max_allowed_packet = 32 * 1024 * 1024;
-- 一个发布请求最多包含 1 张主图和 5 张副图，
-- 每张 Base64 解码后约 2MB，编码后约 2.7MB，合计约 16MB。
-- ----------------------------------------------------------------------------
