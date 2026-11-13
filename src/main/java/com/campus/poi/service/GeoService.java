package com.campus.poi.service;

import com.campus.poi.entity.Poi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 地理与营业状态工具（对应 Python 版的 haversine_m / fmt_distance / walk_minutes /
 * is_open_now / is_late_open / decorate_poi / pretty_intro）。
 * 写成 @Service，别的类注入就能用。
 */
@Service
public class GeoService {

    @Value("${campus.lng}")
    private double campusLng;

    @Value("${campus.lat}")
    private double campusLat;

    @Value("${campus.walk-speed:75}")
    private double walkSpeed;

    /** 两点球面距离（米），haversine 公式 */
    public int haversineM(double lng1, double lat1, double lng2, double lat2) {
        final double R = 6371000;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = p2 - p1;
        double dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                 + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return (int) Math.round(2 * R * Math.asin(Math.sqrt(a)));
    }

    /** 距离友好显示：1km 内用 m，超过用 km（保留 1 位） */
    public String fmtDistance(Integer meters) {
        if (meters == null) return "—";
        if (meters < 1000) return meters + " m";
        return String.format("%.1f km", meters / 1000.0);
    }

    /** 按步行速度估算分钟，最少 1 分钟 */
    public Integer walkMinutes(Integer meters) {
        if (meters == null) return null;
        return Math.max(1, (int) Math.round(meters / walkSpeed));
    }

    /** 解析 "08:00-22:00" 里的起止时间，格式不对返回 null */
    private LocalTime[] parseRange(String businessHour) {
        if (businessHour == null) return null;
        String bh = businessHour.trim();
        if (!bh.contains("-")) return null;
        try {
            String s = bh.substring(0, bh.indexOf('-')).trim();
            String e = bh.substring(bh.indexOf('-') + 1).trim();
            return new LocalTime[]{LocalTime.parse(s), LocalTime.parse(e)};
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean is24h(String businessHour) {
        if (businessHour == null) return false;
        String bh = businessHour.trim();
        return bh.contains("24") && (bh.contains("小时") || bh.toLowerCase().contains("h"));
    }

    /** 现在是否营业：true 营业 / false 打烊 / null 没填或认不出 */
    public Boolean isOpenNow(String businessHour, LocalTime now) {
        if (businessHour == null || businessHour.isBlank()) return null;
        if (is24h(businessHour)) return true;
        LocalTime[] r = parseRange(businessHour);
        if (r == null) return null;
        LocalTime t = (now == null) ? LocalTime.now() : now;
        if (!r[1].isBefore(r[0])) {
            return !t.isBefore(r[0]) && !t.isAfter(r[1]);
        }
        // 跨夜，如 18:00-02:00
        return !t.isBefore(r[0]) || !t.isAfter(r[1]);
    }

    /** 是否营业到 22 点以后（正好 22:00 关门不算） */
    public boolean isLateOpen(String businessHour) {
        if (businessHour == null || businessHour.isBlank()) return false;
        if (is24h(businessHour)) return true;
        LocalTime[] r = parseRange(businessHour);
        if (r == null) return false;
        if (!r[1].isAfter(r[0])) return true;   // 跨夜
        return r[1].isAfter(LocalTime.of(22, 0));
    }

    /** 给 POI 补上距离、步行分钟、是否营业（对应 decorate_poi） */
    public Poi decorate(Poi poi, LocalTime now) {
        poi.setDistanceM(null);
        poi.setDistanceText("—");
        poi.setWalkMin(null);
        poi.setOpenNow(isOpenNow(poi.getBusinessHour(), now));

        if (poi.getLng() != null && poi.getLat() != null) {
            int m = haversineM(poi.getLng(), poi.getLat(), campusLng, campusLat);
            poi.setDistanceM(m);
            poi.setDistanceText(fmtDistance(m));
            poi.setWalkMin(walkMinutes(m));
        }
        return poi;
    }

    public Poi decorate(Poi poi) {
        return decorate(poi, null);
    }

    /**
     * 简介清洗：把"餐饮服务;中餐厅;中餐厅"变成"中餐厅"。
     * 原版是 Jinja 的自定义过滤器，Java 版做成静态方法，实体 getter 里调用。
     */
    public static String prettyIntro(String text) {
        if (text == null || text.isBlank()) return "暂无简介";
        String[] raw = text.split("[;；]");
        Set<String> uniq = new LinkedHashSet<>();
        for (String p : raw) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (t.equals("餐饮服务") || t.equals("购物服务") || t.equals("生活服务")) continue;
            uniq.add(t);
        }
        if (uniq.isEmpty()) return "暂无简介";
        return String.join(" · ", uniq);
    }

    public double getCampusLng() { return campusLng; }
    public double getCampusLat() { return campusLat; }

    /** 标签拆分成列表（"安静,通宵,有插座" → [安静, 通宵, 有插座]） */
    public static List<String> splitTags(String tags) {
        List<String> list = new ArrayList<>();
        if (tags == null || tags.isBlank()) return list;
        for (String t : tags.split(",")) {
            String s = t.trim();
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }
}
