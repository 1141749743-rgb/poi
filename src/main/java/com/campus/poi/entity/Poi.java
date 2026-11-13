package com.campus.poi.entity;

/**
 * POI（Point of Interest，兴趣点）实体。
 * 对应数据库 poi 表；带 * 的字段不存数据库，是查出来之后算出来的（距离/是否营业等）。
 */
public class Poi {

    private Integer id;
    private String name;
    private Integer categoryId;
    private String categoryName;   // 连表拿到的分类名 *
    private Double lng;
    private Double lat;
    private String address;
    private Integer visitCount;
    private Double avgScore;
    private String tags;
    private String businessHour;
    private String intro;
    private String coverImg;
    private Integer status;

    // ---- 计算字段（对应 Python 版 decorate_poi 补的三个） ----
    private Integer distanceM;      // 距校区多少米
    private String distanceText;    // "179 m" / "1.4 km"
    private Integer walkMin;        // 步行几分钟
    private Boolean openNow;        // 此刻是否营业（null = 营业时间没填）

    // ---- 推荐模块用 ----
    private java.util.List<String> matchedTags;  // 命中了哪些偏好标签
    private Double matchScore;                   // 推荐打分

    // ---- 我的收藏页用 ----
    private String favTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getVisitCount() { return visitCount; }
    public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }

    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getBusinessHour() { return businessHour; }
    public void setBusinessHour(String businessHour) { this.businessHour = businessHour; }

    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }

    public String getCoverImg() { return coverImg; }
    public void setCoverImg(String coverImg) { this.coverImg = coverImg; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getDistanceM() { return distanceM; }
    public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }

    public String getDistanceText() { return distanceText; }
    public void setDistanceText(String distanceText) { this.distanceText = distanceText; }

    public Integer getWalkMin() { return walkMin; }
    public void setWalkMin(Integer walkMin) { this.walkMin = walkMin; }

    public Boolean getOpenNow() { return openNow; }
    public void setOpenNow(Boolean openNow) { this.openNow = openNow; }

    public java.util.List<String> getMatchedTags() { return matchedTags; }
    public void setMatchedTags(java.util.List<String> matchedTags) { this.matchedTags = matchedTags; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public String getFavTime() { return favTime; }
    public void setFavTime(String favTime) { this.favTime = favTime; }

    /** 模板里直接用 ${poi.introPretty} 就能拿到清洗过的简介（等价于原来的 pretty_intro 过滤器） */
    public String getIntroPretty() {
        return com.campus.poi.service.GeoService.prettyIntro(intro);
    }

    /** 标签列表，模板里 <span th:each="t : ${poi.tagList}" th:text="${t}"></span> */
    public java.util.List<String> getTagList() {
        return com.campus.poi.service.GeoService.splitTags(tags);
    }

    /** 按平均分画 5 颗星：3.4 分 → ★★★☆☆（四舍五入） */
    public String getStarText() {
        int filled = (int) Math.round(avgScore == null ? 0 : avgScore);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < filled ? '★' : '☆');
        return sb.toString();
    }
}
