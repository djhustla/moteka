package main.controlleurs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        String sql = "SELECT table_name, table_schema, table_type " +
                "FROM information_schema.tables " +
                "WHERE table_schema NOT IN ('information_schema', 'pg_catalog') " +
                "ORDER BY table_schema, table_name";

        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/tables/public")
    public List<String> getPublicTables() {
        String sql = "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' " +
                "ORDER BY table_name";

        return jdbcTemplate.queryForList(sql, String.class);
    }
}