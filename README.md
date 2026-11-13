# 校园生活圈 POI 推荐平台 · Java 版

这是 Flask 版的 **Spring Boot 3.2.5 + Java 17 + Thymeleaf + JdbcTemplate** 复刻工程。
页面结构、样式、路由、业务逻辑与 Flask 版一一对应，数据库仍然是同一个 `campus_poi`，数据一条没重导。

> 前端界面截图见 `screenshots/` 目录。

---

## 1. 技术栈对照表

| 层 | Flask 版 | Java 版 | 说明 |
| --- | --- | --- | --- |
| Web 框架 | Flask | Spring Boot 3.2.5 | 路由 = `@GetMapping` / `@PostMapping` |
| 模板 | Jinja2 | Thymeleaf | `{{ x }}` → `[[${x}]]`，`{% for %}` → `th:each` |
| 数据库访问 | pymysql 手写 SQL | JdbcTemplate 手写 SQL | SQL 语句原样保留，不引入 MyBatis 以减少学习成本 |
| 会话 | Flask session（签名 Cookie） | HttpSession（服务端 Session） | 存 `userId / username / isAdmin / adminName` |
| 静态资源 | `static/css/app.css` | `resources/static/css/app.css` | 一个字没改，所以长得一模一样 |
| 端口 | 5000 | 8080 | 两个可以同时跑，方便对比 |

---

## 2. 目录结构

```
javaversion/
├── pom.xml                          # Maven 依赖清单
├── screenshots/                     # 运行效果截图
└── src/main/
    ├── java/com/campus/poi/
    │   ├── PoiApplication.java      # 启动类
    │   ├── entity/                  # 实体：Poi / Category / Comment / User
    │   ├── repository/              # 数据访问：拼 SQL、映射成对象
    │   ├── service/                 # 业务：距离计算、场景筛选、推荐、后台
    │   └── controller/              # 路由：把请求分给 service，把数据给模板
    └── resources/
        ├── application.properties   # 端口 / 数据库 / 高德 Key / 校区常量
        ├── static/css/app.css       # 设计系统样式（从 Flask 版原样复制）
        └── templates/               # 12 个 Thymeleaf 模板
```

分层为什么这么写：`controller` 只做「收参数 → 调 service → 塞 model」，
`service` 放业务规则（距离、营业时间、推荐打分），`repository` 只跟数据库打交道。
这样课程评分表里「核心业务由 Java 实现」才是真的，不算页面拼装。

---

## 3. 启动步骤

### 3.0 最快的办法：双击 `启动.bat`

在项目根目录双击 **`启动.bat`**，它会自动做三件事：

1. 读 `keys.local.bat`（本地密钥文件，不进 git），把高德 Key 注入环境变量；
2. 检查 Java 和 Maven 版本，把 `D:\maven\apache-maven-3.9.9\bin` 补进 PATH（如果没配过）；
3. 执行 `mvn spring-boot:run` 启动服务。

看到 `Tomcat started on port 8080` 就用浏览器打开 <http://127.0.0.1:8080>。
**停止服务：在那个黑窗口里按 Ctrl+C**，关掉窗口也行。

> 注意：`keys.local.bat` 里有你的 Key，已经被 `.gitignore` 排除，永远不会被提交。
> 换一台电脑演示时，把 `keys.local.bat` 也拷过去（或者手动设环境变量）即可。

### 3.1 环境确认

```powershell
java -version    # 需要 17
mvn -v           # 需要 3.8+
```

### 3.2 配置高德 Key（环境变量，不写死在代码里）

课程评分表里「API Key 明文写死 = 数据安全不合格」，所以 Key 走环境变量：

```powershell
$env:AMAP_JS_KEY="你的高德JS API Key"
$env:AMAP_SECURITY_CODE="你的安全密钥"
```

不配也能跑，只是地点详情页的地图区域会显示「地图待接入」，其余功能不受影响。

### 3.3 启动

```powershell
cd C:\Users\ZhuanZ1\Desktop\poi\javaversion
mvn spring-boot:run
```

看到 `Tomcat started on port 8080` 后，浏览器打开 <http://127.0.0.1:8080>。

管理员后台：<http://127.0.0.1:8080/admin/login>，账号 `admin / admin123`。

### 3.4 打包成 jar（可选）

```powershell
mvn -DskipTests package
java -jar target\campus-poi-0.0.1-SNAPSHOT.jar
```

---

## 4. 路由对照（与 Flask 版完全一致）

| 功能 | 路径 | 方法 |
| --- | --- | --- |
| 首页 | `/` | GET |
| 分类页 | `/category/{cid}?radius=&sort=` | GET |
| 地点详情 | `/poi/{pid}` | GET |
| 场景页 | `/scene/{key}?radius=` | GET |
| 登录 / 注册 / 退出 / 我的收藏 | `/login` `/register` `/logout` `/my` | GET+POST / GET |
| 发评论 | `/comment/add/{pid}` | POST |
| 收藏开关 | `/favorite/toggle/{pid}` | POST |
| 前台删评论（仅管理员） | `/comment/delete/{cid}` | POST |
| 后台登录 / 退出 | `/admin/login` `/admin/logout` | GET+POST / GET |
| 后台首页（地点列表） | `/admin` | GET |
| 地点增删改、上下架 | `/admin/poi/add` `/admin/poi/edit/{pid}` `/admin/poi/delete/{pid}` `/admin/poi/toggle/{pid}` | POST |
| 分类管理 | `/admin/categories`、`/admin/category/add|edit/{cid}|delete/{cid}` | GET / POST |
| 用户管理 | `/admin/users`、`/admin/user/toggle/{uid}` | GET / POST |
| 评论管理 | `/admin/comments`、`/admin/comment/delete/{cid}` | GET / POST |

---

## 5. 迁移时踩到的坑（都修了，写下来防止你下次再撞）

1. **`comment` / `user` 是 MySQL 关键字**：SQL 里必须写成 `` `comment` ``、`` `user` ``，不反引号会报语法错误。
2. **`rs.getInt("user_id")` 在结果集里没有这一列时直接抛异常**（不是返回 0）：
   后台评论列表一开始漏选了 `cm.user_id`，整页 500。JDBC 里「查什么列就用什么列」。
3. **Thymeleaf 事件属性不能拼字符串**：`th:onsubmit="...字符串..."` 会报
   *"Only variable expressions returning numbers or booleans are allowed"*。
   正确做法是把提示语放进 `data-msg`，JS 里 `confirm(this.dataset.msg)`。
4. **Properties 文件里的中文会乱码**：改成 `\uXXXX` 转义（如 `campus.name=\u5e7f\u4e1c...`），
   这样无论 IDE / 系统默认编码是什么都不会错。
5. **PowerShell 的 `Set-Content -Encoding utf8` 会加 BOM**：BOM 落到 `.java` 文件开头，
   javac 报「非法字符 '\ufeff'」。改 Java 文件后用 `[IO.File]::WriteAllText(..., UTF8Encoding($false))`。
6. **模板片段引用写法**：`th:replace="~{partials/nav}"`（文件级），
   不要写成 `~{partials/nav :: nav}`，除非文件里真的定义了 `th:fragment="nav"`。

---

## 6. 和 Flask 版对齐的关键实现

- **距离**：`GeoService.haversineM` 用 Haversine 公式算球面距离，格式化 `65 m` / `1.4 km`，
  步行时间 = 距离 / 75 米每分钟（四舍五入取整，最少 1 分钟）。
- **营业状态**：`isOpenNow` 支持 `24小时`、跨夜（`22:00-02:00`）、以及 22:00 的「深夜」判定。
- **推荐**：`RecommendService` 拿最近 10 条浏览 + 全部收藏 → 数标签频次 → 取前 3 个偏好标签
  → 候选打分 `命中数×100 + 浏览量×0.1 + 评分` → 排除看过的 → 取 Top N。
- **生活圈雷达图**：`OverviewService` 用 `距离^0.35` 映射成像素半径，按方位角摆点，
  圈层配额 7/5/4，点大小 `6.4 - min(r,1000)/1000*2.4`。
- **删除顺序**：删地点先删 `favorite`、`comment`、`browse_record` 三张子表，避免外键/孤儿数据。
- **评价重算**：删评论后重新 `ROUND(AVG(score),1)` 写回 `poi.avg_score`。
- **分类保护**：分类下还有地点时禁止删除。

---

## 7. 已知限制（如实写在这里）

- 密码仍是明文存库，和 Flask 版一致。真要上线得换 BCrypt。
- 没做 CSRF 防护（教学演示够用，正式项目必须加）。
- 后台「地点表单」页沿用 Bootstrap CDN，和 Flask 版一样是独立的一套样式。

---

## 8. Git 仓库

这个目录已经是一个完整的 git 仓库（`main` 分支），提交按「模块」拆分，不是一次性糊上去的：

```
chore: 初始化 Spring Boot 3.2.5 工程骨架
feat(entity,repository): 四个实体 + 五个 Repository
feat(service): 距离/营业时间、场景筛选、推荐打分、生活圈概览、后台业务
feat(controller): 前台 5 个 + 后台 1 个控制器
feat(view): 12 个 Thymeleaf 模板
docs: README 与运行截图
chore: 一键启动脚本
fix: 启动脚本编码与 call 问题
```

**不进仓库的东西**（写在 `.gitignore` 里）：`target/`、`*.log`、`keys.local.bat`、IDE 配置。

### 8.1 推到 Gitee（推荐，国内快）

```powershell
# 1. 在 gitee.com 上新建一个空仓库（不要勾选初始化 README）
# 2. 回到项目目录执行：
cd C:\Users\ZhuanZ1\Desktop\poi\javaversion
git remote add origin https://gitee.com/你的用户名/仓库名.git
git push -u origin main
```

### 8.2 以后每次改完代码

```powershell
git status                 # 看改了哪些文件
git add -A                 # 全部加入暂存区
git commit -m "feat: 补上 8 个单元测试"
git push                   # 推到远端
```

### 8.3 检查有没有把 Key 传上去

```powershell
git log -S "你的JS-Key前8位" --oneline
```

这条命令如果**没有任何输出**，说明 Key 没进过任何一次提交 —— 这正是课程「数据安全」要的证据，演示时可以当场跑给老师看。
