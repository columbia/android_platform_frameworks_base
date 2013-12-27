package com.android.tmservice;

import android.os.RemoteException;
import android.util.Log;

/**
 * Class that implements RPC client
 * 
 * @author jikk
 *
 */
public class TMIMSIManager {
  private ITMService mContext = null;
  private static String TAG = "TMIMSIManager";

  public TMIMSIManager(ITMService b) {
    Log.e("JIKK_IMSIMgr", "Constructor:" + b);
    mContext = b;
  }
  public String getIMSI() {
    try {
      String ret = mContext.getIMSI();
      return ret;
    } catch (RemoteException re) {
      Log.e(TAG, "getIMSI(): Remote Exception" + re.toString());
      return null;
    } catch (Exception e) {
      Log.e(TAG, "getIMSI() Exception" + e.toString());
      return null;
    }
  }
  
  public int getTag() {
      try {
        int ret = mContext.getTag();
        return ret;
      } catch (RemoteException re) {
        Log.e(TAG, "getTag Remote Exception" + re.toString());
        return 0;
      } catch (Exception e) {
        Log.e(TAG, "getTag Exception" + e.toString());
        return 0;
      }
  }
}