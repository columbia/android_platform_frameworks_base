package com.android.server.tmservice;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Thread;
import java.util.*;

import java.net.InetAddress;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.OutputStream;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.util.Slog;
import android.util.Log;

import com.android.tm.service.*;
import com.android.server.tmservice.*;
import dalvik.system.Taint;

import com.android.server.LocationManagerService;
import com.android.server.location.GpsLocationProvider;

import android.os.ServiceManager;


/**
 * Custom tuple definition
 */
class Pair<X, Y> { 
  public final X x; 
  public final Y y; 
  public Pair(X x, Y y) { 
    this.x = x; 
    this.y = y; 
  } 
} 

public class TMLocationService extends ITMLocationService.Stub {

  private LocationManagerService locationManager = null;
  private GpsLocationProvider gpsProvider = null;

  private static final String TAG = "TaintService";
  private static final boolean LOCAL_LOGV = false;
  private static final int ENTRY_MAX = 10;

  private final Context mContext;
  private final Thread mListener;

  private List<Pair<Double, Double>> coordList;
  private int coordPtr = 0;

  private TMLogcat tmLogcat;

  //Methods for providing fake GPS input.
  //TODO: implement input generations here.
  public double getLatitude() {
    return 1.0;
  }

  public double getLongitude() {
    return 2.0;
  }

  /**
   *
   */
  private void sockClient(int port, String[] msgs) throws IOException{
    try {
      Socket client = new Socket(InetAddress.getLoopbackAddress(), port);
      DataOutputStream outToTMSvc = new DataOutputStream(client.getOutputStream());
      for (String msg : msgs) {
        outToTMSvc.writeUTF(msg);
      }
    } catch(IOException e) {
      Log.e(TAG, "sockClient:IOException:" + e.toString());
      throw e;
    }
  }

  /**
   *
   */
  private void run_over() {
    //fake value pair for GPS location
    Double latitude = coordList.get(coordPtr).x;
    Double longitude = coordList.get(coordPtr).y;;

    //iterate over prepared <lati, long> pairs
    coordPtr = (coordPtr + 1) % ENTRY_MAX;

    Taint.TMLog("location:" + latitude + " :" + longitude);

    //emulating native device
    gpsProvider.reportLocationP(1, 
                                latitude.doubleValue(), 
                                longitude.doubleValue(), 
                                (double) 0.0, 
                                (float) 0.0, (float) 0.0, (float) 0.0, 
                                new Date().getTime());

    //initiating run_over
    String[] msgs = {"run_over"};
    try {
      sockClient(10000, msgs);
    } catch (IOException e) {
      Log.e(TAG, "run_over: failed with socket connection error " + e.toString());
      return;
    }
    
    List<String> lines = tmLogcat.getLineList();
    for (String line: lines) {
      Log.v(TAG, "DBG: " + line);
    }
  }


  private class TMListenerThread implements Runnable {
    private int tmport = 0;
    private ServerSocket serverSocket = null;
    private Socket incoming = null;

    public void run() {
      try {
        serverSocket = new ServerSocket(tmport);
        while (true) {

          //FIXME: this part shouldn't block the original operations.
          //try to re-implement this later using some non-block 
          //primitives such as AsyncTask. 

          incoming = serverSocket.accept();
          while (true) {
            BufferedReader reader = new BufferedReader(
              new InputStreamReader(incoming.getInputStream()));
            String line = reader.readLine().trim();

            if (line.startsWith("exit")) {
              break;
            } else if(line.trim().equals("run_over")) {
              run_over();
            } else {
              //not expecting to reach this point
              //assert false;
              Taint.TMLog("unexpected input: " + line);
            }
          }
        }
      }  catch (IOException e) {
        //FIXME: need proper error handling
        System.err.println(e);
      }
      //
      //TODO: implement thread join event here
      //
    }

    TMListenerThread(int port) {
      tmport = port;
    }
  }

  public TMLocationService(Context context) {
    super();

    mContext = context;

    locationManager = (LocationManagerService) 
      ServiceManager.getService(Context.LOCATION_SERVICE);

    GpsLocationProvider gpsProvider = locationManager.getGpsProvider();

    //init random coordinates
    for (int i = 0 ; i < ENTRY_MAX; i++) {
      coordList.add(new Pair<Double, Double>(Math.random() * 36000 - 18000, 
                                             Math.random() * 36000 - 18000));
    }

    tmLogcat = new TMLogcat();
    mListener = new Thread(new TMListenerThread(Taint.tmport));
    mListener.start();

    if (LOCAL_LOGV) {
      Slog.v(TAG, "Constructed LocationManager Service");
    }
  }
}

