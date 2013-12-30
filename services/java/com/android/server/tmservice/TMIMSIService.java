package com.android.server.tmservice;

import android.util.Log;
import android.content.Context;

import dalvik.system.Taint;

/**
 * Class for TMeasure IMSI service.
 *
 * IMSI is fixed 15-digit length.
 *
 * 3-digit Mobile Country Code(MCC) +
 * 3-digit Mobile Network Code(MNC) +
 * 9-digit Mobile Station Identification Number(MSIN).
 *
 * @author jikk
 *
 */
public class TMIMSIService extends TMService {
  protected static final String TAG = "TMIMSIService";
  int tag = -1;

  private int mcc = 310;
  private int mnc = 260;
  private int msin = 1;
  private String imsi;

  public String getIMSI() {
    Log.e("JIKK-IMSIService", "getIMSI called:0");
    return imsi;
  }

  public int getTag() {
      return tag;
  }

  public void next() {
      //TODO: implement some fuzzing logic here.
      tag += 1;
      msin += 1;
      imsi = String.format("%d%d%09d", mcc, mnc, msin);
  }

  protected void run_over(int port_, String subcmd) {
    //signals that we begin another iteration
    next();
    Taint.TMLog("runover |" + Taint.incTmCounter() + "| | | " + Integer.toHexString(tag));
  }

  public TMIMSIService(Context context) {
    super(context);

    Log.v(TAG, "TMIMSIService started" );
    registerTmSvc(TAG, this);

    //init. private(service specific) variables.
    imsi = String.format("%d%d%09d", mcc, mnc, msin);
  }
}
