CREATE DATABASE IF NOT EXISTS tourism_qa
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE tourism_qa;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(40) NOT NULL,
  email VARCHAR(120) NULL,
  password_hash VARCHAR(120) NOT NULL,
  display_name VARCHAR(60) NOT NULL,
  preferred_departure VARCHAR(120) NULL,
  budget_preference VARCHAR(80) NULL,
  travel_preferences VARCHAR(255) NULL,
  interest_tags VARCHAR(255) NULL,
  memory_strategy VARCHAR(40) NOT NULL DEFAULT 'STANDARD',
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  banned TINYINT(1) NOT NULL DEFAULT 0,
  ban_reason VARCHAR(255) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users MODIFY COLUMN email VARCHAR(120) NULL;

CREATE TABLE IF NOT EXISTS chat_conversations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  provider VARCHAR(40) NOT NULL,
  model VARCHAR(120) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_chat_conversation_user_created (user_id, created_at),
  CONSTRAINT fk_chat_conversation_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(12) NOT NULL,
  content TEXT NOT NULL,
  provider VARCHAR(40) NULL,
  model VARCHAR(120) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_chat_message_conversation_created (conversation_id, created_at),
  CONSTRAINT fk_chat_message_conversation
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS llm_models (
  id BIGINT NOT NULL AUTO_INCREMENT,
  provider VARCHAR(40) NOT NULL,
  model_id VARCHAR(160) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  base_url VARCHAR(255) NOT NULL,
  api_key VARCHAR(255) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  is_default TINYINT(1) NOT NULL DEFAULT 0,
  last_checked_at TIMESTAMP(6) NULL,
  last_check_passed TINYINT(1) NULL,
  last_check_message VARCHAR(255) NULL,
  total_call_count BIGINT NOT NULL DEFAULT 0,
  successful_call_count BIGINT NOT NULL DEFAULT 0,
  failed_call_count BIGINT NOT NULL DEFAULT 0,
  total_latency_ms BIGINT NOT NULL DEFAULT 0,
  average_latency_ms BIGINT NULL,
  last_latency_ms BIGINT NULL,
  last_called_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_llm_model_provider_id (provider, model_id),
  KEY idx_llm_model_enabled_default (enabled, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_column_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_definition VARCHAR(512)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_column_if_missing('llm_models', 'total_call_count', 'BIGINT NOT NULL DEFAULT 0 AFTER last_check_message');
CALL add_column_if_missing('llm_models', 'successful_call_count', 'BIGINT NOT NULL DEFAULT 0 AFTER total_call_count');
CALL add_column_if_missing('llm_models', 'failed_call_count', 'BIGINT NOT NULL DEFAULT 0 AFTER successful_call_count');
CALL add_column_if_missing('llm_models', 'total_latency_ms', 'BIGINT NOT NULL DEFAULT 0 AFTER failed_call_count');
CALL add_column_if_missing('llm_models', 'average_latency_ms', 'BIGINT NULL AFTER total_latency_ms');
CALL add_column_if_missing('llm_models', 'last_latency_ms', 'BIGINT NULL AFTER average_latency_ms');
CALL add_column_if_missing('llm_models', 'last_called_at', 'TIMESTAMP(6) NULL AFTER last_latency_ms');

DROP PROCEDURE add_column_if_missing;

CREATE TABLE IF NOT EXISTS llm_call_metrics (
  id BIGINT NOT NULL AUTO_INCREMENT,
  provider VARCHAR(40) NOT NULL,
  model VARCHAR(160) NOT NULL,
  success TINYINT(1) NOT NULL,
  latency_ms BIGINT NOT NULL,
  error_message VARCHAR(255) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_llm_call_metrics_provider_model_created (provider, model, created_at),
  KEY idx_llm_call_metrics_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kg_change_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL,
  target_label VARCHAR(160) NULL,
  operator_user_id BIGINT NULL,
  operator_username VARCHAR(40) NULL,
  operator_display_name VARCHAR(60) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_kg_change_log_target_created (target_type, target_id, created_at),
  KEY idx_kg_change_log_operator_created (operator_user_id, created_at),
  CONSTRAINT fk_kg_change_log_operator
    FOREIGN KEY (operator_user_id) REFERENCES users(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS managed_external_services (
  id BIGINT NOT NULL AUTO_INCREMENT,
  service_key VARCHAR(40) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  base_url VARCHAR(512) NOT NULL,
  settings_json TEXT NOT NULL,
  last_checked_at TIMESTAMP(6) NULL,
  last_check_passed TINYINT(1) NULL,
  last_check_message VARCHAR(255) NULL,
  last_heartbeat_at TIMESTAMP(6) NULL,
  last_heartbeat_passed TINYINT(1) NULL,
  last_heartbeat_message VARCHAR(255) NULL,
  last_heartbeat_latency_ms BIGINT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_managed_service_key (service_key),
  KEY idx_managed_service_key_enabled (service_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS route_plans (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  conversation_id BIGINT NULL,
  destination VARCHAR(120) NOT NULL,
  days INT NOT NULL,
  start_date DATE NULL,
  end_date DATE NULL,
  interests VARCHAR(200) NULL,
  budget VARCHAR(120) NULL,
  departure VARCHAR(120) NULL,
  provider VARCHAR(40) NOT NULL,
  model VARCHAR(120) NOT NULL,
  title VARCHAR(180) NOT NULL,
  summary TEXT NOT NULL,
  raw_llm_output LONGTEXT NULL,
  tips_json TEXT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_route_plan_user_created (user_id, created_at),
  KEY idx_route_plan_conversation (conversation_id),
  CONSTRAINT fk_route_plan_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_route_plan_conversation
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS route_points (
  id BIGINT NOT NULL AUTO_INCREMENT,
  route_plan_id BIGINT NOT NULL,
  day_no INT NOT NULL,
  point_order INT NOT NULL,
  name VARCHAR(150) NOT NULL,
  description TEXT NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  PRIMARY KEY (id),
  KEY idx_route_point_plan_day_order (route_plan_id, day_no, point_order),
  CONSTRAINT fk_route_point_plan
    FOREIGN KEY (route_plan_id) REFERENCES route_plans(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS explore_posts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NULL,
  content TEXT NULL,
  image_urls LONGTEXT NULL,
  location_tag VARCHAR(120) NULL,
  route_json LONGTEXT NULL,
  click_count INT NOT NULL DEFAULT 0,
  apply_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_explore_post_created (created_at),
  KEY idx_explore_post_user_created (user_id, created_at),
  CONSTRAINT fk_explore_post_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS explore_post_likes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_explore_post_like_post_user (post_id, user_id),
  KEY idx_explore_post_like_post_created (post_id, created_at),
  KEY idx_explore_post_like_user_created (user_id, created_at),
  CONSTRAINT fk_explore_post_like_post
    FOREIGN KEY (post_id) REFERENCES explore_posts(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_explore_post_like_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS explore_post_favorites (
  id BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_explore_post_favorite_post_user (post_id, user_id),
  KEY idx_explore_post_favorite_post_created (post_id, created_at),
  KEY idx_explore_post_favorite_user_created (user_id, created_at),
  CONSTRAINT fk_explore_post_favorite_post
    FOREIGN KEY (post_id) REFERENCES explore_posts(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_explore_post_favorite_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS explore_post_comments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_explore_post_comment_post_created (post_id, created_at),
  KEY idx_explore_post_comment_user_created (user_id, created_at),
  CONSTRAINT fk_explore_post_comment_post
    FOREIGN KEY (post_id) REFERENCES explore_posts(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_explore_post_comment_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS explore_post_comment_likes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  comment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_explore_comment_like_comment_user (comment_id, user_id),
  KEY idx_explore_comment_like_comment_created (comment_id, created_at),
  KEY idx_explore_comment_like_user_created (user_id, created_at),
  CONSTRAINT fk_explore_comment_like_comment
    FOREIGN KEY (comment_id) REFERENCES explore_post_comments(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_explore_comment_like_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
