-- ----------------------------
-- Table structure for alert_contact
-- ----------------------------
DROP TABLE IF EXISTS `alert_contact`;
CREATE TABLE `alert_contact`
(
    `id`             bigint                                                 NOT NULL AUTO_INCREMENT COMMENT '主键',
    `contact_name`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '联系人姓名',
    `email`          varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '邮箱',
    `phone`          varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL COMMENT '手机号',
    `remark`         varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '备注',
    `create_by`      bigint                                                  DEFAULT NULL COMMENT '创建人ID',
    `create_by_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL COMMENT '创建人名称',
    `create_time`    datetime                                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY              `idx_create_by` (`create_by`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='报警联系人';
-- ----------------------------
-- Table structure for alert_contact_group
-- ----------------------------
DROP TABLE IF EXISTS `alert_contact_group`;
CREATE TABLE `alert_contact_group`
(
    `id`             bigint                                                  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_name`     varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '组名称',
    `contact_ids`    text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '联系人ID列表，逗号分隔',
    `remark`         varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '备注',
    `create_by`      bigint                                                  DEFAULT NULL COMMENT '创建人ID',
    `create_by_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL COMMENT '创建人名称',
    `create_time`    datetime                                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime                                                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY              `idx_create_by` (`create_by`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='报警联系人组';
-- ----------------------------
-- Table structure for alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule`
(
    `id`                 bigint                                                  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_name`          varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '规则名称',
    `alert_channels`     varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'EMAIL' COMMENT '预警通道，多选逗号分隔: EMAIL,SMS',
    `alert_level`        int                                                     DEFAULT '2' COMMENT '预警级别: 1-通知 2-警告 3-严重',
    `trigger_type`       varchar(20)                                             DEFAULT 'KEYWORD' COMMENT '触发类型: KEYWORD-关键字触发 THRESHOLD-超过阈值触发',
    `condition_keyword`  varchar(500)                                            DEFAULT NULL COMMENT '触发关键字，逗号分隔',
    `condition_threshold` double DEFAULT NULL COMMENT '触发阈值',
    `contact_group_id`   bigint                                                  DEFAULT NULL COMMENT '报警联系人组ID',
    `contact_group_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '报警联系人组名称',
    `is_enabled`         tinyint(1) DEFAULT '1' COMMENT '是否启用: 1-启用 0-禁用',
    `silence_period`     int                                                     DEFAULT '5' COMMENT '静默期（分钟）：同一规则触发后不重复发送的时间窗口',
    `trigger_threshold`  int                                                     DEFAULT '1' COMMENT '连续触发阈值：连续触发N次后才发送预警',
    `aggregate_window`   int                                                     DEFAULT '0' COMMENT '聚合窗口（分钟）：窗口内多次触发合并为一条预警',
    `create_by`          bigint                                                  DEFAULT NULL COMMENT '创建人ID',
    `create_by_name`     varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL COMMENT '创建人名称',
    `remark`             varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '备注',
    `remark_tmp`         varchar(255)                                            DEFAULT NULL,
    `create_time`        datetime                                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY                  `idx_create_by` (`create_by`) USING BTREE,
    KEY                  `idx_contact_group_id` (`contact_group_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='预警规则配置';
-- ----------------------------
-- Table structure for alert_record
-- ----------------------------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record`
(
    `id`             bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_id`        bigint                                                  DEFAULT NULL COMMENT '关联预警规则ID',
    `rule_name`      varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '规则名称',
    `alert_level`    int                                                     DEFAULT NULL COMMENT '预警级别: 1-通知 2-警告 3-严重',
    `alert_channel`  varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL COMMENT '预警通道: EMAIL / SMS',
    `alert_content`  text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '预警内容',
    `recipient`      varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '接收人',
    `send_status`    tinyint                                                 DEFAULT '0' COMMENT '发送状态: 0-成功 1-失败',
    `fail_reason`    varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '失败原因',
    `is_silenced`    tinyint                                                 DEFAULT '0' COMMENT '是否被降噪静默: 0-否 1-是',
    `trigger_count`  int                                                     DEFAULT '1' COMMENT '连续触发次数（降噪统计）',
    `create_by`      bigint                                                  DEFAULT NULL COMMENT '创建人ID',
    `create_by_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL COMMENT '创建人名称',
    `service_id`     int                                                     DEFAULT NULL COMMENT '环境id',
    `service_name`   varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '环境名',
    `create_time`    datetime                                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY              `idx_rule_id` (`rule_id`) USING BTREE,
    KEY              `idx_service_id` (`service_id`) USING BTREE,
    KEY              `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='预警发送记录';