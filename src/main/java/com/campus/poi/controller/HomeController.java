package com.campus.poi.controller;

import com.campus.poi.entity.Category;
import com.campus.poi.entity.Poi;
import com.campus.poi.repository.CategoryRepository;
import com.campus.poi.repository.PoiRepository;
import com.campus.poi.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final CategoryRepository categoryRepo;
    private final PoiRepository poiRepo;
    private final GeoService geo;
    private final RecommendService recommendService;
    private final SceneService sceneService;
    private final OverviewService overviewService;

    public HomeController(CategoryRepository categoryRepo, PoiRepository poiRepo, GeoService geo,
                          RecommendService recommendService, SceneService sceneService,
                          OverviewService overviewService) {
        this.categoryRepo = categoryRepo;
        this.poiRepo = poiRepo;
        this.geo = geo;
        this.recommendService = recommendService;
        this.sceneService = sceneService;
        this.overviewService = overviewService;
    }

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        List<Category> categoryList = categoryRepo.listAll();

        List<Poi> hotList = new ArrayList<>();
        for (Poi p : poiRepo.listHot(6)) hotList.add(geo.decorate(p));

        Integer uid = (Integer) session.getAttribute("userId");
        RecommendService.Result rec = recommendService.build(uid, 6);

        List<Poi> nowList = sceneService.pickForNow(6);
        SceneService.Period period = sceneService.nowPeriod();
        OverviewService.Overview overview = overviewService.build();

        model.addAttribute("categoryList", categoryList);
        model.addAttribute("hotPoiList", hotList);
        model.addAttribute("recommendList", rec.getList());
        model.addAttribute("userTags", rec.getTags());
        model.addAttribute("nowList", nowList);
        model.addAttribute("period", period);
        model.addAttribute("overview", overview);
        return "index";
    }
}
