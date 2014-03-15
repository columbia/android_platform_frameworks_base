package com.android.server.tmservice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

  //default IMSI
  private int mcc = 310;
  private int mnc = 260;
  private int msin = 1;
  private String imsi;

  private List<Tuple<Integer, Integer, Integer>> imsiList =
          new ArrayList<Tuple<Integer, Integer, Integer>>();


  Iterator<Tuple<Integer, Integer, Integer>> it;

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
      //TODO: implement better fuzz logic here.
      tag = getNextTag(tag);

      //End of the list. Reset it to the first entry.
      if (!it.hasNext()) {
          it = imsiList.iterator();
      }

      Tuple<Integer, Integer, Integer> imsiInst = it.next();
      mcc = imsiInst.x;
      mnc = imsiInst.y;
      //FIXME: change field name: tag --> z
      msin = imsiInst.tag;

      imsi = String.format("%03d%03d%09d", mcc, mnc, msin);
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
    Log.e("DBG-jikk:", "subcmd:" + subcmd);
    if (!subcmd.equals("skip")) {
      next();
    } else {
      tag = getNextTag(tag);
    }

    Taint.TMLog("runover |" + Taint.incTmCounter() + "|imsi |"+ imsi + " | "+ 
		    " | " + Integer.toHexString(tag));
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
    imsi = String.format("%03d%03d%09d", mcc, mnc, msin);


    /*
     * http://en.wikipedia.org/wiki/Mobile_country_code
     */

    //South Korean providers
    imsiList.add(new Tuple<Integer, Integer, Integer>(450, 02, 1));
    imsiList.add(new Tuple<Integer, Integer, Integer>(450, 03, 1));
    // ...

    //GB providers
    imsiList.add(new Tuple<Integer, Integer, Integer>(234, 00, 1));  //BT
    imsiList.add(new Tuple<Integer, Integer, Integer>(234, 01, 1));  //Vectone Mobile
    // ...


    //US providers
    imsiList.add(new Tuple<Integer, Integer, Integer>(310, 053, 1));  //Virgin Mobile
    imsiList.add(new Tuple<Integer, Integer, Integer>(310, 054, 1));  //Alltel US
    // ...

    it = imsiList.iterator();
  }
}
