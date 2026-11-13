package com.campus.poi.entity;

/** 评论实体，对应 comment 表；createTime 用字符串存，页面展示和原版一致。 */
public class Comment {
    private Integer id;
    private Integer userId;
    private Integer poiId;
    private Integer score;
    private String content;
    private String createTime;

    // 连表带出来的
    private String username;
    private String nickname;
    private String poiName;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getPoiId() { return poiId; }
    public void setPoiId(Integer poiId) { this.poiId = poiId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPoiName() { return poiName; }
    public void setPoiName(String poiName) { this.poiName = poiName; }

    /** 头像首字母：优先昵称，没有就用用户名 */
    public String getInitial() {
        String n = (nickname != null && !nickname.isEmpty()) ? nickname : username;
        return n == null || n.isEmpty() ? "?" : n.substring(0, 1);
    }

    /** 展示名：同 Python 版的 (cm.nickname or cm.username) */
    public String getDisplayName() {
        return (nickname != null && !nickname.isEmpty()) ? nickname : username;
    }

    /** 这条评论得了几分就画几颗实心星 */
    public String getStarText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (score == null ? 0 : score); i++) sb.append('★');
        return sb.toString();
    }
}
