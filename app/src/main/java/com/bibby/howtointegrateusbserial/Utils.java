package com.bibby.howtointegrateusbserial;

import android.util.Log;

public class Utils {

    private static final String TAG = "Utils";

    public static final void logThread() {
        Thread t = Thread.currentThread();
        Log.d(TAG,
                "<" + t.getName() + ">id: " + t.getId() + ", Priority: "
                        + t.getPriority() + ", Group: "
                        + t.getThreadGroup().getName());
    }
}
