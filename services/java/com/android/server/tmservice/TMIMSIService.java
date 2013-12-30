package com.android.server.tmservice;

import android.util.Log;
import android.content.Context;

import dalvik.system.Taint;

/**
 * Class for TMeasure IMSI service 
 * @author jikk
 *
 */
public class TMIMSIService extends TMService {
  protected static final String TAG = "TMIMSIService";
  int tag = -1;

  public String getIMSI() {
    Log.e("JIKK-IMSIService", "getIMSI called:0");
    return "310260000000002";
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

    Log.v(TAG, "TMIMSIService started" );
    registerTmSvc(TAG, this);
  }
}
