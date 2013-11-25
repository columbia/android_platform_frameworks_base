package com.android.server.tmservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.util.Slog;

import com.android.tm.service.ITMLocationService;

public class TMLocationService extends ITMLocationService.Stub {
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

  public TMLocationService(Context context) {
    super();
    mContext = context;

    if (LOCAL_LOGV) {
      Slog.v(TAG, "Constructed LocationManager Service");
    }
  }
}
