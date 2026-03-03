package com.vulscan.dashboard.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventorySchemaAligner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public InventorySchemaAligner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE inventory_item ALTER COLUMN hostname TYPE VARCHAR(1000)");
        jdbcTemplate.execute("ALTER TABLE inventory_item ALTER COLUMN product TYPE VARCHAR(1000)");
        jdbcTemplate.execute("ALTER TABLE inventory_item ALTER COLUMN vendor TYPE VARCHAR(1000)");
        jdbcTemplate.execute("ALTER TABLE inventory_item ALTER COLUMN asset_tag TYPE VARCHAR(1000)");
        jdbcTemplate.execute("ALTER TABLE inventory_item ALTER COLUMN cpe TYPE TEXT");
    }
}
