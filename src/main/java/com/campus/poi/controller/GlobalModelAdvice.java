package com.campus.poi.controller;

import com.campus.poi.service.SceneService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全局模型注入：所有模板都能直接用 username / isAdmin / adminName / scenes / active。
 * 等价于 Python 版每个 render_template 都传一遍 session.get("username")。
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @Value("${campus.name}")
    private String campusName;

    @Value("${campus.address}")
    private String campusAddress;

    @ModelAttribute
    public void common(Model model, HttpSession session, HttpServletRequest request) {
        model.addAttribute("username", session.getAttribute("username"));
        Object adminName = session.getAttribute("adminName");
        model.addAttribute("adminName", adminName != null ? adminName : session.getAttribute("username"));
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        model.addAttribute("campusName", campusName);
        model.addAttribute("campusAddress", campusAddress);
        model.addAttribute("scenes", SceneService.SCENES.values());

        // 后台导航高亮：靠当前 URL 判断，省得每个 controller 都传一遍 active
        String uri = request.getRequestURI();
        String active = "";
        if (uri.startsWith("/admin/categories")) active = "cate";
        else if (uri.startsWith("/admin/comments")) active = "comment";
        else if (uri.startsWith("/admin/users")) active = "user";
        else if (uri.startsWith("/admin")) active = "poi";
        model.addAttribute("active", active);
    }
}
