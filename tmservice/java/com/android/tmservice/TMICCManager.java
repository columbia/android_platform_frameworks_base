package com.android.tmservice;

import android.os.RemoteException;
import android.util.Log;

/**
 * Class that implements RPC client
 *
 * @author jikk
 *
 */
public class TMICCManager {
  private ITMService mContext = null;
  private static String TAG = "TMICCManager";

  public TMICCManager(ITMService b) {
    Log.e("ICCMgr", "Constructor:" + b);
    mContext = b;
  }
  public String getICC() {
    try {
      String ret = mContext.getICC();
      return ret;
    } catch (RemoteException re) {
      Log.e(TAG, "getICC(): Remote Exception" + re.toString());
      return null;
    } catch (Exception e) {
      Log.e(TAG, "getICC(): Exception" + e.toString());
      return null;
    }
  }

  public void next() {
    try {
      mContext.next();
    } catch (RemoteException re) {
      Log.e(TAG, "next() Remote Exception" + re.toString());
    } catch (Exception e) {
      Log.e(TAG, "next() Exception" + e.toString());
    }
  }

  public int getTag() {
      try {
        int ret = mContext.getTag();
        return ret;
      } catch (RemoteException re) {
        Log.e(TAG, "getTag() Remote Exception" + re.toString());
        return 0;
      } catch (Exception e) {
        Log.e(TAG, "getTag() Exception" + e.toString());
        return 0;
      }
  }
}
