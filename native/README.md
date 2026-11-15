# native payloads

- `timer-dzj3-apk.c`：标准模式（`--ephemeral --allow-shell`，KSU 缺失时 fallback）
- `timer-dzj3-lsposed.c`：LSPosed/模块模式（跑 module stage，不 ephemeral）
- `offsets.h`：DZJ3 定标常量

编译：

```bash
export ANDROID_NDK_HOME=/path/to/android-ndk-r29
./build.sh
```

把生成的 `libtimer_std.so` / `libtimer_lspd.so` 放入
`app/src/main/jniLibs/arm64-v8a/`。

源码基于 SamTimer（MIT）；见仓库根目录 `THIRD_PARTY_NOTICES.md`。
