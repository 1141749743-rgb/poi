package com.campus.poi.controller;

import com.campus.poi.entity.Poi;
import com.campus.poi.entity.User;
import com.campus.poi.repository.CommentRepository;
import com.campus.poi.repository.PoiRepository;
import com.campus.poi.repository.UserRepository;
import com.campus.poi.service.AdminService;
import com.campus.poi.service.PoiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * 后台管理：登录、地点 CRUD、分类、用户、评论。
 * 每个方法第一行都检查 isAdmin —— HTTP 是无状态的，不检查就等于门没锁。
 */
@Controller
public class AdminController {

    private final AdminService adminService;
    private final PoiService poiService;
    private final PoiRepository poiRepo;
    private final CommentRepository commentRepo;
    private final UserRepository userRepo;

    public AdminController(AdminService adminService, PoiService poiService,
                           PoiRepository poiRepo, CommentRepository commentRepo, UserRepository userRepo) {
        this.adminService = adminService;
        this.poiService = poiService;
        this.poiRepo = poiRepo;
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
    }

    private static boolean isAdmin(HttpSession session) {
        Object v = session.getAttribute("isAdmin");
        return v != null && ((Integer) v) == 1;
    }

    // ---------------- 管理员登录 ----------------
    @GetMapping("/admin/login")
    public String loginPage() { return "admin_login"; }

    @PostMapping("/admin/login")
    public String doLogin(@RequestParam(defaultValue = "") String username,
                          @RequestParam(defaultValue = "") String pwd,
                          HttpSession session, RedirectAttributes ra) {
        User user = userRepo.findAdmin(username.trim(), pwd);
        if (user == null) {
            ra.addFlashAttribute("flash", "管理员账号或密码错误（或该账号不是管理员）！");
            return "redirect:/admin/login";
        }
        session.setAttribute("isAdmin", 1);
        session.setAttribute("adminName", user.getUsername());
        ra.addFlashAttribute("flash", "管理员登录成功！");
        return "redirect:/admin";
    }

    @GetMapping("/admin/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.removeAttribute("isAdmin");
        session.removeAttribute("adminName");
        ra.addFlashAttribute("flash", "已退出管理员后台");
        return "redirect:/admin/login";
    }

    // ---------------- 后台首页：地点列表 ----------------
    @GetMapping("/admin")
    public String index(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("flash", "请先用管理员账号登录！");
            return "redirect:/admin/login";
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("poiN", poiRepo.listAll().size());
        stats.put("cmN", commentRepo.listAll(99999).size());
        stats.put("uN", userRepo.listAllWithCounts().size());

        model.addAttribute("poiList", adminService.allPois());
        model.addAttribute("commentList", adminService.latestComments());
        model.addAttribute("stats", stats);
        return "admin";
    }

    // ---------------- 地点 CRUD ----------------
    private Poi readPoiForm(@RequestParam Map<String, String> form) {
        Poi p = new Poi();
        p.setName(form.getOrDefault("name", "").trim());
        try { p.setCategoryId(Integer.valueOf(form.getOrDefault("category_id", "").trim())); }
        catch (Exception e) { p.setCategoryId(null); }

        String lng = form.getOrDefault("lng", "").trim();
        p.setLng(lng.isEmpty() ? null : Double.valueOf(lng));
        String lat = form.getOrDefault("lat", "").trim();
        p.setLat(lat.isEmpty() ? null : Double.valueOf(lat));

        p.setAddress(blankToNull(form.get("address")));
        p.setTags(blankToNull(form.get("tags")));
        p.setBusinessHour(blankToNull(form.get("business_hour")));
        p.setIntro(blankToNull(form.get("intro")));
        p.setCoverImg(blankToNull(form.get("cover_img")));

        // 注意：不能用 `status == null ? 1 : status` 之外的写法把 0 吃掉
        String status = form.get("status");
        p.setStatus(status == null || status.trim().isEmpty() ? 1 : Integer.valueOf(status.trim()));
        return p;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @GetMapping("/admin/poi/add")
    public String addForm(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("cates", adminService.categories());
        model.addAttribute("poi", null);
        model.addAttribute("mode", "add");
        return "admin_poi_form";
    }

    @PostMapping("/admin/poi/add")
    public String addSubmit(@RequestParam Map<String, String> form, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Poi p = readPoiForm(form);
        if (p.getName().isEmpty() || p.getCategoryId() == null) {
            ra.addFlashAttribute("flash", "名称和分类是必填项！");
            return "redirect:/admin/poi/add";
        }
        adminService.savePoi(p, true);
        ra.addFlashAttribute("flash", "已新增地点「" + p.getName() + "」");
        return "redirect:/admin";
    }

    @GetMapping("/admin/poi/edit/{pid}")
    public String editForm(@PathVariable int pid, Model model, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Poi poi = adminService.findPoi(pid);
        if (poi == null) {
            ra.addFlashAttribute("flash", "要编辑的地点不存在！");
            return "redirect:/admin";
        }
        model.addAttribute("cates", adminService.categories());
        model.addAttribute("poi", poi);
        model.addAttribute("mode", "edit");
        return "admin_poi_form";
    }

    @PostMapping("/admin/poi/edit/{pid}")
    public String editSubmit(@PathVariable int pid, @RequestParam Map<String, String> form,
                             HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Poi p = readPoiForm(form);
        if (p.getName().isEmpty() || p.getCategoryId() == null) {
            ra.addFlashAttribute("flash", "名称和分类是必填项！");
            return "redirect:/admin/poi/edit/" + pid;
        }
        p.setId(pid);
        adminService.savePoi(p, false);
        ra.addFlashAttribute("flash", "修改已保存");
        return "redirect:/admin";
    }

    @PostMapping("/admin/poi/delete/{pid}")
    public String delete(@PathVariable int pid, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        adminService.deletePoi(pid);
        ra.addFlashAttribute("flash", "地点及其相关收藏/评论/浏览记录已删除");
        return "redirect:/admin";
    }

    @PostMapping("/admin/poi/toggle/{pid}")
    public String toggle(@PathVariable int pid, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        adminService.togglePoi(pid);
        ra.addFlashAttribute("flash", "上架/下架状态已切换");
        return "redirect:/admin";
    }

    // ---------------- 评论 ----------------
    @PostMapping("/admin/comment/delete/{cid}")
    public String deleteComment(@PathVariable int cid, HttpSession session, RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        poiService.deleteComment(cid);
        ra.addFlashAttribute("flash", "评论已删除，平均分已重算");
        // 从评论管理页来的就回评论管理页，从后台首页来的就回后台首页
        return referer != null && referer.contains("/admin/comments")
                ? "redirect:/admin/comments" : "redirect:/admin";
    }

    @GetMapping("/admin/comments")
    public String comments(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("flash", "请先用管理员账号登录！");
            return "redirect:/admin/login";
        }
        model.addAttribute("commentList", adminService.allComments());
        return "admin_comments";
    }

    // ---------------- 分类 ----------------
    @GetMapping("/admin/categories")
    public String categories(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("flash", "请先用管理员账号登录！");
            return "redirect:/admin/login";
        }
        model.addAttribute("cateList", adminService.categoriesWithCount());
        return "admin_categories";
    }

    @PostMapping("/admin/category/add")
    public String addCategory(@RequestParam(defaultValue = "") String name,
                              HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        ra.addFlashAttribute("flash", adminService.addCategory(name.trim()));
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/category/edit/{cid}")
    public String editCategory(@PathVariable int cid, @RequestParam(defaultValue = "") String name,
                               HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        ra.addFlashAttribute("flash", adminService.renameCategory(cid, name.trim()));
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/category/delete/{cid}")
    public String deleteCategory(@PathVariable int cid, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        ra.addFlashAttribute("flash", adminService.deleteCategory(cid));
        return "redirect:/admin/categories";
    }

    // ---------------- 用户 ----------------
    @GetMapping("/admin/users")
    public String users(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("flash", "请先用管理员账号登录！");
            return "redirect:/admin/login";
        }
        model.addAttribute("userList", adminService.users());
        return "admin_users";
    }

    @PostMapping("/admin/user/toggle/{uid}")
    public String toggleUser(@PathVariable int uid, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Integer currentUid = (Integer) session.getAttribute("userId");
        String adminName = (String) session.getAttribute("adminName");
        String username = (String) session.getAttribute("username");
        ra.addFlashAttribute("flash", adminService.toggleUser(uid, currentUid, adminName, username));
        return "redirect:/admin/users";
    }
}
