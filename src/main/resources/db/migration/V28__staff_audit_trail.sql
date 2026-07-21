CREATE TABLE pv_audit_logs (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 branch_id INT UNSIGNED NULL,
 actor_staff_id INT UNSIGNED NULL,
 actor_username VARCHAR(60) NULL,
 actor_name VARCHAR(100) NULL,
 actor_role VARCHAR(20) NULL,
 action_type VARCHAR(40) NOT NULL,
 resource_type VARCHAR(60) NOT NULL,
 resource_id VARCHAR(80) NULL,
 outcome VARCHAR(20) NOT NULL,
 description VARCHAR(500) NOT NULL,
 http_method VARCHAR(10) NULL,
 request_path VARCHAR(500) NULL,
 response_status SMALLINT UNSIGNED NULL,
 ip_address VARCHAR(64) NULL,
 user_agent VARCHAR(500) NULL,
 correlation_id VARCHAR(100) NOT NULL,
 metadata_json JSON NULL,
 occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_audit_branch FOREIGN KEY (branch_id) REFERENCES pv_branches(id),
 CONSTRAINT fk_audit_actor FOREIGN KEY (actor_staff_id) REFERENCES pv_staff(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_branch_date ON pv_audit_logs(branch_id, occurred_at);
CREATE INDEX idx_audit_actor_date ON pv_audit_logs(actor_staff_id, occurred_at);
CREATE INDEX idx_audit_action_date ON pv_audit_logs(action_type, occurred_at);
CREATE INDEX idx_audit_resource ON pv_audit_logs(resource_type, resource_id);
CREATE UNIQUE INDEX uk_audit_correlation ON pv_audit_logs(correlation_id);
