CREATE SCHEMA IF NOT EXISTS myapp_db;

CREATE TABLE IF NOT EXISTS `business_units` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `name`                VARCHAR(32) NOT NULL,
  `status`              INT DEFAULT 0,
  `cBy`                 VARCHAR(128) DEFAULT NULL,
  `uBy`                 VARCHAR(128) DEFAULT NULL,
  `cDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY unique_bu_name_company(`name`));

CREATE TABLE IF NOT EXISTS `roles` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `name`                VARCHAR(128) NOT NULL,
  `status`              TINYINT NOT NULL DEFAULT 0,
  `cBy`                 VARCHAR(128) DEFAULT NULL,
  `uBy`                 VARCHAR(128) DEFAULT NULL,
  `cDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY unique_name(`name`));

CREATE TABLE IF NOT EXISTS `back_office_users` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `userName`            VARCHAR(128) NOT NULL,
  `password`            VARCHAR(128) NOT NULL,
  `roleId`              CHAR(36) NOT NULL,
  `businessUnitId`      CHAR(36) NOT NULL,
  `email`               VARCHAR(128) NOT NULL,
  `phoneNumber`         VARCHAR(50)  DEFAULT NULL,
  `firstName`           VARCHAR(128) NOT NULL,
  `middleName`          VARCHAR(128) DEFAULT NULL,
  `lastName`            VARCHAR(128) NOT NULL,
  `description`         VARCHAR(128) DEFAULT NULL,
  `homePage`            VARCHAR(128) DEFAULT NULL,
  `status`              TINYINT NOT NULL DEFAULT 0,
  `activeLanguage`      VARCHAR(50) DEFAULT NULL,
  `customData`          VARCHAR(512) DEFAULT NULL,
  `lastLoginTimestamp`  BIGINT(20) UNSIGNED DEFAULT NULL,
  `cBy`                 VARCHAR(128) DEFAULT NULL,
  `uBy`                 VARCHAR(128) DEFAULT NULL,
  `cDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`roleId`) REFERENCES roles(`id`),
  FOREIGN KEY (`businessUnitId`) REFERENCES business_units(`id`),
  UNIQUE KEY unique_email(`email`),
  UNIQUE KEY unique_userName (`userName`));

CREATE TABLE IF NOT EXISTS `scopes` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `parentId`            CHAR(36) DEFAULT NULL,
  `name`                VARCHAR(32) NOT NULL,
  `description`         VARCHAR(255) DEFAULT NULL,
  `status`              BIT NOT NULL DEFAULT 1,
  `cBy`                 VARCHAR(128) DEFAULT NULL,
  `uBy`                 VARCHAR(128) DEFAULT NULL,
  `cDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`parentId`) REFERENCES scopes(`id`),
  UNIQUE KEY unique_scope_name (`name`));

CREATE TABLE IF NOT EXISTS `permissions` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `buId`                CHAR(36) DEFAULT NULL,
  `userId`              CHAR(36) DEFAULT NULL,
  `roleId`              CHAR(36) DEFAULT NULL,
  `scopeId`             CHAR(36) NOT NULL,
  `canWrite`            BOOLEAN NOT NULL,
  `status`              TINYINT NOT NULL DEFAULT 1,
  `cBy`                 VARCHAR(128) DEFAULT NULL,
  `uBy`                 VARCHAR(128) DEFAULT NULL,
  `cDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`buId`) REFERENCES business_units(`id`),
  FOREIGN KEY (`userId`) REFERENCES `back_office_users`(`id`),
  FOREIGN KEY (`roleId`) REFERENCES roles(`id`),
  FOREIGN KEY (`scopeId`) REFERENCES scopes(`id`),
  UNIQUE KEY `unique_business_unit_and_role_scope` (`buId`, `roleId`, `scopeId`),
  UNIQUE KEY unique_user_scope (`userId`, `scopeId`));