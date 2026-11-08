-- Phase 9: ML Dataset Table
USE projectguard;

CREATE TABLE IF NOT EXISTS ml_dataset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NULL,
    project_id BIGINT NULL,
    student_skill_level DOUBLE NULL,
    skill_gap DOUBLE NULL,
    project_complexity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    team_size INT NULL,
    available_hours_per_week DOUBLE NULL,
    technology_familiarity DOUBLE NULL,
    number_of_requirements INT NULL,
    number_of_features INT NULL,
    previous_experience DOUBLE NULL,
    estimated_effort DOUBLE NULL,
    actual_outcome VARCHAR(30) NULL,
    outcome_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_OUTCOME',
    data_source VARCHAR(50) DEFAULT 'AUTOMATIC_COLLECTION',
    validation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    label_evidence TEXT NULL,
    label_notes TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ml_dataset_student_project UNIQUE (student_profile_id, project_id),
    CONSTRAINT fk_ml_dataset_student_profile FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE SET NULL,
    CONSTRAINT fk_ml_dataset_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    INDEX idx_ml_dataset_student (student_profile_id),
    INDEX idx_ml_dataset_project (project_id),
    INDEX idx_ml_dataset_outcome (outcome_status),
    INDEX idx_ml_dataset_val_status (validation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

