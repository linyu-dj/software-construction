# Git + 开发博客 操作手册（Gitee 版）

> 适用：软件构造综合实践 · 20 阶增量迭代
> 前置环境（已检测通过）：Git 2.45.1 已装，JDK 8 已装，IDEA 2026.1.4 已装
> 本文所有命令都在 **PowerShell** 里执行，复制粘贴即可

---

## 第 0 步：我已经替你做好的部分

| 配置项 | 值 | 作用 |
|---|---|---|
| `user.name` | 徐文昊 | 提交记录里的作者名 |
| `core.quotepath` | false | 中文文件名在 git 里正常显示，不变成 `\346\226\207` |
| `init.defaultBranch` | main | 新建仓库默认分支为 main |
| `core.longpaths` | true | 支持超长路径，避免深层目录报错 |
| `http.postBuffer` | 500 MB | 大批量推送不易失败 |
| 仓库骨架 | `桌面\软件构造\` | 已建 `docs/blog`、`.gitignore`、`README.md`、博客模板 |

系统级已自带：`core.autocrlf=true`（换行符自动转换）、`credential.helper=manager`（凭据自动记住）。

---

## 第 1 步：补一个邮箱（唯一还没配的项）

提交作者需要邮箱，且**要和 Gitee 账号绑定的邮箱一致**，否则 Gitee 的贡献统计不认你。

```powershell
git config --global user.email "你的邮箱@example.com"
```

验证：

```powershell
git config --global user.name
git config --global user.email
```

---

## 第 2 步：注册 Gitee 并实名认证

1. 打开 https://gitee.com → 右上角「注册」，用手机号注册
2. 登录后 → 右上角头像 → **设置** → 左侧 **实名认证**
3. 提交真实姓名 + 身份证号 + 身份证正反面照片

> **为什么现在就要认证**：Gitee Pages（博客网页托管）**必须实名认证后才能开通**，这是平台的硬性要求。认证通常 1 个工作日内通过。
> 顺带一个限制：免费版 Pages **不支持自动部署**，每次 `push` 后要手动点一次「更新」按钮；自动部署是 Gitee Pages Pro（99 元/年）。课程作业用免费版完全够。

---

## 第 3 步：在 Gitee 创建远程仓库（关键：建空仓库）

1. 右上角 **+** → **新建仓库**
2. 填写：
   - **仓库名称**：`software-construction`（或 `ruangong`，全小写英文，别用中文）
   - **仓库介绍**：软件构造综合实践 · 校园资源预约系统 · 20 阶增量迭代
   - **是否开源**：选 **公开**（私有仓库老师打不开，直接算没交）
3. **下面三个初始化选项全部不要勾**：
   - ❌ 使用 Readme 文件初始化这个仓库
   - ❌ 使用 .gitignore 文件初始化这个仓库
   - ❌ 使用模版文件初始化这个仓库

> **为什么必须留空**：本地已经有 README 和文件了。如果远程也初始化，两边会有两份互不相干的提交历史，第一次 push 就会被拒（`rejected - non-fast-forward`），得额外做一次合并，纯属给自己找麻烦。

4. 创建成功后，记下仓库地址，形如：
   `https://gitee.com/你的用户名/software-construction.git`

---

## 第 4 步：配置认证方式（SSH 推荐，一次配置永久免密）

### 方案 A：SSH 密钥（推荐）

**① 生成密钥**（一路回车，口令留空即可）：

```powershell
ssh-keygen -t ed25519 -C "你的邮箱@example.com"
```

**② 复制公钥内容**（复制整段输出，从 `ssh-ed25519` 到邮箱结尾）：

```powershell
Get-Content "$env:USERPROFILE\.ssh\id_ed25519.pub"
```

**③ 粘贴到 Gitee**：

Gitee → 头像 → **设置** → 左侧 **SSH 公钥** → 标题随便填（如 `我的笔记本`）→ 粘贴公钥 → 确定

**④ 测试连通性**：

```powershell
ssh -T git@gitee.com
```

第一次会问 `Are you sure you want to continue connecting?` → 输入 `yes` 回车。
看到 `Hi 你的用户名! You've successfully authenticated...` 就成功了。

### 方案 B：HTTPS + 私人令牌（不想碰 SSH 就用这个）

1. Gitee → 设置 → 左侧 **私人令牌** → 生成新令牌 → 勾选 `projects` 权限 → 生成
2. **令牌只显示一次，立刻复制存好**
3. 推送时用户名填 Gitee 用户名，密码填**这串令牌**（不是登录密码）
4. Windows 凭据管理器会记住，之后不再输入

> 用方案 A 的话，仓库地址是 `git@gitee.com:用户名/software-construction.git`
> 用方案 B 的话，是 `https://gitee.com/用户名/software-construction.git`

---

## 第 5 步：本地初始化并首次推送

```powershell
cd C:\Users\18723\Desktop\软件构造

# 如果目录里还没有 .git，先初始化（我建骨架时已初始化过，这一步会自动跳过）
git init -b main

# 添加所有文件
git add -A

# 首次提交
git commit -m "chore: 初始化仓库骨架、.gitignore 与开发博客目录"

# 关联远程仓库（把地址换成你自己的，SSH 版示例）
git remote add origin git@gitee.com:你的用户名/software-construction.git

# 首次推送并建立追踪关系
git push -u origin main
```

推送成功后刷新 Gitee 页面，能看到 README、docs、.gitignore 就成功了。

**验证三连**：

```powershell
git remote -v          # 应显示 origin 的 fetch/push 地址
git log --oneline      # 应有一条提交
git status             # 应显示 working tree clean
```

---

## 第 6 步：IDEA 接入 Git

IDEA 通常能自动识别，但必须点一次 Test 确认，否则会出现"IDEA 里 Git 灰着用不了"。

1. IDEA → **File → Settings**（`Ctrl+Alt+S`）
2. 左侧 **Version Control → Git**
3. `Path to Git executable` 填：`C:\Program Files\Git\cmd\git.exe`
4. 点 **Test** → 弹出 `Git version is 2.45.1` 即成功
5. 同一页若提示 "Git user email is not configured"，说明第 1 步没做，回去补

提交界面就会出现在右上角（`Ctrl+K` 提交 / `Ctrl+Shift+K` 推送）。

**在 IDEA 里打开哪个目录**：打开 `桌面\软件构造`（仓库根），不要打开某个 Task 子目录。这样 20 个任务共用一个仓库，Git 面板一眼能看到完整迭代历史。

---

## 第 7 步：每个任务的日常提交流程（背下来，20 次都这样）

### IDEA 图形化（推荐）

1. `Ctrl+K` 打开提交窗口 → 勾选要提交的文件（**别把 `out/`、`.idea/` 勾进去**，.gitignore 已挡住）
2. 填提交信息：`feat(V1.1): 实现用户注册与账号查重`
3. 点 **Commit**（不是 Commit and Push，先本地提交）
4. `Ctrl+Shift+K` 推送
5. 打标签：左侧 **Git** 工具窗口 → **Log** 标签页 → 右键最新提交 → **New Tag** → 输入 `v1.1` → 推送标签

### 命令行（等价，复制用）

```powershell
cd C:\Users\18723\Desktop\软件构造

git add -A
git commit -m "feat(V1.1): 实现用户注册与账号查重"
git tag -a v1.1 -m "V1.1 用户注册功能"
git push origin main
git push origin v1.1
```

### 提交信息规范（老师会看）

| 前缀 | 用途 | 示例 |
|---|---|---|
| `feat(版本)` | 新增功能 | `feat(V2.0): 新增校园资源 CRUD` |
| `refactor(版本)` | 重构不改功能 | `refactor(V3.1): 拆分为三层架构` |
| `fix(版本)` | 修 bug | `fix(V1.3): 修复重启后数据丢失` |
| `docs` | 文档博客 | `docs: 补充 V1.3 迭代博客` |
| `chore` | 杂项配置 | `chore: 更新 .gitignore` |

**绝对不要**：`update`、`修改`、`1`、`提交` 这种信息。20 个任务 20 条有效提交 = 迭代过程可追溯，这是老师给"增量迭代"打分的直接证据。

---

## 第 8 步：搭开发博客

### 方案 A：Gitee Pages（有网页链接，推荐）

前提：第 2 步实名认证已通过。

1. 进入仓库页 → 顶部 **服务** → **Gitee Pages**
2. 部署设置：
   - **部署分支**：`main`
   - **部署目录**：`docs`
   - 若下拉框里没有 `docs` 选项，就选根目录 `/`，并把 `docs/index.md` 复制一份到仓库根目录当首页
3. 点 **启动**（首次）→ 生成访问地址：`https://你的用户名.gitee.io/software-construction`
4. **以后每次推送完博客，都要回这个页面点一次「更新」**（免费版没有自动部署）

### 方案 B：仓库内 Markdown 直接读（最省事）

不做 Pages，博客就是 `docs/blog/` 下的 `.md` 文件。Gitee 网页能直接渲染 Markdown，老师点进去就能看，图片也能显示。
缺点是没有独立博客站点，但课程作业通常认这个。

### 方案 C：双保险（我的建议）

`docs/blog/` 里存 Markdown 作为存档，同时把同一篇内容发到 **CSDN / 博客园**（注册即写、免审核、国内可访问），在 README 里放 CSDN 链接。
好处：老师一点就开，不会遇到 Pages 打不开或需要手动更新的尴尬；坏处是要复制两遍。

> 判断标准：老师只要"能看到开发过程"就选 B；老师要求"有博客链接/能访问"就选 A 或 C。**先按 C 做，最稳。**

---

## 第 9 步：每篇博客怎么写

模板已放在 `docs/blog/模板-迭代开发日志.md`，直接复制改名。八个小节：

1. 本轮要解决什么问题（上一版的痛点）
2. 改动清单（文件 / 类表格）
3. 关键设计与实现（核心代码 + 为什么这么写）
4. **踩过的坑**（最重要，证明是你自己写的）
5. 验收自测（用例表格 + 运行截图）
6. 设计原则 / 模式落地
7. 下一轮计划
8. 提交号与 tag

写作节奏（关键，别攒到最后补）：
**每完成一个任务 → 提交 → 打 tag → 立刻写这一篇**。20 篇写下来正好对应 20 次提交，博客和 Git 历史完全对得上，这是最能拿分的地方。

图片放 `docs/blog/images/`，命名 `01-login.png`、`01-register.png`，和博客同名序号。

---

## 第 10 步：排错速查

| 报错 / 现象 | 原因 | 解决 |
|---|---|---|
| `fatal: not a git repository` | 当前目录不是仓库 | `cd C:\Users\18723\Desktop\软件构造` |
| `Permission denied (publickey)` | SSH 公钥没加到 Gitee | 重做第 4 步 ②③④ |
| `Authentication failed` / 401 | HTTPS 用了登录密码而非令牌 | 密码位置填私人令牌 |
| `rejected - non-fast-forward` | 远程仓库不是空的 | 远程删库重建为**空仓库**，或 `git pull --rebase origin main` 后重推 |
| 中文文件名显示成 `\346\226\207` | quotepath 设置 | 已配好，若仍出现执行 `git config --global core.quotepath false` |
| `LF will be replaced by CRLF` | 换行符转换提示 | 无害警告，忽略 |
| push 卡住/超时 | 网络问题 | 重试；或 `git config --global http.postBuffer 524288000`（已配） |
| IDEA 里 Git 菜单灰色 | Git 路径未确认 | 第 6 步 Test 一次 |
| `error: GH001: Large files detected` 之类 | 把 `out/`、压缩包提交了 | `.gitignore` 已挡；若已提交，`git rm -r --cached out` 后重提 |
| 提交写错了想改 | — | `git reset --soft HEAD~1` 撤销提交但保留改动 |

**三条铁律**：

1. **永远不要** `git push -f`（强推会覆盖历史，作业直接判废）
2. **永远不要**提交 `out/`、`.idea/`、`.class`
3. 每完成一步先 `git status` 看一眼，确认要提交的文件清单

---

## 交作业前检查清单

- [ ] 仓库是 **public**，用浏览器未登录状态也能打开
- [ ] `git log --oneline` 里至少有 20 条有意义的提交（每个任务一条）
- [ ] 20 个 tag 与 20 篇博客一一对应
- [ ] README 里有任务索引表和博客链接
- [ ] 仓库里没有 `out/`、`.idea/`、`.class` 等垃圾文件
- [ ] 最新代码是 V6.0，且能正常编译运行
- [ ] 博客有运行截图，不是纯文字
- [ ] 记录下仓库地址，交作业时直接填
