package com.android.server.tmservice;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Thread;
import java.util.*;

import java.net.InetSocketAddress;
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
import dalvik.system.Taint;

import android.os.ServiceManager;


public class TMDevIdService extends TMService {

  private static final String TAG = "TMDevIdService";
  private static final boolean LOCAL_LOGV = false;
  private static final int ENTRY_MAX = 10;

  private final Context mContext;
  private final Thread mListener;

  public int getIMSI() {
    return 0;
  }

  /**
   *
   */
  private void sockClient(String addr, int port, String[] msgs) throws IOException{
    Log.v(TAG, "sockClient is called with " + addr + " and " + port);
    int timeout = 30;

    try {
      Socket client = new Socket();
      client.connect(new InetSocketAddress(addr, port), 30);
      DataOutputStream writer = new DataOutputStream(client.getOutputStream());
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(client.getInputStream()));

      for (String msg : msgs) {
        writer.writeBytes(msg);
      }

      //blocking for response
      String response = reader.readLine();
      Log.v(TAG, "recv'd response : " + response);
      
    } catch(IOException e) {
      Log.e(TAG, "sockClient:IOException:" + e.toString());
      throw e;
    }
  }

  /**
   *
   */
  private void run_over(int port_) {
    int tag = 0;

    //signals that we begin another iteration
    Taint.TMLog("runover |" + Taint.incTmCounter() + "| | | " + Integer.toHexString(tag));
  }

  public TMDevIdService(Context context) {
    super();

    mContext = context;

    //To prevent port number conflict
    mListener = new Thread(new TMListenerThread(Taint.tmport  + 1));
    mListener.start();
    
    Log.v(TAG, "mListener started: " + (Taint.tmport + 1) + ":" + mListener);
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
            } else if(line.startsWith("run_over")) {
              String[] tokens = line.split(" ");
              int port = 0;
              if (tokens.length == 1) {
                run_over(port);
              } else if (tokens.length == 2) {
                try {
                  port = Integer.parseInt(tokens[1]);
                  run_over(port);
                } catch(NumberFormatException e) {
                  Log.v(TAG, "run_over: NumberFormatException raised with " 
                        + tokens[1]);
                }
              }
            } else {
              Log.v(TAG, "unexpected input: " + line);
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
