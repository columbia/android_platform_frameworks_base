package com.android.server.tmservice;

import android.content.Context;

import dalvik.system.Taint;


public class TMIMSIService extends TMService {
  protected static final String TAG = "TMIMSIService";

  public String getIMSI() {
    return "00000";
  }

  protected void run_over(int port_, String cmd) {
    int tag = 0;

    //signals that we begin another iteration
    Taint.TMLog("runover |" + Taint.incTmCounter() + "| | | " + Integer.toHexString(tag));
  }

  public TMIMSIService(Context context) {
    super(context);
    registerTmSvc(TAG, this);
  }
}
