package com.campus.poi.repository;

import com.campus.poi.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 用户表的数据访问 */
@Repository
public class UserRepository {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> MAPPER = (rs, n) -> {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setNickname(rs.getString("nickname"));
        u.setIsAdmin(rs.getInt("is_admin"));
        Timestamp ts = rs.getTimestamp("create_time");
        u.setCreateTime(ts == null ? "" : ts.toLocalDateTime().format(FMT));
        return u;
    };

    public User findByUsernameAndPassword(String username, String password) {
        List<User> rows = jdbc.query("SELECT * FROM `user` WHERE `username`=? AND `password`=?",
                MAPPER, username, password);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 管理员登录：额外要求 is_admin=1 */
    public User findAdmin(String username, String password) {
        List<User> rows = jdbc.query("SELECT * FROM `user` WHERE `username`=? AND `password`=? AND `is_admin`=1",
                MAPPER, username, password);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean existsByUsername(String username) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE `username`=?", Integer.class, username);
        return n != null && n > 0;
    }

    public void insert(String username, String password, String nickname) {
        jdbc.update("INSERT INTO `user` (`username`,`password`,`nickname`) VALUES (?,?,?)",
                username, password, nickname);
    }

    /** 后台用户列表：带上评论数、收藏数 */
    public List<User> listAllWithCounts() {
        return jdbc.query("SELECT u.*, "
                        + "(SELECT COUNT(*) FROM `comment` cm WHERE cm.user_id=u.id) AS comment_count, "
                        + "(SELECT COUNT(*) FROM `favorite` f WHERE f.user_id=u.id) AS fav_count "
                        + "FROM `user` u ORDER BY u.id",
                (rs, n) -> {
                    User u = MAPPER.mapRow(rs, n);
                    u.setCommentCount(rs.getInt("comment_count"));
                    u.setFavCount(rs.getInt("fav_count"));
                    return u;
                });
    }

    public User findById(int id) {
        List<User> rows = jdbc.query("SELECT * FROM `user` WHERE `id`=?", MAPPER, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 一条 SQL 完成角色取反 */
    public void toggleAdmin(int id) {
        jdbc.update("UPDATE `user` SET `is_admin`=IF(`is_admin`=1,0,1) WHERE `id`=?", id);
    }

    public void setAdmin(int id, int isAdmin) {
        jdbc.update("UPDATE `user` SET `is_admin`=? WHERE `id`=?", isAdmin, id);
    }
}
