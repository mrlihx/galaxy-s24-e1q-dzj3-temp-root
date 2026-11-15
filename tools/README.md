# calibration tools

- `dzj3_profile_probe.py`：从 DZJ3 `vmlinux.elf` + `Image` 读取关键符号和
  `do_timer_create` / `prepare_creds` / `dup_task_struct` 的分配返回点；
- `diff_kernels.py`：比较两个内核 Image 的差异（用于确认不同 build 是否同布局）。

运行前修改脚本里的 `VMLINUX` / `KERNEL` 路径，或改成环境变量。
依赖：`pyelftools`、`capstone`。

这些脚本只做静态分析，不接触设备。
