package com.campus.poi.service;

import com.campus.poi.entity.Poi;
import com.campus.poi.repository.BehaviorRepository;
import com.campus.poi.repository.PoiRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 推荐模块：基于标签的内容推荐。
 * 对应 Python 版 build_recommend(uid)：
 *   最近 10 条浏览 + 全部收藏 → 统计标签频次 → 前 3 个偏好标签
 *   → 给候选打分（命中×100 + 浏览量×0.1 + 评分）→ 排除看过的 → 前 N 条
 */
@Service
public class RecommendService {

    private final BehaviorRepository behaviorRepo;
    private final PoiRepository poiRepo;
    private final GeoService geo;

    public RecommendService(BehaviorRepository behaviorRepo, PoiRepository poiRepo, GeoService geo) {
        this.behaviorRepo = behaviorRepo;
        this.poiRepo = poiRepo;
        this.geo = geo;
    }

    /** 返回结果：推荐的 POI + 用户偏好标签 */
    public static class Result {
        private List<Poi> list = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        public List<Poi> getList() { return list; }
        public List<String> getTags() { return tags; }
    }

    public Result build(Integer userId, int limit) {
        Result r = new Result();
        if (userId == null) return r;

        // 第 1 步：拿行为数据
        List<Poi> history = new ArrayList<>();
        history.addAll(behaviorRepo.recentBrowsed(userId, 10));
        history.addAll(behaviorRepo.favoritedPois(userId));

        // 第 2 步：统计标签频次，同时记下看过/收藏过的 ID
        Map<String, Integer> tagCounter = new LinkedHashMap<>();
        Set<Integer> touched = new HashSet<>();
        for (Poi p : history) {
            touched.add(p.getId());
            for (String tag : GeoService.splitTags(p.getTags())) {
                tagCounter.merge(tag, 1, Integer::sum);
            }
        }
        if (tagCounter.isEmpty()) return r;   // 新用户：前端回退成热门榜

        // 第 3 步：前 3 个偏好标签
        List<String> topTags = new ArrayList<>(tagCounter.keySet());
        topTags.sort((a, b) -> tagCounter.get(b) - tagCounter.get(a));
        if (topTags.size() > 3) topTags = topTags.subList(0, 3);

        // 第 4 步：全部上架 POI 当候选
        List<Poi> candidates = poiRepo.listActive();

        // 第 5 步：打分排序
        List<Poi> scored = new ArrayList<>();
        for (Poi p : candidates) {
            if (touched.contains(p.getId())) continue;
            List<String> poiTags = GeoService.splitTags(p.getTags());
            List<String> matched = new ArrayList<>();
            for (String t : topTags) if (poiTags.contains(t)) matched.add(t);
            if (matched.isEmpty()) continue;

            p.setMatchedTags(matched);
            double visit = p.getVisitCount() == null ? 0 : p.getVisitCount();
            double score = p.getAvgScore() == null ? 0 : p.getAvgScore();
            p.setMatchScore(matched.size() * 100 + visit * 0.1 + score);
            geo.decorate(p);
            scored.add(p);
        }
        scored.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        r.list = scored.stream().limit(limit).collect(java.util.stream.Collectors.toList());
        r.tags = topTags;
        return r;
    }
}
