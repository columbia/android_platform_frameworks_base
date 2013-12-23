package com.android.tmservice;

interface ITMService {

  /* FIXME: I'm still not clear about why we are exporting methods from here.
  For me, it doesn't seem to make any difference since what we end up doing are

    tmXXSvc = context.getSystemSevice("TMXXService");
    tmXXSvc.getYYY();

    In this occasion, do we need to export getYYY() from here?
   */

  //return tag information
  int getTag();

  //Remote service methods for Device ID information.
  String getIMSI();
}
