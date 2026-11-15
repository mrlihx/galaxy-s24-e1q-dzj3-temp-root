package com.roottimer.app;

import com.roottimer.app.IRootCallback;

interface IRootService {
    void setMode(boolean lspd);
    void setSoftReboot(boolean enabled);
    void softRebootNow();
    void fixKernelFeatures(IRootCallback callback);
    void runExploit(IRootCallback callback);
    boolean isReady();
    boolean isRooted();
    void destroy();
}
