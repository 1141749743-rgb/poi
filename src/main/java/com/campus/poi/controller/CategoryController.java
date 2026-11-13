package com.campus.poi.controller;

import com.campus.poi.entity.Poi;
import com.campus.poi.repository.CategoryRepository;
import com.campus.poi.service.GeoService;
import com.campus.poi.service.SceneService;
import com.campus.poi.entity.Category;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/** 分类页：某个分类下的所有 POI，支持圈层 + 排序 */
@Controller
public class CategoryController {

    private final CategoryRepository categoryRepo;
    private final SceneService sceneService;
    private final GeoService geo;

    public CategoryController(CategoryRepository categoryRepo, SceneService sceneService, GeoService geo) {
        this.categoryRepo = categoryRepo;
        this.sceneService = sceneService;
        this.geo = geo;
    }

    @GetMapping("/category/{cid}")
    public String category(@org.springframework.web.bind.annotation.PathVariable("cid") int cid,
                           @RequestParam(required = false) Integer radius,
                           @RequestParam(defaultValue = "near") String sort,
                           Model model) {
        Category cate = categoryRepo.findById(cid);
        if (cate == null) {
            model.addAttribute("flash", "分类不存在！");
            return "redirect:/";
        }
        if (radius == null || !(radius == 500 || radius == 1000 || radius == 3000)) radius = null;

        List<Poi> list = new ArrayList<>();
        for (Poi p : sceneService.fetchScenePois(List.of(cate.getName()), 999999, false, false)) {
            list.add(p);
        }
        if (radius != null) list = sceneService.filterByRadius(list, radius);

        if ("hot".equals(sort)) {
            list.sort((a, b) -> Integer.compare(b.getVisitCount() == null ? 0 : b.getVisitCount(),
                    a.getVisitCount() == null ? 0 : a.getVisitCount()));
        } else if ("score".equals(sort)) {
            list.sort((a, b) -> Double.compare(b.getAvgScore() == null ? 0 : b.getAvgScore(),
                    a.getAvgScore() == null ? 0 : a.getAvgScore()));
        } else {
            list.sort((a, b) -> {
                Integer da = a.getDistanceM(), db = b.getDistanceM();
                if (da == null && db == null) return 0;
                if (da == null) return 1;      // 没坐标的甩到最后
                if (db == null) return -1;
                return Integer.compare(da, db);
            });
        }

        model.addAttribute("cate", cate);
        model.addAttribute("poiList", list);
        model.addAttribute("radius", radius);
        model.addAttribute("sort", sort);
        return "category";
    }
}
