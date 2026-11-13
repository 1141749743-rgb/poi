package com.campus.poi.service;

import com.campus.poi.entity.Comment;
import com.campus.poi.entity.Poi;
import com.campus.poi.repository.CommentRepository;
import com.campus.poi.repository.PoiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地点详情页的写入操作：浏览量、评论、评分重算。
 * 写操作放在 Service 里加 @Transactional，出异常自动回滚
 * （等价于 Python 版里 try/finally 的 commit/close）。
 */
@Service
public class PoiService {

    private final PoiRepository poiRepo;
    private final CommentRepository commentRepo;
    private final GeoService geo;

    public PoiService(PoiRepository poiRepo, CommentRepository commentRepo, GeoService geo) {
        this.poiRepo = poiRepo;
        this.commentRepo = commentRepo;
        this.geo = geo;
    }

    /** 详情页：浏览量 +1、补距离信息 */
    @Transactional
    public Poi view(int poiId) {
        Poi poi = poiRepo.findById(poiId);
        if (poi == null) return null;
        poiRepo.incrementVisit(poiId);
        poi.setVisitCount(poi.getVisitCount() + 1);   // 内存里也 +1，页面显示最新数字
        geo.decorate(poi);
        return poi;
    }

    public List<Comment> comments(int poiId) {
        return commentRepo.listByPoi(poiId);
    }

    /** 发评论 + 重算平均分 */
    @Transactional
    public double addComment(int userId, int poiId, int score, String content) {
        commentRepo.insert(userId, poiId, score, content);
        Double avg = commentRepo.avgScore(poiId);
        double newAvg = (avg == null) ? 5.0 : avg;
        poiRepo.updateAvgScore(poiId, newAvg);
        return newAvg;
    }

    /** 删评论 + 重算平均分（管理员用），返回评论所属的 POI id */
    @Transactional
    public Integer deleteComment(int commentId) {
        Integer poiId = commentRepo.findPoiId(commentId);
        commentRepo.delete(commentId);
        if (poiId != null) {
            Double avg = commentRepo.avgScore(poiId);
            if (avg != null) poiRepo.updateAvgScore(poiId, avg);
        }
        return poiId;
    }
}
