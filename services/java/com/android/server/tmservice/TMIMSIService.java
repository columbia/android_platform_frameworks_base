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
 * @author Kangkook Jee(jikk@cs.columbia.edu)
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

  /**
   * Method to prepare input value and tag value for next iteration.
   */
  public void next() {
      //TODO: implement reasonable fuzzing logic here.
      tag += 1;
      msin += 1;
      imsi = String.format("%d%d%09d", mcc, mnc, msin);
  }


/**
 * Method to initiate another iteration.
 * 
 * @param port
 *      port number to connect GUI fuzzer outside
 * @param subcmd
 *      sub-command to direct specific sub-action.     
 */
  protected void run_over(int port_, String subcmd) {
    //signals that we begin another iteration
    next();
    Taint.TMLog("runover |" + Taint.incTmCounter() + "| | | " 
            + Integer.toHexString(tag));
  }

  /**
   * Constructor method
   * @param context 
   *    global environment.
   */
  public TMIMSIService(Context context) {
    super(context);

    Log.v(TAG, "TMIMSIService started" );
    registerTmSvc(TAG, this);

    //init. private(service specific) variables.
    imsi = String.format("%d%d%09d", mcc, mnc, msin);
  }
}
