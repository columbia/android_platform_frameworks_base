package com.android.tmservice;

import android.os.IBinder;
import android.os.RemoteException;

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
      return mContext.getIMSI();
    } catch (RemoteException re) {
      return null;
    }
  }
  
  public int getTag() {
      try {
        return mContext.getTag();
      } catch (RemoteException re) {
        return 0;
      }
    }  
}