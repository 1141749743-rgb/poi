package com.campus.poi.controller;

import com.campus.poi.entity.Poi;
import com.campus.poi.service.SceneService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** 场景页：/scene/rush 这种，按"我想干什么"筛 */
@Controller
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @GetMapping("/scene/{key}")
    public String scene(@PathVariable String key,
                        @RequestParam(required = false) Integer radius,
                        Model model, RedirectAttributes ra) {
        SceneService.Scene scene = SceneService.SCENES.get(key);
        if (scene == null) {
            ra.addFlashAttribute("flash", "没有这个场景～");
            return "redirect:/";
        }
        if (radius == null || !(radius == 500 || radius == 1000 || radius == 3000)) {
            radius = scene.radius;
        }

        List<Poi> list = sceneService.fetchScenePois(scene.cats, radius, scene.onlyOpen, scene.late);

        model.addAttribute("scene", scene);
        model.addAttribute("key", key);
        model.addAttribute("poiList", list);
        model.addAttribute("radius", radius);
        return "scene";
    }
}
