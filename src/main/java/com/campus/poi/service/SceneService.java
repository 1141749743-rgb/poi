package com.campus.poi.service;

import com.campus.poi.entity.Poi;
import com.campus.poi.repository.PoiRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 时间感知 + 场景 + 筛选。
 * 对应 Python 版的 PERIODS / PERIOD_RULES / now_period / SCENES /
 * fetch_scene_pois / pick_for_now。
 */
@Service
public class SceneService {

    private final PoiRepository poiRepo;
    private final GeoService geo;

    public SceneService(PoiRepository poiRepo, GeoService geo) {
        this.poiRepo = poiRepo;
        this.geo = geo;
    }

    /** 时段：key / 起止小时 / 标题 / 一句话说明 */
    public static class Period {
        public final String key;
        public final String label;
        public final String tip;
        public Period(String key, String label, String tip) {
            this.key = key; this.label = label; this.tip = tip;
        }
        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getTip() { return tip; }
    }

    public static final List<Period> PERIODS = List.of(
            new Period("late_night", "🌙 深夜", "这时候还开着的，基本只有 24 小时便利店"),
            new Period("breakfast", "🌅 早八", "先垫一口，别空着肚子去上课"),
            new Period("morning", "☀️ 上午", "自习和打印的好时候，人少不用排队"),
            new Period("lunch", "🍚 饭点", "食堂和小店现在都在营业"),
            new Period("afternoon", "☕ 下午", "适合自习、喝奶茶、跑腿办事"),
            new Period("dinner", "🍜 晚饭", "吃饭时间，热门店要排队"),
            new Period("evening", "🌆 晚上", "自习室和便利店还开着"),
            new Period("late", "🌙 夜深", "只剩便利店和夜宵了")
    );

    /** 每个时段的起止小时 */
    private static final int[][] PERIOD_HOURS = {
            {0, 6}, {6, 9}, {9, 11}, {11, 14}, {14, 17}, {17, 19}, {19, 22}, {22, 24}
    };

    /** 场景：一种"我现在想干什么"的诉求 */
    public static class Scene {
        public final String key, name, emoji, desc;
        public final List<String> cats;
        public final int radius;
        public final boolean onlyOpen, late;

        public Scene(String key, String name, String emoji, String desc,
                     List<String> cats, int radius, boolean onlyOpen, boolean late) {
            this.key = key; this.name = name; this.emoji = emoji; this.desc = desc;
            this.cats = cats; this.radius = radius; this.onlyOpen = onlyOpen; this.late = late;
        }
        public String getKey() { return key; }
        public String getName() { return name; }
        public String getEmoji() { return emoji; }
        public String getDesc() { return desc; }
        public List<String> getCats() { return cats; }
        public int getRadius() { return radius; }
    }

    public static final LinkedHashMap<String, Scene> SCENES = new LinkedHashMap<>();
    static {
        SCENES.put("rush", new Scene("rush", "赶时间", "⏱", "步行 5 分钟内的吃饭点",
                List.of("餐饮美食", "便利店", "饮品店"), 500, true, false));
        SCENES.put("study", new Scene("study", "通宵自习", "📚", "能坐到深夜的自习地",
                List.of("自习室"), 1000, true, true));
        SCENES.put("print", new Scene("print", "打印论文", "🖨", "1km 内能打印的地方",
                List.of("打印店"), 1000, true, false));
        SCENES.put("parcel", new Scene("parcel", "取快递", "📦", "校内外快递点",
                List.of("快递驿站"), 1000, true, false));
        SCENES.put("latenight", new Scene("latenight", "深夜食堂", "🌙", "22 点后还开着的夜宵",
                List.of("餐饮美食", "便利店", "饮品店"), 1000, true, true));
        SCENES.put("rainy", new Scene("rainy", "雨天室内", "🌧", "下雨也能待着的室内场所",
                List.of("自习室", "饮品店", "超市", "便利店"), 1000, true, false));
    }

    /** 当前处于哪个时段 */
    public Period nowPeriod() {
        int hour = LocalTime.now().getHour();
        for (int i = 0; i < PERIODS.size(); i++) {
            if (hour >= PERIOD_HOURS[i][0] && hour < PERIOD_HOURS[i][1]) return PERIODS.get(i);
        }
        return PERIODS.get(7);
    }

    /** 按时段给出"吃什么/去哪"的分类与范围 */
    public List<String> periodCats(String key) {
        switch (key) {
            case "late_night": return List.of("便利店");
            case "breakfast": return List.of("餐饮美食", "便利店", "饮品店");
            case "morning": return List.of("自习室", "打印店", "饮品店");
            case "lunch": return List.of("餐饮美食");
            case "afternoon": return List.of("自习室", "饮品店", "打印店");
            case "dinner": return List.of("餐饮美食");
            case "evening": return List.of("自习室", "饮品店", "便利店");
            default: return List.of("便利店", "餐饮美食", "饮品店");
        }
    }

    public int periodRadius(String key) {
        switch (key) {
            case "breakfast": case "lunch": case "dinner": return 500;
            default: return 1000;
        }
    }

    public boolean periodLate(String key) {
        return "late_night".equals(key) || "late".equals(key);
    }

    /**
     * 按"分类 + 距离 + 营业 + 是否开得晚"筛 POI，按距离从近到远。
     * now 传 null 用真实时间；传 LocalTime 可模拟（测试用，和 Python 版一致）。
     */
    public List<Poi> fetchScenePois(List<String> cats, int radius, boolean onlyOpen, boolean late, LocalTime now) {
        List<Poi> rows = new ArrayList<>();
        for (Poi p : poiRepo.listActive()) {
            rows.add(geo.decorate(p, now == null ? LocalTime.now() : now));
        }

        List<Poi> result = new ArrayList<>();
        for (Poi p : rows) {
            if (p.getDistanceM() == null || p.getDistanceM() > radius) continue;
            if (onlyOpen && !Boolean.TRUE.equals(p.getOpenNow())) continue;
            if (late && !geo.isLateOpen(p.getBusinessHour())) continue;
            if (cats != null && !cats.isEmpty()) {
                if (p.getCategoryName() == null || !cats.contains(p.getCategoryName())) continue;
            }
            result.add(p);
        }
        result.sort(Comparator.comparingInt(Poi::getDistanceM)
                .thenComparing(p -> -(p.getAvgScore() == null ? 0 : p.getAvgScore())));
        return result;
    }

    public List<Poi> fetchScenePois(List<String> cats, int radius, boolean onlyOpen, boolean late) {
        return fetchScenePois(cats, radius, onlyOpen, late, null);
    }

    /** 此刻适合去哪：按时段选分类，只要此刻营业的，取最近的几家 */
    public List<Poi> pickForNow(int limit) {
        Period period = nowPeriod();
        List<Poi> pois = fetchScenePois(periodCats(period.key), periodRadius(period.key),
                true, periodLate(period.key));
        return pois.stream().limit(limit).collect(Collectors.toList());
    }

    public List<Poi> filterByRadius(List<Poi> list, int radius) {
        List<Poi> out = new ArrayList<>();
        for (Poi p : list) if (p.getDistanceM() != null && p.getDistanceM() <= radius) out.add(p);
        return out;
    }
}
