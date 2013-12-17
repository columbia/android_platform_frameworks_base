package com.android.server.tmservice;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
//import java.util.concurrent.Callable;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Thread;

import java.io.DataOutputStream;
import java.io.PrintWriter;

import android.content.Context;
import android.util.Log;

import com.android.tmservice.*;
import com.android.server.tmservice.TMLogcat;

/**
 * 
 * @author Kangkook Jee
 *
 */
public abstract class TMService extends ITMService.Stub {

  protected static String TAG = "TaintService";
  protected static boolean LOCAL_LOGV = false;
  protected static int ENTRY_MAX = 10;

  protected Context mContext = null;
  protected Thread mListener = null;
  protected TMLogcat tmLogcat = null;

  /**
   * Default implementation
   */
  public double getLatitude() {
    return 0.0;
  }

  /**
   * Default implementation
   */
  public double getLongitude() {
    return 0.0;
  }
  
  /**
   * Default implementation
   */
  public int getDevId() {
	  return 0;
  }

  /**
   * Static method that makes socket connection to the remote service. 
   * 
   * @param addr
   * @param port
   * @param msgs
   * @throws IOException
   */
  protected static void sockClient(String addr, int port, String[] msgs) throws IOException{
    Log.v(TAG, "sockClient is called with " + addr + " and " + port);
    int timeout = 30;

    try {
      Socket client = new Socket();
      client.connect(new InetSocketAddress(addr, port), timeout);
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
   * @param port
   * @param cmd
   */
  protected abstract void run_over(int port, String cmd);

  /**
   * Constructor method.
   * 
   * @param context
   */
//  public TMService(Context context) {
//    super();
//
//    mContext = context;
//  }

  protected class TMListenerThread implements Runnable {    
    private int tmport = 0;
    private ServerSocket serverSocket = null;
    private Socket incoming = null;
    
//    public void registerCallback(String SvcStr, Callable<Integer> func) {
//        ;
//    }

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
              String cmd = "";
              if (tokens.length == 2) {
            	cmd = tokens[1];
                run_over(port, cmd);
              } else if (tokens.length == 3) {
                try {
                  port = Integer.parseInt(tokens[1]);
                  cmd = tokens[2];
                 
                  run_over(port ,cmd);
                } catch(NumberFormatException e) {
                  Log.v(TAG, "run_over: NumberFormatException raised with " 
                        + tokens[1]);
                }
              } else {
            	  Log.v(TAG, "unexpected input: " + line);
              }
            } else {
              Log.v(TAG, "unexpected input: " + line);
            }
            writer.println("RESPONSE: " + line.trim());
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
