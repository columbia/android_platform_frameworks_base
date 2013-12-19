package com.android.tmservice;

interface ITMService {
  //Remote service methods for GPS Location information.
  double getLatitude();
  double getLongitude();

  //Remote service methods for Device ID information.
  int getDevId();
}
