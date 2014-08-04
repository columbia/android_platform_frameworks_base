package com.android.server.tmservice;

import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.Thread;
import java.util.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;

import android.content.Context;
import android.util.Log;
import com.android.server.LocationManagerService;
import com.android.server.location.GpsLocationProvider;

import dalvik.system.Taint;

import android.os.RemoteException;
import android.os.ServiceManager;

/**
 * TM Class for Location service.
 * @author Kangkook Jee
 *
 */
public class TMLocationService extends TMService{
  private Thread mThread = null;
  private LocationManagerService locationManager = null;
  private GpsLocationProvider gpsProvider = null;
  private List<CoordTag> coordList = new ArrayList<CoordTag>();
  private Iterator<CoordTag> it;
  private int coordIdx = 0;

  private Double longitude_ = 0.0;
  private Double latitude_ = 0.0;

  protected static String TAG = "TMLocationService";
  protected static int SLEEP_TIME = 5000;
  protected static String gpsPath = "/data/local/tmp/gps.list";

  private class CoordTag {
    private Double long_;
    private Double lat_;
    private int tag = 0;
    private boolean validFlag = false;

    public CoordTag(String line) {
      StringTokenizer st = new StringTokenizer(line);

      if (st.countTokens() == 2) {
        try {
          long_ = Double.parseDouble(st.nextToken());
          lat_ =  Double.parseDouble(st.nextToken());
          validFlag = true;
        } catch (NullPointerException ne) {
          long_ = 0.0;
          lat_ = 0.0;
          validFlag = false;
        } catch (NumberFormatException ne) {
          long_ = 0.0;
          lat_ = 0.0;
          validFlag = false;
        }
      }  else {
        long_ = 0.0;
        lat_ = 0.0;
        validFlag = false;
      }
    }

    public boolean isValid() {
      return validFlag;
    }

    public Double getLatitude() {
      return lat_;
    }

    public Double getLongitude() {
      return long_;
    }
    public int getTag() {
      return tag;
    }
  }

  private class GpsReportThread extends Thread {
    private boolean updateFlag = true;
    public void run() {
      Log.v(TAG, "GPS thread started");
      while (updateFlag) {
        try {
          Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException ie) {
          break;
        }

        if (longitude_ != 0 && latitude_ != 0) {
          //Log.v(TAG, "invokeReportGpsLocation called from Thread");
          invokeReportGpsLocation(longitude_, latitude_, tag);
        }
      }
    }
    protected void finalize() throws Throwable {
      updateFlag = false;
    }
  }

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

  public int getTag() {
      return tag;
  }

  public void next() {
      //iterate over prepared <lati, long> pairs
      coordIdx = (coordIdx + 1) % coordList.size();
      tag = getNextTag(tag);
      Log.v(TAG, "next() called tag set to " + tag);
  }

  protected void run_over(int port_, String subcmd) {
    Log.v(TAG, "run_over invoked with " + port_ + " and subcmd: " + subcmd + "coordIdx: " + coordIdx + "tag:" + tag);

    //fake value pair for GPS location
    if (coordList.size() == 0) {
        initialize();
    }

    if (coordList.size() != 0) {
        try {
            latitude_ = coordList.get(coordIdx).getLatitude();
            longitude_ = coordList.get(coordIdx).getLongitude();
        } catch (IndexOutOfBoundsException ie) {
            Log.v(TAG, "IndexOutOfBoundsException for size " + coordList.size() + " index: " + coordIdx);
        }

        //Signals that we begin another iteration
        Taint.TMLog("runover |" + Taint.incTmCounter() + "|gps|" + latitude_ + ", "
                    + longitude_ + "| " + Integer.toHexString(getTag()));

    	if (!subcmd.equals("skip")) {
            next();
        }
    } else {
        Log.v(TAG, "No coordinates provided. Make sure to have /data/local/tmp/gps.list in place.");
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

    //init. private(service specific) variables.
    locationManager = (LocationManagerService)
      ServiceManager.getService(Context.LOCATION_SERVICE);

    GpsLocationProvider gpsProvider = locationManager.getGpsProvider();
    Log.v(TAG, "LocationManager:" + locationManager +  " gpsProvider:" + gpsProvider);

    mThread = new GpsReportThread();
    mThread.start();
    Log.v(TAG, "GPS Thread assigned");
    
    initialize();
  }


  private void initialize() {
    BufferedReader br = null;
    String line = null;
    CoordTag coord = null;
    coordIdx = 0;

    try {
      br = new BufferedReader( new InputStreamReader( new FileInputStream(gpsPath)));
      while ((line = br.readLine()) != null) {
        coord = new CoordTag(line); 
        if (coord.isValid()) 
          Log.v(TAG, "Coord added: lat: " + coord.getLatitude() + ", long: " + coord.getLongitude());
          coordList.add(coord);
      }

      it = coordList.iterator();
    } catch (IOException ie) {
        /* Seoul - 1 */

        /* 
        coordList.add(new Tuple<Double, Double, Integer>(
                    37.33,
                    126.58,
                    new Integer(randInt(0, 32))));
        // New York - 2
        coordList.add(new Tuple<Double, Double, Integer>(
                    40.42,
                    74.0,
                    new Integer(randInt(0, 32))));
        // Washington - 3
        coordList.add(new Tuple<Double, Double, Integer>(
                    38.54,
                    77.2,
                    new Integer(randInt(0, 32))));
        // Athens - 4
        coordList.add(new Tuple<Double, Double, Integer>(
                    37.59,
                    23.43,
                    new Integer(randInt(0, 32))));
        // Madrid - 5
        coordList.add(new Tuple<Double, Double, Integer>(
                    40.25,
                    3.42,
                    new Integer(randInt(0, 32))));
        // London - 6
        coordList.add(new Tuple<Double, Double, Integer>(
                    51.30,
                    0.7,
                    new Integer(randInt(0, 32))));
        // Paris - 7
        coordList.add(new Tuple<Double, Double, Integer>(
                    48.51,
                    2.21,
                    new Integer(randInt(0, 32))));
        // Beijing - 8
        coordList.add(new Tuple<Double, Double, Integer>(
                    39.54,
                    116.24,
                    new Integer(randInt(0, 32))));
        // Rome - 9
        coordList.add(new Tuple<Double, Double, Integer>(
                    41.53,
                    12.28,
                    new Integer(randInt(0, 32))));
        // Tokyo - 10
        coordList.add(new Tuple<Double, Double, Integer>(
                    35.41,
                    139.41,
                    new Integer(randInt(0, 32))));
        */
    }
  }

  protected void refresh() {
    Log.v(TAG, "refresh called");
    initialize();
  }
}
