package com.android.server.tmservice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;
import android.content.Context;

import dalvik.system.Taint;

/**
 * Class for TMeasure phone number service.
 */
public class TMPNumerService extends TMService {
  protected static final String TAG = "TMPNumberService";
  int tag = -1;

  //default phone number
  private String pnumber;

  private List<Tuple<Integer, Integer, Integer>> pnumList =
          new ArrayList<Tuple<Integer, Integer, Integer>>();


  Iterator<Tuple<Integer, Integer, Integer>> it;

  public String getPNumber() {
    Log.e("PhoneNumberService", "getPNumber called:0");
    return pnumer;
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
          it = pnumList.iterator();
      }

      Tuple<Integer, Integer, Integer> pnumInst = it.next();
      tac = pnumInst.x;
      md = pnumInst.y;
      //FIXME: change field name: tag --> z
      lc = pnumInst.tag;

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
    initialize();
  }

  private void initialize() {
    /*
     * http://en.wikipedia.org/wiki/Type_Allocation_Code
     */

    //HTC
    pnumList.add(new Tuple<Integer, Integer, Integer>(35896704, 1, 1));
    pnumList.add(new Tuple<Integer, Integer, Integer>(35902803, 1, 0));
    // ...

    //Nokia
    pnumList.add(new Tuple<Integer, Integer, Integer>(35274901, 1, 5));
    pnumList.add(new Tuple<Integer, Integer, Integer>(35566600, 1, 0));
    // ...


    //Samsung
    pnumList.add(new Tuple<Integer, Integer, Integer>(35853704, 1, 2));
    pnumList.add(new Tuple<Integer, Integer, Integer>(35226005, 1, 3));
    // ...

    it = pnumList.iterator();
  }

  protected void refresh() {
    // TODO Auto-generated method stub
    Log.v("TM-MSG", "refresh called");
    initialize();
  }
}
