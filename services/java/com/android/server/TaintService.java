package com.android.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.util.Slog;

import com.android.tm.service.ITaintService;


public class TaintService extends ITaintService.Stub {
    private static final String TAG = "TaintService";
    private static final boolean LOCAL_LOGV = false;
    private final Context mContext;

    //Methods for providing fake GPS input.
    //TODO: implement input generations here.
    public double getLatitude() {
        return 1.0;
    }

     public double getLongitude() {
        return 2.0;
    }

    public TaintService(Context context) {
        super();
        mContext = context;

        if (LOCAL_LOGV) {
            Slog.v(TAG, "Constructed LocationManager Service");
        }
    }
}
