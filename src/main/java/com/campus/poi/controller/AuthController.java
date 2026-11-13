package com.campus.poi.controller;

import com.campus.poi.entity.Poi;
import com.campus.poi.entity.User;
import com.campus.poi.repository.BehaviorRepository;
import com.campus.poi.repository.UserRepository;
import com.campus.poi.service.GeoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/** 登录 / 注册 / 退出 / 我的收藏 */
@Controller
public class AuthController {

    private final UserRepository userRepo;
    private final BehaviorRepository behaviorRepo;
    private final GeoService geo;

    public AuthController(UserRepository userRepo, BehaviorRepository behaviorRepo, GeoService geo) {
        this.userRepo = userRepo;
        this.behaviorRepo = behaviorRepo;
        this.geo = geo;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam(defaultValue = "") String username,
                          @RequestParam(defaultValue = "") String pwd,
                          HttpSession session, RedirectAttributes ra) {
        User user = userRepo.findByUsernameAndPassword(username.trim(), pwd);
        if (user == null) {
            ra.addFlashAttribute("flash", "用户名或者密码错误！");
            return "redirect:/login";
        }
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        // 管理员账号从前台登录，也顺手给后台权限（和 Python 版一致的修复）
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            session.setAttribute("isAdmin", 1);
            session.setAttribute("adminName", user.getUsername());
        }
        ra.addFlashAttribute("flash", "登录成功，欢迎回来！");
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam(defaultValue = "") String username,
                             @RequestParam(defaultValue = "") String pwd,
                             @RequestParam(defaultValue = "") String nickname,
                             RedirectAttributes ra) {
        username = username.trim();
        String nick = nickname.trim().isEmpty() ? username : nickname.trim();

        if (username.isEmpty() || pwd.isEmpty()) {
            ra.addFlashAttribute("flash", "用户名和密码不能为空！");
            return "redirect:/register";
        }
        if (userRepo.existsByUsername(username)) {
            ra.addFlashAttribute("flash", "用户名已经存在，请换一个！");
            return "redirect:/register";
        }
        userRepo.insert(username, pwd, nick);
        ra.addFlashAttribute("flash", "注册成功，可以去登录啦！");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("flash", "已退出登录");
        return "redirect:/";
    }

    @GetMapping("/my")
    public String myFavorites(Model model, HttpSession session, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("userId");
        if (uid == null) {
            ra.addFlashAttribute("flash", "请先登录！");
            return "redirect:/login";
        }
        List<Poi> favList = new ArrayList<>();
        for (Poi p : behaviorRepo.listMyFavorites(uid)) {
            geo.decorate(p);
            favList.add(p);
        }
        model.addAttribute("favList", favList);
        return "my";
    }
}
