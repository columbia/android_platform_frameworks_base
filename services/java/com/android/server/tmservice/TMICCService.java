package com.android.server.tmservice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;
import android.content.Context;

import dalvik.system.Taint;

/**
 * Class for TMeasure ICC service.

 *
 * ICC is fixed 15-digit length.
 *
 * 8-digit Type Allocation Code (TAC)
 * 6-digit manufacturer-defined
 * 1-digit Luhn check
 *
 *
 */
public class TMICCService extends TMService {
  protected static final String TAG = "TMICCService";
  protected static final String iccPath = "/data/local/tmp/icc.list";
  int tag = -1;

  private String icc;

  private class ICCTag {
    private String icc = null;
    private int tag = 0;

    ICCTag(String icc_, int tag_) {
        icc = icc_;
        tag = tag_;
    }

    public String getICC() {
        return icc;
    }

    public int getTag() {
        return tag;
    }
 }

 private static boolean checkICC(String line) {
   /* FIXME: */
   return true;
 }
  private List<ICCTag> iccList = new ArrayList<ICCTag>();
  private Iterator<ICCTag> it;

  public String getICC() {
    Log.e("ICCService", "getICC called:");
    return icc;
  }

  public int getTag() {
      return tag;
  }

  /**
   * Method to prepare input value and tag value for next iteration.
   */
  public void next() {
      //End of the list. Reset it to the first entry.
      if (!it.hasNext()) {
          it = iccList.iterator();
      }

      ICCTag iccInst = it.next();
      icc = iccInst.getICC();

      //TODO: implement better fuzz logic here.
      tag = getNextTag(tag);
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
    if (!subcmd.equals("skip")) {
      next();
    } else {
      tag = getNextTag(tag);
    }

    Taint.TMLog("runover |" + Taint.incTmCounter() + "|"+ icc + " | "+ " | "
                + Integer.toHexString(tag) + "|TMICC");
  }

  /**
   * Constructor method
   * @param context
   *    global environment.
   */
  public TMICCService(Context context) {
    super(context);

    Log.v(TAG, "TMICCService started" );
    registerTmSvc(TAG, this);

    initialize();
  }

  private void initialize() {
    BufferedReader br = null;
    String line = null;

    try {
        br = new BufferedReader( new InputStreamReader( new FileInputStream(iccPath)));

        while ((line = br.readLine()) != null) {
            if (checkICC(line)) {
                iccList.add(new ICCTag(line, 0));
            }
        }

        it = iccList.iterator();

    } catch (IOException ie) {
        Log.v(TAG, "File Error");
    }
  }

  protected void refresh() {
    // TODO Auto-generated method stub
    Log.v(TAG, "refresh called");
    initialize();
  }
}
