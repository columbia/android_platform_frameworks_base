package com.android.tmservice;

import android.os.RemoteException;

public class TMIMSIManager {
  ITMIMSIService mContext = null;
  public TMIMSIManager(ITMIMSIService iTmImsiSvc) {
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