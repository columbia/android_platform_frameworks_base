package com.android.tmservice;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Class that implements RPC client
 * 
 * @author jikk
 *
 */
public class TMIMSIManager {
  ITMService mContext = null;

  public TMIMSIManager(IBinder b) {
    mContext = (ITMService) b;
  }
  public String getIMSI() {
    try {
      Log.e("JIKK-IMSIMgr", "getIMSI called:0");
      String ret = mContext.getIMSI();
      Log.e("JIKK-IMSIMgr", "getIMSI called:1:" + ret);
      return ret;
    } catch (RemoteException re) {
      return null;
    }
  }
  
  public int getTag() {
      try {
        Log.e("JIKK-IMSIMgr", "getTag called:0");
        int ret = mContext.getTag();
        Log.e("JIKK-IMSIMgr", "getTag called:1" + ret);
        return ret;
      } catch (RemoteException re) {
        return 0;
      }
    }  
}