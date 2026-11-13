package com.campus.poi.repository;

import com.campus.poi.entity.Poi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * POI 数据访问层（Repository）。
 * 只负责"跟数据库打交道"，不写业务判断 —— 业务判断放 Service。
 */
@Repository
public class PoiRepository {

    private final JdbcTemplate jdbc;

    public PoiRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 统一的行映射：数据库一行 → Poi 对象 */
    private static final RowMapper<Poi> MAPPER = (rs, n) -> {
        Poi p = new Poi();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setCategoryId(rs.getInt("category_id"));
        try { p.setCategoryName(rs.getString("category_name")); } catch (Exception ignored) {}
        double lng = rs.getDouble("lng");
        p.setLng(rs.wasNull() ? null : lng);
        double lat = rs.getDouble("lat");
        p.setLat(rs.wasNull() ? null : lat);
        p.setAddress(rs.getString("address"));
        p.setVisitCount(rs.getInt("visit_count"));
        p.setAvgScore(rs.getDouble("avg_score"));
        p.setTags(rs.getString("tags"));
        p.setBusinessHour(rs.getString("business_hour"));
        p.setIntro(rs.getString("intro"));
        p.setCoverImg(rs.getString("cover_img"));
        p.setStatus(rs.getInt("status"));
        return p;
    };

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name FROM `poi` p LEFT JOIN `category` c ON p.category_id = c.id ";

    public List<Poi> listActive() {
        return jdbc.query(BASE_SELECT + "WHERE p.status=1", MAPPER);
    }

    public List<Poi> listActiveByCategory(int categoryId) {
        return jdbc.query(BASE_SELECT + "WHERE p.category_id=? AND p.status=1", MAPPER, categoryId);
    }

    public List<Poi> listHot(int limit) {
        return jdbc.query(BASE_SELECT + "WHERE p.status=1 ORDER BY p.visit_count DESC, p.avg_score DESC LIMIT ?",
                MAPPER, limit);
    }

    public Poi findById(int id) {
        List<Poi> rows = jdbc.query(BASE_SELECT + "WHERE p.id=?", MAPPER, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 后台列表：不带 status 过滤，全部地点 */
    public List<Poi> listAll() {
        return jdbc.query(BASE_SELECT + "ORDER BY p.id", MAPPER);
    }

    public void incrementVisit(int id) {
        jdbc.update("UPDATE `poi` SET `visit_count`=`visit_count`+1 WHERE `id`=?", id);
    }

    public void updateAvgScore(int id, double avg) {
        jdbc.update("UPDATE `poi` SET `avg_score`=? WHERE `id`=?", avg, id);
    }

    public void toggleStatus(int id) {
        jdbc.update("UPDATE `poi` SET `status`=IF(`status`=1,0,1) WHERE `id`=?", id);
    }

    public int countByCategory(int categoryId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM `poi` WHERE `category_id`=?", Integer.class, categoryId);
        return n == null ? 0 : n;
    }

    public void insert(Poi p) {
        jdbc.update("INSERT INTO `poi` (`name`,`category_id`,`lng`,`lat`,`address`,`tags`,`business_hour`,`intro`,`cover_img`,`status`) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                p.getName(), p.getCategoryId(), p.getLng(), p.getLat(), p.getAddress(),
                p.getTags(), p.getBusinessHour(), p.getIntro(), p.getCoverImg(), p.getStatus());
    }

    public void update(Poi p) {
        jdbc.update("UPDATE `poi` SET `name`=?,`category_id`=?,`lng`=?,`lat`=?,`address`=?,"
                        + "`tags`=?,`business_hour`=?,`intro`=?,`cover_img`=?,`status`=? WHERE `id`=?",
                p.getName(), p.getCategoryId(), p.getLng(), p.getLat(), p.getAddress(),
                p.getTags(), p.getBusinessHour(), p.getIntro(), p.getCoverImg(), p.getStatus(), p.getId());
    }

    /**
     * 删除 POI。注意顺序：先删三张子表，再删主表，
     * 否则 MySQL 会报 1451 外键约束错误（和 Python 版同一个坑）。
     */
    public void deleteCascade(int id) {
        jdbc.update("DELETE FROM `comment` WHERE `poi_id`=?", id);
        jdbc.update("DELETE FROM `favorite` WHERE `poi_id`=?", id);
        jdbc.update("DELETE FROM `browse_record` WHERE `poi_id`=?", id);
        jdbc.update("DELETE FROM `poi` WHERE `id`=?", id);
    }
}
