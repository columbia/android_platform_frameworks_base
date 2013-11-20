package edu.columbia.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import edu.columbia.service.ITaintService;


public class TaintService extends Service {

    private final ITaintService.Stub binder = new ITaintService.Stub() {
	//Methods for providing fake GPS input.
	//TODO: implement input generations here.
        public double getLatitude() {
            return 1.0;
        }

        public double getLongitude() {
            return 2.0;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }
}
