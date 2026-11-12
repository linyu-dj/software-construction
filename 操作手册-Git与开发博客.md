# Git + 开发博客 操作手册（GitHub 版）

> 适用：软件构造综合实践 · 20 阶增量迭代
> 这台机器的状态：Git 2.45.1、JDK 8、IDEA 2026.1.4 都装好了，仓库已建、代码已推、博客已上线
> 下面命令都在 PowerShell 里执行

## 0. 现在已经配好的（不需要重做）

| 配置项 | 当前值 | 说明 |
|---|---|---|
| GitHub 账号 | `linyu-dj` | 仓库所有者 |
| 仓库地址（SSH） | `git@github.com:linyu-dj/software-construction.git` | 已设为 `origin` |
| 仓库地址（网页） | https://github.com/linyu-dj/software-construction | 公开仓库 |
| 博客站点 | https://linyu-dj.github.io/software-construction/ | Pages 已开启 |
| 提交作者 | 徐文昊 / `202483670082@nuist.edu.cn` | `user.name` / `user.email` 已写进全局配置 |
| SSH 密钥 | `~/.ssh/id_ed25519`，公钥标题 `NUIST-laptop-2026` | 已加进 GitHub，推送免密 |
| git 代理 | `http.proxy = http://127.0.0.1:7897` | Clash Verge 的本地端口，不配会 `Connection was reset` |
| 中文文件名 | `core.quotepath = false` | 不会显示成 `\346\226\207` |

想核对一遍：

```powershell
git config --global --list
ssh -T git@github.com
```

`ssh -T` 应该返回 `Hi linyu-dj! You've successfully authenticated, but GitHub does not provide shell access.`

## 1. 代理这件事必须先说清楚

这台机器上 GitHub 直连不通，全靠 Clash Verge 走代理。但**git 不认系统代理**，它只看自己配置里的 `http.proxy`。所以：

```powershell
git config --global http.proxy http://127.0.0.1:7897
git config --global https.proxy http://127.0.0.1:7897
```

不配这条，`git push` 会报 `Recv failure: Connection was reset`，看起来像 GitHub 挂了，其实只是 git 在裸连。

副作用也要知道：以后关掉 Clash，所有 HTTPS 的 git 操作都会失败。要临时取消代理：

```powershell
git config --global --unset http.proxy
git config --global --unset https.proxy
```

好消息是**日常推送走的是 SSH，不经过这个代理配置**，只要 Clash 开着（或你换了别的能连 GitHub 的方式）就没事。

## 2. 认证方式：为什么最后选了 SSH

一开始想用 HTTPS + 凭据管理器（GCM）。结果是：在后台终端里 push 时，GCM 要你在浏览器点授权，但命令行没有交互界面，最后就卡在那里不动，任务跑了几分钟一点输出都没有。

所以改成了 SSH，一次配好永久免密：

**① 生成密钥**（`.ssh` 目录不存在的话先建）：

```powershell
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh"
ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\id_ed25519" -N '""' -C "202483670082@nuist.edu.cn"
Get-Content "$env:USERPROFILE\.ssh\id_ed25519.pub"
```

**② 贴到 GitHub**：打开 https://github.com/settings/ssh/new ，Title 随便写（`NUIST-laptop-2026`），Key 粘贴刚输出的整段公钥（从 `ssh-ed25519` 开头到邮箱结尾），点 Add SSH key。

**③ 测试**：

```powershell
ssh -T git@github.com
```

第一次会问 `Are you sure you want to continue connecting?`，输 `yes` 回车。看到 `Hi linyu-dj!` 就成了。

如果 22 端口被封，可以强行走 443：

```powershell
ssh -T -p 443 git@ssh.github.com
```

**④ 把远程地址换成 SSH**：

```powershell
git remote set-url origin git@github.com:linyu-dj/software-construction.git
```

## 3. 每隔一轮的日常提交（20 轮都长这样）

```powershell
cd "C:\Users\18723\Desktop\软件构造"
git status                    # 先看一眼要交什么
git add -A
git commit -m "feat(V1.1): 实现用户注册与账号查重"
git tag -a v1.1 -m "V1.1 用户注册功能"
git push origin main --tags
```

`git push origin main --tags` 一行就把提交和标签都推上去。

### 提交信息怎么写

| 前缀 | 什么时候用 | 例子 |
|---|---|---|
| `feat(V版本)` | 加功能 | `feat(V2.0): 新增校园资源增删改查` |
| `refactor(V版本)` | 只重构不改功能 | `refactor(V3.1): 拆成三层架构` |
| `fix(V版本)` | 修 bug | `fix(V1.3): 修复重启后数据丢失` |
| `docs` | 写博客、改文档 | `docs(V1.0): 新增第 1 篇迭代博客` |
| `chore` | 杂项 | `chore: 更新 .gitignore` |

别写 `update`、`修改`、`提交` 这类。老师看"增量迭代"就是看提交历史，20 条有意义的提交比什么都直观。

## 4. IDEA 接入（这一步容易卡）

配之前先知道 IDEA 的两个脾气：

**第一，它不知道 Git 装在哪。** 装了 Git ≠ IDEA 能用 Git。必须手动指一次路径：

`File → Settings（Ctrl+Alt+S）→ Version Control → Git`，*Path to Git executable* 填：

```
C:\Program Files\Git\cmd\git.exe
```

点 **Test**，出现 `Git version 2.45.1` 才算通。

**第二，新目录默认"不信任"。** 未信任状态下 IDEA 会禁掉所有 Git 命令，右下角弹的"未安装 Git / Shouldn't be possible to run a Git command in potentially untrusted project"多半就是这个原因。打开项目时看到 Trust Project 的提示，点信任。

**打开哪个目录**：打开仓库根 `C:\Users\18723\Desktop\软件构造`，不要打开 `Task01-V1.0-项目初始化` 这种子文件夹——`.git` 在根上，打开子目录 IDEA 看不到仓库。

**一个要当心的地方**：20 个任务用的是同一个包名 `campus`、同一个类名 `SystemMain`。别在根项目里把每个任务的 `src` 都标成 Sources Root，那样一份工程里会出现 20 个 `campus.SystemMain`，直接编译冲突。稳妥的做法是双开：根项目管 Git，跑代码时另开窗口打开任务文件夹，或者直接在 PowerShell 里 `.\run.bat`。

## 5. 博客站点（GitHub Pages）

已经开好了，配置是：仓库 `Settings → Pages` → Source 选 **Deploy from a branch** → 分支 `main`、目录 `/docs`。

每次 push 后自动重新构建，大概一到两分钟生效，不需要像 Gitee 那样手动点"更新"。

**唯一要养成的习惯**：`docs/index.md` 里的文章链接要用 `.html` 后缀，不是 `.md`。

```markdown
[阅读](blog/01-V1.0-项目初始化.html)
```

原因：Jekyll 会把 `xxx.md` 渲染成 `xxx.html`，站点上根本没有 `.md` 这个地址，写成 `.md` 点进去就是 404（GitHub 那个 Unicorn 页面）。第 1 篇博客上线时我就踩了这一下，全站链接改了一遍。

在那边改配置的话还有个小坑：`Settings → Pages` 里的分支和目录是两个 Primer 自定义下拉菜单，不是原生 select，脚本读选项会读到空。手动点开菜单选就行。

## 6. 每篇博客怎么写

模板在 `docs/blog/模板-迭代开发日志.md`，复制改名成 `NN-V版本-任务名.md`。

内容上，老师主要看两段：

- **踩过的坑**：写具体报错原文、当时怎么想的、最后怎么改的。这一段最像"自己写的"，也最容易露馅——编是编不出来的。
- **验收自测**：把输入和实际输出列出来，最好配上运行截图。

其他几节（改动清单、关键设计、下一轮计划）照实写就行。**别在每节末尾硬加一句总结金句**，也别把"设计原则"那条凑满——没落地就写没落地，写明下一版谁接手，比硬安一个名词可信。

节奏上：每完成一个任务就立刻写这一篇，不要攒到最后补。20 篇刚好对应 20 次提交和 20 个 tag，博客和 Git 历史一对，分数就稳。

图片放 `docs/blog/images/`，命名跟博客序号一致，比如 `01-login.png`。

## 7. 排错速查（都是这次实际遇到的）

| 现象 | 原因 | 解决 |
|---|---|---|
| `Recv failure: Connection was reset` | git 没走代理 | 配 `http.proxy`，见第 1 节 |
| `git push` 卡住几分钟没输出 | GCM 在等浏览器授权，后台终端没法应答 | 改用 SSH |
| `Permission denied (publickey)` | 公钥没加到 GitHub，或地址还是 HTTPS | 检查 `git remote -v`，重贴公钥 |
| `unable to access ... 443` | Clash 关了 | 开代理，或临时取消 http.proxy |
| `LF will be replaced by CRLF` | Windows 换行符转换 | 无害，忽略 |
| Pages 上点文章 404 | 索引链接写的是 `.md` | 改成 `.html` |
| Pages 改了没生效 | 构建要一两分钟 | 等一会，强刷 Ctrl+F5 |
| IDEA 提示"未安装 Git" | 没配 Git 路径 + 目录未信任 | 见第 4 节 |
| IDEA 里 Git 面板是空的 | 打开的是子文件夹 | 打开仓库根目录 |
| 中文文件名变成 `\346\226\207` | quotepath | `git config --global core.quotepath false` |

三条不能碰的红线：

1. 不要 `git push -f`，强推覆盖历史，作业直接判废
2. 不要提交 `out/`、`.idea/`、`*.class`（`.gitignore` 已经挡住了）
3. 提交前跑一次 `git status`，确认清单里没有意外文件

## 8. 交作业前自查

- [ ] 仓库是 public，退出登录状态也能打开
- [ ] `git log --oneline` 里 20 条提交，每条都能看出是哪个任务
- [ ] 20 个 tag（`v1.0` ~ `v6.0`）和 20 篇博客对得上
- [ ] README 的任务索引表和博客链接都点得开
- [ ] 仓库里没有 `out/`、`.idea/`、`*.class`
- [ ] 最新代码是 V6.0，能正常编译运行
- [ ] 博客里有运行截图
- [ ] 仓库地址记下来，交作业时直接填
