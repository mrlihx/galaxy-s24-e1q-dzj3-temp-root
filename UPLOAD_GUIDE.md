# GitHub 上传指南（本机没有安装 Git）

当前构建机上没有 `git` 命令，所以推荐直接用 GitHub 网页上传；以后装了 Git 也可以按下面的命令推送。

## 方式 A：网页上传（不用 Git）

1. 打开 https://github.com/new
   - Repository name: `galaxy-s24u-dzj3-temp-root`（可自定）
   - Visibility: Public
   - License: **GNU General Public License v3.0**
   - 不要勾选 "Add a README file"（包内已有）
2. 创建后点 **Add file → Upload files**；
3. 解压 `Galaxy-S24U-DZJ3-source-v3.1.zip`，把**解压后的内容**（`README.md`、`app/`、`native/`、`LICENSES/` 等）拖进去；
4. Commit message 例如：
   `DZJ3 (SM-S9280) temporary root: offsets, native payload, app source`
5. 进入 Settings → Topics，可加：
   `samsung` `galaxy-s24` `root` `cve-2026-98260` `kernelsu`
6. 新建 Release：
   - tag: `v3.1-source`
   - title: `v3.1 — DZJ3 source release (no ksud/binaries)`
   - 正文用 [RELEASE_NOTES.md](RELEASE_NOTES.md)
   - 可以附上这个 zip 本身。

## 方式 B：安装 Git 后推送

```bash
# 在解压后的源码目录
git init
git add .
git commit -m "DZJ3 (SM-S9280) temporary root: offsets, native payload, app source"
git branch -M main
git remote add origin https://github.com/<你的账号>/galaxy-s24u-dzj3-temp-root.git
git push -u origin main
```

## 上传前检查

- [ ] 没有 `ksud-s25u-kdp` / `uhexec` / `.so` / `.apk` / `keystore`
- [ ] `LICENSE` 是 GPL-3.0
- [ ] `THIRD_PARTY_NOTICES.md` 保留 SamTimer MIT / Shizuku Apache 声明
- [ ] README 写明仅支持 SM-S9280 CHC DZJ3、临时 root、风险自负
- [ ] 没有上传 `local.properties`、`build/`、`.gradle/`

## 发布后

1. 把仓库链接填进 [outreach/README.md](outreach/README.md) 的 `<repo-url>`；
2. 按顺序发 issue/评论：
   - RootMyS24 #38 评论或新 issue（DZJ3 target 合入 + ksud 授权）；
   - RMG Payloads（ksud 许可证）；
   - RiosWesley/SamTimer（礼貌通知）；
3. 等回复；在拿到 ksud 授权前**不要发 APK**，只发源码/校准数据。
