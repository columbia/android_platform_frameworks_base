package com.android.server.tmservice;

import android.content.Context;

import dalvik.system.Taint;


public class TMDevIdService extends TMService {
  protected static final String TAG = "TMDevIdService";

  public int getDevId() {
    return 0;
  }

  protected void run_over(int port_, String cmd) {
    int tag = 0;

    //signals that we begin another iteration
    Taint.TMLog("runover |" + Taint.incTmCounter() + "| | | " + Integer.toHexString(tag));
  }

  public TMDevIdService(Context context) {
    super(context);
  }
}
