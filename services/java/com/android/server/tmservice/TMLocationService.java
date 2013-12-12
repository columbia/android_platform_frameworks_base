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
import java.io.PrintWriter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.util.Slog;
import android.util.Log;

import com.android.tmservice.*;
import com.android.server.tmservice.TMLogcat;
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

  private List<Pair<Double, Double>> coordList =
    new ArrayList<Pair<Double, Double>>();
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

  private boolean IsGpsProviderAvailable() {
    if (gpsProvider != null ||
        (gpsProvider != locationManager.getGpsProvider())) {
      gpsProvider = locationManager.getGpsProvider();

    }
    return  gpsProvider != null ? true : false;
  }

  private void invokeReportGpsLocation(double longitude, double latitude, int tag) {
    if (IsGpsProviderAvailable()) {
      gpsProvider.tmReportGpsLocation(longitude, latitude, (double) 1.0, tag);
    } else {
      Log.v(TAG, "GpsProvider not available yet");
    }
  }


  /**
   *
   */
  private void sockClient(String addr, int port, String[] msgs) throws IOException{
    try {
      Socket client = new Socket(addr, port);
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

    Log.v(TAG, "run_over - location:" + latitude + " :" + 
                longitude + "::" + locationManager.getGpsProvider());

    int tag = Taint.TAINT_LOCATION | Taint.TAINT_LOCATION_GPS;
    invokeReportGpsLocation(latitude.doubleValue(), longitude.doubleValue(), tag);


    // //initiating run_over
    // String[] msgs = {"run_over"};
    // try {
    //   sockClient("sos15.cs.columbia.edu", 10000, msgs);
    // } catch (IOException e) {
    //   Log.e(TAG, "run_over: failed with socket connection error " + e.toString());
    //   return;
    // }
    

    Log.v(TAG, "tmLogcat: " + tmLogcat);
    
    try {
      List<String> lines = tmLogcat.getLineList();
      for (String line: lines) {
        Log.v(TAG, "DBG: " + line);
      }
    } catch (NullPointerException ne) {
      Log.v(TAG, "nullPointerExeption raised:");
      ne.printStackTrace();
    }
  }

  public static int randInt(int min, int max) {

    // Usually this can be a field rather than a method variable
    Random rand = new Random();

    // nextInt is normally exclusive of the top value,
    // so add 1 to make it inclusive
    int randomNum = rand.nextInt((max - min) + 1) + min;

    return randomNum;
  }

  public TMLocationService(Context context) {
    super();

    mContext = context;

    locationManager = (LocationManagerService) 
      ServiceManager.getService(Context.LOCATION_SERVICE);

    GpsLocationProvider gpsProvider = locationManager.getGpsProvider();
    Log.v(TAG, "LocationManager:" + locationManager +  " gpsProvider:" + gpsProvider);

    //init random coordinates
    for (int i = 0 ; i < ENTRY_MAX; i++) {
      coordList.add(new Pair<Double, Double>(new Double(randInt(0, 20)) , 
                                             new Double(randInt(0, 20)) ));
    }

    tmLogcat = new TMLogcat();
    mListener = new Thread(new TMListenerThread(Taint.tmport));
    mListener.start();
    
    Log.v(TAG, "mListener started: " + Taint.tmport + ":" + mListener);

    if (LOCAL_LOGV) {
      Slog.v(TAG, "Constructed LocationManager Service");
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
          // FIXME: And now it only supports a single connection at a time.

          incoming = serverSocket.accept();
          Log.v(TAG, "inside listener thread -- it's running");
          BufferedReader reader = new BufferedReader(
            new InputStreamReader(incoming.getInputStream()));

          PrintWriter writer = new PrintWriter(
            incoming.getOutputStream(), true);

          while (true) {
            String line = reader.readLine().trim();

            if (line.startsWith("disc")) {
              reader.close();
              writer.close();
              incoming.close();
              
              break;
            } else if(line.equals("run_over")) {
              run_over();

            } else {
              Taint.TMLog("unexpected input: " + line);
            }
            writer.println("RECV: " + line.trim());
          }
        }
      }  catch (IOException e) {
        //FIXME: need proper error handling
        Log.v(TAG, "socket error: " + e.toString());
      }
      //
      //TODO: implement thread join event here
      //
    }

    TMListenerThread(int port) {
      tmport = port;
    }
  }
}


