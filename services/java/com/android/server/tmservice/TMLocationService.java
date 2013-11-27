package com.android.server.tmservice;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Thread;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.util.Slog;

import com.android.tm.service.ITMLocationService;
import dalvik.system.Taint;

public class TMLocationService extends ITMLocationService.Stub {
  private static final String TAG = "TaintService";
  private static final boolean LOCAL_LOGV = false;
  private final Context mContext;
  private final Thread mListener;

  //Methods for providing fake GPS input.
  //TODO: implement input generations here.
  public double getLatitude() {
    return 1.0;
  }

  public double getLongitude() {
    return 2.0;
  }

  private void socketOutputCaptured(String dstr, String tstr) {
  }

  private class TMListenerThread implements Runnable {
    private int tmport = 0;
    private ServerSocket serverSocket = null;
    private Socket incoming = null;

    public void run() {
      try {
        serverSocket = new ServerSocket(tmport);
        while (true) {
          incoming = serverSocket.accept();
          while (true) {
            BufferedReader reader = new BufferedReader(
              new InputStreamReader(incoming.getInputStream()));
            String line = reader.readLine();
            Taint.TMLog("line: " + line);
            if (line.startsWith("exit")) {
              break;
            } else if(line.startsWith("output:")) {
              String[] tokens = line.split(":");
              socketOutputCaptured(tokens[1], tokens[2]);
            } else {
              //not expecting to reach this point
              //assert false;
              ;
            }
          }
        }
      }  catch (IOException e) {
        //FIXME: need proper error handling
        System.err.println(e);
      }
      //
      //TODO: add thread join event here
      //
    }

    TMListenerThread(int port) {
      tmport = port;
    }
  }

  public TMLocationService(Context context) {
    super();

    mContext = context;
    mListener = new Thread(new TMListenerThread(Taint.tmport));
    mListener.start();

    if (LOCAL_LOGV) {
      Slog.v(TAG, "Constructed LocationManager Service");
    }
  }
}
