package com.campus.poi.controller;

import com.campus.poi.entity.Comment;
import com.campus.poi.entity.Poi;
import com.campus.poi.repository.BehaviorRepository;
import com.campus.poi.service.PoiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** POI 详情页 + 评论 + 收藏 + 管理员在前台删评论 */
@Controller
public class PoiController {

    private final PoiService poiService;
    private final BehaviorRepository behaviorRepo;

    @Value("${amap.js-key:}")
    private String amapKey;

    @Value("${amap.security-code:}")
    private String amapSecurity;

    public PoiController(PoiService poiService, BehaviorRepository behaviorRepo) {
        this.poiService = poiService;
        this.behaviorRepo = behaviorRepo;
    }

    @GetMapping("/poi/{pid}")
    public String detail(@PathVariable int pid, Model model, HttpSession session, RedirectAttributes ra) {
        Poi poi = poiService.view(pid);
        if (poi == null) {
            ra.addFlashAttribute("flash", "该地点不存在！");
            return "redirect:/";
        }

        Integer uid = (Integer) session.getAttribute("userId");
        boolean isFav = false;
        if (uid != null) {
            behaviorRepo.addBrowse(uid, pid);          // 写浏览记录，推荐模块要用
            isFav = behaviorRepo.isFavorite(uid, pid);
        }

        List<Comment> comments = poiService.comments(pid);

        model.addAttribute("poi", poi);
        model.addAttribute("isFav", isFav);
        model.addAttribute("commentList", comments);
        model.addAttribute("amapKey", amapKey);
        model.addAttribute("amapSecurity", amapSecurity);
        return "poi_detail";
    }

    @PostMapping("/comment/add/{pid}")
    public String addComment(@PathVariable int pid,
                             @RequestParam(required = false) Integer score,
                             @RequestParam(defaultValue = "") String content,
                             HttpSession session, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("userId");
        if (uid == null) {
            ra.addFlashAttribute("flash", "请先登录后再评论！");
            return "redirect:/login";
        }
        if (score == null || score < 1 || score > 5) {
            ra.addFlashAttribute("flash", "请选择 1~5 星的评分！");
            return "redirect:/poi/" + pid;
        }
        if (content == null || content.trim().isEmpty()) {
            ra.addFlashAttribute("flash", "评论内容不能为空！");
            return "redirect:/poi/" + pid;
        }

        double avg = poiService.addComment(uid, pid, score, content.trim());
        ra.addFlashAttribute("flash", "评论成功！当前平均分 " + avg + " 分");
        return "redirect:/poi/" + pid;
    }

    @PostMapping("/favorite/toggle/{pid}")
    public String toggleFavorite(@PathVariable int pid, HttpSession session, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("userId");
        if (uid == null) {
            ra.addFlashAttribute("flash", "请先登录后再收藏！");
            return "redirect:/login";
        }
        if (behaviorRepo.isFavorite(uid, pid)) {
            behaviorRepo.removeFavorite(uid, pid);
            ra.addFlashAttribute("flash", "已取消收藏");
        } else {
            behaviorRepo.addFavorite(uid, pid);
            ra.addFlashAttribute("flash", "收藏成功！");
        }
        return "redirect:/poi/" + pid;
    }

    /** 前台管理员删评论：只有 isAdmin=1 才能走，删完重算平均分并回到详情页 */
    @PostMapping("/comment/delete/{cid}")
    public String deleteComment(@PathVariable int cid, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("flash", "只有管理员可以删除评论");
            return "redirect:/";
        }
        Integer poiId = poiService.deleteComment(cid);
        ra.addFlashAttribute("flash", "评论已删除，平均分已重算");
        return poiId == null ? "redirect:/" : "redirect:/poi/" + poiId;
    }

    static boolean isAdmin(HttpSession session) {
        Object v = session.getAttribute("isAdmin");
        return v != null && ((Integer) v) == 1;
    }
}
