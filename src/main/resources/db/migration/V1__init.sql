-- V1__init.sql
-- DB: PostgreSQL

CREATE TABLE inventory_item (
  id              BIGSERIAL PRIMARY KEY,
  source          VARCHAR(50) NOT NULL DEFAULT 'powershell_csv',
  hostname        VARCHAR(1000) NOT NULL,
  asset_tag       VARCHAR(1000),
  vendor          VARCHAR(1000),
  product         VARCHAR(1000) NOT NULL,
  version         VARCHAR(100),
  cpe             TEXT,
  installed_on    DATE,
  last_seen_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_host ON inventory_item(hostname);
CREATE INDEX idx_inventory_vendor_product ON inventory_item(vendor, product);
-- CPE index opcionális, de legyen:
CREATE INDEX idx_inventory_cpe ON inventory_item(cpe);

CREATE TABLE kev_entry (
  cve_id              VARCHAR(20) PRIMARY KEY,
  vendor_project      VARCHAR(1000),
  product             VARCHAR(1000),
  vulnerability_name  TEXT,
  date_added          DATE,
  short_description   TEXT,
  required_action     TEXT,
  due_date            DATE,
  known_ransomware_campaign_use BOOLEAN
);

CREATE INDEX idx_kev_vendor_product ON kev_entry(vendor_project, product);
CREATE INDEX idx_kev_date_added ON kev_entry(date_added);


CREATE TABLE kev_refresh_log (
  id              BIGSERIAL PRIMARY KEY,
  refreshed_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  vulns_read      INT NOT NULL,
  upserted        INT NOT NULL,
  kev_count_before INT NOT NULL,
  kev_count_after  INT NOT NULL
);

CREATE INDEX idx_kev_refresh_log_refreshed_at ON kev_refresh_log(refreshed_at DESC);

