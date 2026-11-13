package com.campus.poi.repository;

import com.campus.poi.entity.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 评论表的数据访问 */
@Repository
public class CommentRepository {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public CommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Comment> MAPPER = (rs, n) -> {
        Comment c = new Comment();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setPoiId(rs.getInt("poi_id"));
        c.setScore(rs.getInt("score"));
        c.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("create_time");
        c.setCreateTime(ts == null ? "" : ts.toLocalDateTime().format(FMT));
        return c;
    };

    /** 某个地点下的全部评论（连 user 表拿昵称），最新在前 */
    public List<Comment> listByPoi(int poiId) {
        return jdbc.query("SELECT cm.*, u.nickname, u.username FROM `comment` cm "
                        + "JOIN `user` u ON cm.user_id = u.id "
                        + "WHERE cm.poi_id=? ORDER BY cm.create_time DESC",
                (rs, n) -> {
                    Comment c = MAPPER.mapRow(rs, n);
                    c.setNickname(rs.getString("nickname"));
                    c.setUsername(rs.getString("username"));
                    return c;
                }, poiId);
    }

    /** 后台首页：最新 15 条 */
    public List<Comment> listLatest(int limit) {
        return jdbc.query("SELECT cm.id, cm.user_id, cm.poi_id, cm.score, cm.content, cm.create_time, "
                        + "u.username, p.name AS poi_name FROM `comment` cm "
                        + "JOIN `user` u ON cm.user_id = u.id "
                        + "JOIN `poi` p ON cm.poi_id = p.id "
                        + "ORDER BY cm.create_time DESC LIMIT ?", LIST_MAPPER, limit);
    }

    /** 后台评论管理：全量前 200 条 */
    public List<Comment> listAll(int limit) {
        return jdbc.query("SELECT cm.id, cm.user_id, cm.poi_id, cm.score, cm.content, cm.create_time, "
                        + "u.username, u.nickname, p.name AS poi_name FROM `comment` cm "
                        + "JOIN `user` u ON cm.user_id = u.id "
                        + "JOIN `poi` p ON cm.poi_id = p.id "
                        + "ORDER BY cm.create_time DESC LIMIT ?", LIST_MAPPER, limit);
    }

    private static final RowMapper<Comment> LIST_MAPPER = (rs, n) -> {
        Comment c = MAPPER.mapRow(rs, n);
        c.setUsername(rs.getString("username"));
        try { c.setNickname(rs.getString("nickname")); } catch (Exception ignored) {}
        c.setPoiName(rs.getString("poi_name"));
        return c;
    };

    public void insert(int userId, int poiId, int score, String content) {
        jdbc.update("INSERT INTO `comment`(`user_id`,`poi_id`,`score`,`content`) VALUES (?,?,?,?)",
                userId, poiId, score, content);
    }

    /** 平均分：保留 1 位小数（ROUND(AVG(score),1)） */
    public Double avgScore(int poiId) {
        return jdbc.queryForObject("SELECT ROUND(AVG(`score`),1) FROM `comment` WHERE `poi_id`=?",
                Double.class, poiId);
    }

    public Integer findPoiId(int commentId) {
        List<Integer> rows = jdbc.query("SELECT `poi_id` FROM `comment` WHERE `id`=?",
                (rs, n) -> rs.getInt("poi_id"), commentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void delete(int commentId) {
        jdbc.update("DELETE FROM `comment` WHERE `id`=?", commentId);
    }
}
