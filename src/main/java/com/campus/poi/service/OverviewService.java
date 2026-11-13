package com.campus.poi.service;

import com.campus.poi.entity.Poi;
import com.campus.poi.repository.PoiRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 首页总览：三个大数字 + 生活圈雷达图。
 * 对应 Python 版 campus_overview()，把一次查询的结果同时用于数字和图形，
 * 保证"首页说 131，图上也是 131"。
 */
@Service
public class OverviewService {

    private final PoiRepository poiRepo;
    private final GeoService geo;

    /** 分类配色：雷达图上的点和图例同色，一色一义 */
    public static final Map<String, String> CATEGORY_COLORS = new LinkedHashMap<>();
    public static final String DEFAULT_COLOR = "#94a3b8";

    static {
        CATEGORY_COLORS.put("餐饮美食", "#f97316");
        CATEGORY_COLORS.put("自习室", "#3b82f6");
        CATEGORY_COLORS.put("打印店", "#8b5cf6");
        CATEGORY_COLORS.put("快递驿站", "#10b981");
        CATEGORY_COLORS.put("便利店", "#14b8a6");
        CATEGORY_COLORS.put("饮品店", "#ec4899");
        CATEGORY_COLORS.put("运动场地", "#84cc16");
        CATEGORY_COLORS.put("超市", "#eab308");
        CATEGORY_COLORS.put("药店", "#ef4444");
        CATEGORY_COLORS.put("理发店", "#06b6d4");
        CATEGORY_COLORS.put("水果店", "#f43f5e");
    }

    public static final int RADAR_CENTER = 210;

    /** 米 → 像素：0.35 次方压缩，300m≈80px、1000m≈122px、3000m≈181px */
    public static double mToPx(double meters) {
        return 10.8 * Math.pow(Math.max(meters, 1), 0.35);
    }

    public OverviewService(PoiRepository poiRepo, GeoService geo) {
        this.poiRepo = poiRepo;
        this.geo = geo;
    }

    /** 雷达图上的一个点 */
    public static class Dot {
        private int id;
        private String name;
        private String cat;
        private double x;
        private double y;
        private double size;
        private String color;
        private double dist;
        private String distText;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCat() { return cat; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getSize() { return size; }
        public String getColor() { return color; }
        public double getDist() { return dist; }
        public String getDistText() { return distText; }
    }

    /** 图例 / 分类计数 / 圈层计数 */
    public static class Legend {
        public String name; public String color; public int n; public int id;
    }
    public static class Ring { public String label; public int n; }

    public static class Overview {
        private int total;
        private int near;
        private int openNow;
        private List<Dot> dots = new ArrayList<>();
        private List<Legend> legend = new ArrayList<>();
        private List<Legend> cats = new ArrayList<>();
        private List<Ring> rings = new ArrayList<>();

        public int getTotal() { return total; }
        public int getNear() { return near; }
        public int getOpenNow() { return openNow; }
        public List<Dot> getDots() { return dots; }
        public List<Legend> getLegend() { return legend; }
        public List<Legend> getCats() { return cats; }
        public List<Ring> getRings() { return rings; }
    }

    public Overview build() {
        List<Poi> rows = new ArrayList<>();
        for (Poi p : poiRepo.listActive()) {
            rows.add(geo.decorate(p));
        }

        Overview ov = new Overview();
        ov.total = rows.size();
        ov.near = (int) rows.stream().filter(p -> p.getDistanceM() != null && p.getDistanceM() <= 750).count();
        ov.openNow = (int) rows.stream().filter(p -> Boolean.TRUE.equals(p.getOpenNow())).count();

        // ---- 把经纬度差换算成米级平面坐标，再压缩成像素 ----
        // 1 度纬度≈111km；广州 1 度经度≈102km
        List<Dot> dots = new ArrayList<>();
        for (Poi p : rows) {
            if (p.getDistanceM() == null || p.getLng() == null || p.getLat() == null) continue;

            double dx = (p.getLng() - geo.getCampusLng()) * 102000;
            double dy = (p.getLat() - geo.getCampusLat()) * 111000;
            double r = Math.hypot(dx, dy);
            if (r == 0) r = 1;

            double px = mToPx(r);
            Dot d = new Dot();
            d.id = p.getId();
            d.name = p.getName();
            d.cat = p.getCategoryName() == null ? "其他" : p.getCategoryName();
            d.x = round(RADAR_CENTER + dx / r * px);
            d.y = round(RADAR_CENTER - dy / r * px);
            d.size = round(6.4 - Math.min(r, 1000) / 1000 * 2.4);
            d.color = CATEGORY_COLORS.getOrDefault(d.cat, DEFAULT_COLOR);
            d.dist = r;
            d.distText = p.getDistanceText();
            dots.add(d);
        }
        dots.sort(Comparator.comparingDouble(a -> a.dist));

        // 选点按圈层分配名额（7/5/4），避免最近的 16 家全挤在圆心
        int[] quotas = {7, 5, 4};
        double[][] ranges = {{0, 300}, {300, 1000}, {1000, 3000}};
        List<Dot> picks = new ArrayList<>();
        for (int i = 0; i < quotas.length; i++) {
            final double lo = ranges[i][0], hi = ranges[i][1];
            List<Dot> inRing = new ArrayList<>();
            for (Dot d : dots) if (d.dist >= lo && d.dist < hi) inRing.add(d);
            picks.addAll(inRing.subList(0, Math.min(quotas[i], inRing.size())));
        }
        ov.dots = picks;

        // 圈层数量
        Object[][] rings = {{0, 300, "300m 校内"}, {300, 1000, "1km 生活带"},
                {1000, 3000, "3km 城区"}, {3000, 1000000000, "3km 以外"}};
        for (Object[] rg : rings) {
            double lo = ((Number) rg[0]).doubleValue();
            double hi = ((Number) rg[1]).doubleValue();
            int n = 0;
            for (Poi p : rows) {
                if (p.getDistanceM() != null && p.getDistanceM() >= lo && p.getDistanceM() < hi) n++;
            }
            if (n > 0) { Ring ring = new Ring(); ring.label = (String) rg[2]; ring.n = n; ov.rings.add(ring); }
        }

        // 图例：只列出图上出现了的分类
        Set<String> seen = new LinkedHashSet<>();
        for (Dot d : picks) {
            if (seen.add(d.cat)) {
                Legend lg = new Legend();
                lg.name = d.cat;
                lg.color = d.color;
                ov.legend.add(lg);
            }
        }

        // 分类计数（按数量从多到少）
        Map<String, Legend> counts = new LinkedHashMap<>();
        for (Poi p : rows) {
            String name = p.getCategoryName() == null ? "其他" : p.getCategoryName();
            Legend lg = counts.computeIfAbsent(name, k -> {
                Legend l = new Legend();
                l.name = k;
                l.color = CATEGORY_COLORS.getOrDefault(k, DEFAULT_COLOR);
                l.n = 0;
                l.id = 0;
                return l;
            });
            lg.n++;
            if (lg.id == 0 && p.getCategoryId() != null) lg.id = p.getCategoryId();
        }
        ov.cats = new ArrayList<>(counts.values());
        ov.cats.sort((a, b) -> b.n - a.n);
        return ov;
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
