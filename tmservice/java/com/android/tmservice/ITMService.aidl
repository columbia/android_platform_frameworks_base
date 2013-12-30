package com.android.tmservice;

interface ITMService {
  //return tag information.
  int getTag();

  //move on to next iteration.
  void next();

  //Remote service methods for Device ID information.
  String getIMSI();
}
