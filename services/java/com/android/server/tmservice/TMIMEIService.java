package com.android.server.tmservice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;
import android.content.Context;

import dalvik.system.Taint;

/**
 * Class for TMeasure IMEI service.

 *
 * IMEI is fixed 15-digit length.
 *
 * 8-digit Type Allocation Code (TAC)
 * 6-digit manufacturer-defined
 * 1-digit Luhn check
 *
 *
 */
public class TMIMEIService extends TMService {
  protected static final String TAG = "TMIMEIService";
  int tag = -1;

  //default IMEI
  private int tac = 0;
  private int md = 0;
  private int lc = 0;
  private String imei;

  private List<Tuple<Integer, Integer, Integer>> imeiList =
          new ArrayList<Tuple<Integer, Integer, Integer>>();


  Iterator<Tuple<Integer, Integer, Integer>> it;

  public String getIMEI() {
    Log.e("IMEIService", "getIMEI called:0");
    return imei;
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
          it = imeiList.iterator();
      }

      Tuple<Integer, Integer, Integer> imeiInst = it.next();
      tac = imeiInst.x;
      md = imeiInst.y;
      //FIXME: change field name: tag --> z
      lc = imeiInst.tag;

      imei = String.format("%d%06d%d", tac, md, lc);
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

    Taint.TMLog("runover |" + Taint.incTmCounter() + "|"+ imei + " | "+ " | "
                + Integer.toHexString(tag));
  }

  /**
   * Constructor method
   * @param context
   *    global environment.
   */
  public TMIMEIService(Context context) {
    super(context);

    Log.v(TAG, "TMIMEIService started" );
    registerTmSvc(TAG, this);

    //init. private(service specific) variables.
    imei = String.format("%d%05d%d", tac, md, lc);


    /*
     * http://en.wikipedia.org/wiki/Type_Allocation_Code
     */

    //HTC
    imeiList.add(new Tuple<Integer, Integer, Integer>(35896704, 1, 1));
    imeiList.add(new Tuple<Integer, Integer, Integer>(35902803, 1, 0));
    // ...

    //Nokia
    imeiList.add(new Tuple<Integer, Integer, Integer>(35274901, 1, 5));
    imeiList.add(new Tuple<Integer, Integer, Integer>(35566600, 1, 0));
    // ...


    //Samsung
    imeiList.add(new Tuple<Integer, Integer, Integer>(35853704, 1, 2));
    imeiList.add(new Tuple<Integer, Integer, Integer>(35226005, 1, 3));
    // ...

    it = imeiList.iterator();
  }
}
