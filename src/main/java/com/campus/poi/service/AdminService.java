package com.campus.poi.service;

import com.campus.poi.entity.Category;
import com.campus.poi.entity.Comment;
import com.campus.poi.entity.Poi;
import com.campus.poi.entity.User;
import com.campus.poi.repository.CategoryRepository;
import com.campus.poi.repository.CommentRepository;
import com.campus.poi.repository.PoiRepository;
import com.campus.poi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 后台管理相关业务：地点 CRUD、分类 CRUD、用户角色、评论列表 */
@Service
public class AdminService {

    private final PoiRepository poiRepo;
    private final CategoryRepository cateRepo;
    private final CommentRepository commentRepo;
    private final UserRepository userRepo;

    public AdminService(PoiRepository poiRepo, CategoryRepository cateRepo,
                        CommentRepository commentRepo, UserRepository userRepo) {
        this.poiRepo = poiRepo;
        this.cateRepo = cateRepo;
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
    }

    // ---------- 地点 ----------
    public List<Poi> allPois() { return poiRepo.listAll(); }
    public List<Comment> latestComments() { return commentRepo.listLatest(15); }
    public List<Comment> allComments() { return commentRepo.listAll(200); }
    public List<Category> categories() { return cateRepo.listAll(); }
    public List<Category> categoriesWithCount() { return cateRepo.listWithCount(); }
    public List<User> users() { return userRepo.listAllWithCounts(); }

    public Poi findPoi(int id) { return poiRepo.findById(id); }

    public void savePoi(Poi poi, boolean isNew) {
        if (isNew) poiRepo.insert(poi);
        else poiRepo.update(poi);
    }

    public void deletePoi(int id) { poiRepo.deleteCascade(id); }

    public void togglePoi(int id) { poiRepo.toggleStatus(id); }

    // ---------- 分类 ----------
    @Transactional
    public String addCategory(String name) {
        if (name == null || name.isBlank()) return "分类名不能为空！";
        if (cateRepo.existsByName(name)) return "分类「" + name + "」已经存在";
        cateRepo.insert(name);
        return "已新增分类「" + name + "」";
    }

    public String renameCategory(int id, String name) {
        if (name == null || name.isBlank()) return "分类名不能为空！";
        cateRepo.rename(id, name);
        return "分类名已修改";
    }

    /** 分类下还有地点就拒绝删除，避免产生孤儿数据 */
    public String deleteCategory(int id) {
        if (poiRepo.countByCategory(id) > 0) {
            return "这个分类下还有地点，先把地点移走或删掉再删分类";
        }
        cateRepo.delete(id);
        return "分类已删除";
    }

    // ---------- 用户 ----------
    @Transactional
    public String toggleUser(int uid, Integer currentUserId, String adminName, String username) {
        // 不能把自己降级，避免把管理员锁在门外
        if (currentUserId != null && uid == currentUserId && adminName != null && adminName.equals(username)) {
            return "不能取消自己的管理员权限";
        }
        User u = userRepo.findById(uid);
        if (u == null) return "用户不存在";
        userRepo.toggleAdmin(uid);
        return "用户「" + u.getUsername() + "」的管理员权限已切换为 "
                + (u.getIsAdmin() == 1 ? "普通用户" : "管理员");
    }
}
