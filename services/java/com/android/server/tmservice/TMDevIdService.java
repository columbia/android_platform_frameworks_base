package com.android.server.tmservice;

import java.lang.Thread;

import android.content.Context;
import android.util.Log;

import dalvik.system.Taint;


public class TMDevIdService extends TMService {
  private static final String TAG = "TMDevIdService";

  public int getDevId() {
    return 0;
  }

  protected void run_over(int port_, String cmd) {
    int tag = 0;

    //signals that we begin another iteration
    Taint.TMLog("runover |" + Taint.incTmCounter() + "| | | " + Integer.toHexString(tag));
  }

  public TMDevIdService(Context context) {
    super();

    mContext = context;

    //Singleton
    if (tmLogcat == null) { 
        tmLogcat = new TMLogcat();
    }
    
    if (mListener == null) {
        mListener = new Thread(new TMListenerThread(Taint.tmport));
        mListener.start();
    
        Log.v(TAG, "mListener started: " + Taint.tmport + ":" + mListener);
    }
  }
}
