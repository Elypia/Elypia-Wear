package com.elypia.watchface;

import android.os.*;

import java.lang.ref.WeakReference;

public class EngineHandler extends Handler {

    private final WeakReference<ElypiaWatchface.Engine> REFERENCE;
    private final int MSG_UPDATE_TIME;

    public EngineHandler(ElypiaWatchface.Engine reference, int updateTime) {
        REFERENCE = new WeakReference<>(reference);
        MSG_UPDATE_TIME = updateTime;
    }

    @Override
    public void handleMessage(Message msg) {
        ElypiaWatchface.Engine engine = REFERENCE.get();

        if (engine != null && msg.what == MSG_UPDATE_TIME)
            engine.handleUpdateTimeMessage();
    }
}
