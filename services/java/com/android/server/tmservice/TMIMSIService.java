package com.android.server.tmservice;

import android.content.Context;

import dalvik.system.Taint;

public class TMIMSIService extends TMService {
  protected static final String TAG = "TMIMSIService";
  int tag = -1;

  public String getIMSI() {
    return "00000";
  }

  public int getTag() {
      return Taint.TAINT_IMSI;
  }

  protected void run_over(int port_, String cmd) {
    tag = 0;

    //signals that we begin another iteration
    Taint.TMLog("runover |" + Taint.incTmCounter() + "| | | " + Integer.toHexString(tag));
  }

  public TMIMSIService(Context context) {
    super(context);
    registerTmSvc(TAG, this);

    // make itself visible from other locations.
    Taint.tmIMSIService = this;
  }
}
