package com.android.server.tmservice;

import java.io.IOException;
import java.util.*;

import android.content.Context;
import android.util.Log;
import com.android.server.LocationManagerService;
import com.android.server.location.GpsLocationProvider;

import dalvik.system.Taint;

import android.os.ServiceManager;

/**
 * TM Class for Location service. 
 * @author Kangkook Jee
 *
 */
public class TMLocationService extends TMService{

  private LocationManagerService locationManager = null;
  private GpsLocationProvider gpsProvider = null;
  private List<Tuple<Double, Double, Integer>> coordList =
    new ArrayList<Tuple<Double, Double, Integer>>();
  private int coordPtr = 0;

  protected static String TAG = "TMLocationService";
  
  /**
   * 
   * @return
   */
  private boolean IsGpsProviderAvailable() {
    if (gpsProvider != null ||
        (gpsProvider != locationManager.getGpsProvider())) {
      gpsProvider = locationManager.getGpsProvider();

    }
    return  gpsProvider != null ? true : false;
  }

  private void invokeReportGpsLocation(double longitude, double latitude, int tag) {
    if (IsGpsProviderAvailable()) {
      //we are not changing the altitude 
      gpsProvider.tmReportGpsLocation(longitude, latitude, (double) 1.0, tag);
    } else {
      Log.v(TAG, "GpsProvider not available yet");
    }
  }
 
  protected void run_over(int port_, String cmd) {
    Log.v(TAG, "run_over invoked with " + port_ + " and " + cmd);
    
    //fake value pair for GPS location
    Double latitude = coordList.get(coordPtr).x;
    Double longitude = coordList.get(coordPtr).y;;

    //iterate over prepared <lati, long> pairs
    coordPtr = (coordPtr + 1) % ENTRY_MAX;

    int tag = Taint.TAINT_LOCATION | Taint.TAINT_LOCATION_GPS;

    //Default port to connect for monkey control
    int port = 10000;
    if (port_ != 0) {
      port = port_;
    }

    Log.v(TAG, "run_over - location:" + latitude + " :" + 
                longitude + "::" + locationManager.getGpsProvider());

    //Signals that we begin another iteration
    Taint.TMLog("runover |" + Taint.incTmCounter() + "|" + latitude + "| " 
                + longitude + "| " + Integer.toHexString(tag));

    //update made to GpsLocation service
    invokeReportGpsLocation(latitude.doubleValue(), longitude.doubleValue(), tag);

    //initiating run_over
    String[] msgs = {"run_over"};
    try {
      sockClient("10.0.2.2", port, msgs);
    } catch (IOException e) {
      Log.e(TAG, "run_over: failed with socket connection error: " + e.toString());
      return;
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
    super(context);
    registerTmSvc(TAG, this);

    locationManager = (LocationManagerService) 
      ServiceManager.getService(Context.LOCATION_SERVICE);

    GpsLocationProvider gpsProvider = locationManager.getGpsProvider();
    Log.v(TAG, "LocationManager:" + locationManager +  " gpsProvider:" + gpsProvider);

    // Init. random coordinates
    for (int i = 0 ; i < ENTRY_MAX; i++) {
      coordList.add(new Tuple<Double, Double, Integer>(
                      new Double(randInt(0, 20)), 
                      new Double(randInt(0, 20)), 
                      new Integer(randInt(0, 32))));
    }


    
  }
}
