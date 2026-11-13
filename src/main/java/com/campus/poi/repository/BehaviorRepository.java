package com.campus.poi.repository;

import com.campus.poi.entity.Poi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 收藏表 + 浏览记录表的数据访问（两张表都是"用户行为"） */
@Repository
public class BehaviorRepository {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public BehaviorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------- 收藏 ----------------

    public boolean isFavorite(int userId, int poiId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM `favorite` WHERE `user_id`=? AND `poi_id`=?",
                Integer.class, userId, poiId);
        return n != null && n > 0;
    }

    public void addFavorite(int userId, int poiId) {
        jdbc.update("INSERT INTO `favorite`(`user_id`,`poi_id`) VALUES (?,?)", userId, poiId);
    }

    public void removeFavorite(int userId, int poiId) {
        jdbc.update("DELETE FROM `favorite` WHERE `user_id`=? AND `poi_id`=?", userId, poiId);
    }

    /** 我的收藏列表（带分类名和收藏时间） */
    public List<Poi> listMyFavorites(int userId) {
        return jdbc.query("SELECT p.*, c.name AS category_name, f.create_time AS fav_time "
                        + "FROM `favorite` f JOIN `poi` p ON f.poi_id = p.id "
                        + "LEFT JOIN `category` c ON p.category_id = c.id "
                        + "WHERE f.user_id=? ORDER BY f.create_time DESC",
                (rs, n) -> {
                    Poi poi = new Poi();
                    poi.setId(rs.getInt("id"));
                    poi.setName(rs.getString("name"));
                    poi.setCategoryId(rs.getInt("category_id"));
                    poi.setCategoryName(rs.getString("category_name"));
                    double lng = rs.getDouble("lng");
                    poi.setLng(rs.wasNull() ? null : lng);
                    double lat = rs.getDouble("lat");
                    poi.setLat(rs.wasNull() ? null : lat);
                    poi.setAddress(rs.getString("address"));
                    poi.setVisitCount(rs.getInt("visit_count"));
                    poi.setAvgScore(rs.getDouble("avg_score"));
                    poi.setTags(rs.getString("tags"));
                    poi.setBusinessHour(rs.getString("business_hour"));
                    poi.setIntro(rs.getString("intro"));
                    poi.setCoverImg(rs.getString("cover_img"));
                    poi.setStatus(rs.getInt("status"));
                    Timestamp ts = rs.getTimestamp("fav_time");
                    poi.setFavTime(ts == null ? "" : ts.toLocalDateTime().format(FMT));
                    return poi;
                }, userId);
    }

    // ---------------- 浏览记录 ----------------

    public void addBrowse(int userId, int poiId) {
        jdbc.update("INSERT INTO `browse_record`(`user_id`,`poi_id`) VALUES (?,?)", userId, poiId);
    }

    /** 最近 N 条浏览记录的 POI（推荐模块第 1 步要用 tags） */
    public List<Poi> recentBrowsed(int userId, int limit) {
        return jdbc.query("SELECT p.id, p.tags FROM `browse_record` b JOIN `poi` p ON b.poi_id = p.id "
                        + "WHERE b.user_id=? ORDER BY b.create_time DESC LIMIT ?",
                (rs, n) -> {
                    Poi p = new Poi();
                    p.setId(rs.getInt("id"));
                    p.setTags(rs.getString("tags"));
                    return p;
                }, userId, limit);
    }

    /** 收藏过的 POI（推荐模块第 1 步） */
    public List<Poi> favoritedPois(int userId) {
        return jdbc.query("SELECT p.id, p.tags FROM `favorite` f JOIN `poi` p ON f.poi_id = p.id WHERE f.user_id=?",
                (rs, n) -> {
                    Poi p = new Poi();
                    p.setId(rs.getInt("id"));
                    p.setTags(rs.getString("tags"));
                    return p;
                }, userId);
    }
}
