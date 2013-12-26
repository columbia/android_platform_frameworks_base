package com.android.tmservice;

import android.os.RemoteException;

public class TMIMSIManager {
  ITMService mContext = null;
  public TMIMSIManager(ITMService iTmImsiSvc) {
    mContext = iTmImsiSvc;
  }
  public String getIMSI() {
    try {
      return mContext.getIMSI();
    } catch (RemoteException re) {
      return null;
    }
  }
}