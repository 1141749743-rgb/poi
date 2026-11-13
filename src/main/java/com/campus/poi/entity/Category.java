package com.campus.poi.entity;

/** 分类实体，对应 category 表。poiCount 是统计出来的，不存库。 */
public class Category {
    private Integer id;
    private String name;
    private Integer poiCount;

    public Category() {}

    public Category(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPoiCount() { return poiCount; }
    public void setPoiCount(Integer poiCount) { this.poiCount = poiCount; }
}
