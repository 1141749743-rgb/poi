package com.campus.poi.repository;

import com.campus.poi.entity.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 分类表的数据访问 */
@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbc;

    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Category> listAll() {
        return jdbc.query("SELECT `id`,`name` FROM `category` ORDER BY id",
                (rs, n) -> new Category(rs.getInt("id"), rs.getString("name")));
    }

    /** 带"这个分类下有多少地点"的列表（后台分类管理用） */
    public List<Category> listWithCount() {
        return jdbc.query("SELECT c.id, c.name, "
                        + "(SELECT COUNT(*) FROM `poi` p WHERE p.category_id=c.id) AS poi_count "
                        + "FROM `category` c ORDER BY c.id",
                (rs, n) -> {
                    Category c = new Category(rs.getInt("id"), rs.getString("name"));
                    c.setPoiCount(rs.getInt("poi_count"));
                    return c;
                });
    }

    public Category findById(int id) {
        List<Category> rows = jdbc.query("SELECT `id`,`name` FROM `category` WHERE `id`=?",
                (rs, n) -> new Category(rs.getInt("id"), rs.getString("name")), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean existsByName(String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM `category` WHERE `name`=?", Integer.class, name);
        return n != null && n > 0;
    }

    public void insert(String name) {
        jdbc.update("INSERT INTO `category` (`name`) VALUES (?)", name);
    }

    public void rename(int id, String name) {
        jdbc.update("UPDATE `category` SET `name`=? WHERE `id`=?", name, id);
    }

    public void delete(int id) {
        jdbc.update("DELETE FROM `category` WHERE `id`=?", id);
    }
}
