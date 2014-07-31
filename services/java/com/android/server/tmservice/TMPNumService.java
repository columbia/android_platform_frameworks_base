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
 * Class for TMeasure phone number service.
 */
public class TMPNumService extends TMService {
  protected static final String TAG = "TMPNumberService";
  protected static final String pNumPath = "/data/local/tmp/pNum.list";
  int tag = -1;

  //default phone number
  private String pNum = null;

  private class PNumTag {
    private String pNum = null;
    private int tag = 0;

    PNumTag(String pNum_, int tag_) {
        pNum = pNum_;
        tag = tag_;
    }

    public String getPNum() {
        return pNum;
    }

    public int getTag() {
        return tag;
    }
 }

  private static boolean checkPNum(String line) {
    /* FIXME: */
    return true;
  }
 
  private List<PNumTag> pNumList = new ArrayList<PNumTag>();
  private Iterator<PNumTag> it;

  public String getPNumber() {
    Log.e("PhoneNumberService", "getPNumber called:0");
    return pNum;
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
          it = pNumList.iterator();
      }

      PNumTag pNumInst = it.next();
      pNum = pNumInst.getPNum();

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

    Taint.TMLog("runover |" + Taint.incTmCounter() + "|"+ pNum + " | "+ " | "
                + Integer.toHexString(tag) + "|TMPNumService");
  }

  /**
   * Constructor method
   * @param context
   *    global environment.
   */
  public TMPNumService(Context context) {
    super(context);

    Log.v(TAG, "TMPNumberService started" );
    registerTmSvc(TAG, this);

    //init. private(service specific) variables.
    initialize();
  }

  private void initialize() {
    BufferedReader br = null; 
    String line = null;
    try {
        br = new BufferedReader( new InputStreamReader( new FileInputStream(pNumPath)));

        while ((line = br.readLine()) != null) {
            if (checkPNum(line)) {
                pNumList.add(new PNumTag(line.trim(), 0));
            }
        }
        it = pNumList.iterator();

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
