-- =============================================================================
-- Aalap schema — managed manually (Hibernate ddl-auto=none).
-- All statements use IF NOT EXISTS so this file is safe to run on every boot.
-- Tested against TiDB Cloud Serverless (MySQL 8.x compatible).
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    bio             VARCHAR(255),
    profile_picture VARCHAR(255),
    created_at      DATETIME,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE IF NOT EXISTS nools (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    title            VARCHAR(255) NOT NULL,
    description      VARCHAR(255),
    created_by       BIGINT       NOT NULL,
    created_at       DATETIME,
    forked_from      BIGINT,
    master_file_path VARCHAR(255),
    bpm              INT,
    musical_key      VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_nools_created_by  FOREIGN KEY (created_by)  REFERENCES users(id),
    CONSTRAINT fk_nools_forked_from FOREIGN KEY (forked_from) REFERENCES nools(id)
);

CREATE TABLE IF NOT EXISTS contributions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    nool_id      BIGINT       NOT NULL,
    role         VARCHAR(255) NOT NULL,
    description  VARCHAR(255),
    file_path    VARCHAR(255),
    file_type    VARCHAR(255),
    carried_from BIGINT,
    created_at   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_contributions_user         FOREIGN KEY (user_id)      REFERENCES users(id),
    CONSTRAINT fk_contributions_nool         FOREIGN KEY (nool_id)      REFERENCES nools(id),
    CONSTRAINT fk_contributions_carried_from FOREIGN KEY (carried_from) REFERENCES contributions(id)
);

