package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Every foreign-key column on the core tables must lead some index. PostgreSQL does not create them
 * automatically, and without one each parent delete scans the child table (audit finding 15).
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaIndexesIntegrationTest extends PostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void everyForeignKeyColumnLeadsAnIndex() {
    // FK constraints whose first column is not the first column of any index on that table.
    List<String> unindexed =
        jdbc.queryForList(
            """
            SELECT c.conrelid::regclass || '.' || a.attname
              FROM pg_constraint c
              JOIN pg_attribute a
                ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
             WHERE c.contype = 'f'
               AND c.connamespace = 'public'::regnamespace
               AND NOT EXISTS (
                     SELECT 1
                       FROM pg_index i
                      WHERE i.indrelid = c.conrelid
                        AND i.indkey[0] = c.conkey[1])
             ORDER BY 1
            """,
            String.class);

    assertThat(unindexed).isEmpty();
  }

  @Test
  void sessionHistoryIndexCoversRecipeAndNewestFirst() {
    String definition =
        jdbc.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE indexname = ?",
            String.class,
            "idx_brew_sessions_recipe_id_brewed_at");

    assertThat(definition).contains("(recipe_id, brewed_at DESC)");
  }
}
