package com.android.server.tmservice;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
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

import dalvik.system.Taint;

/**
 *
 * @author Kangkook Jee
 *
 */
public abstract class TMService extends ITMService.Stub {

  protected static String TAG = "TMService";
  protected static boolean LOCAL_LOGV = false;
  protected static int ENTRY_MAX = 10;

  //Singleton elements
  protected static Thread mListener = null;
  protected static TMLogcat tmLogcat = null;
  protected static Map<String, TMService> tmSvcHMap =
              new HashMap<String, TMService>();

  //Fields related to Android service
  protected Context mContext = null;
  //A field for TAG value
  protected int tag = -1;

  static private String helpMessage = "Usage examples \n" +
  		"runover TMSvc [port] [cmd]\n" +
  		"disc\n";

  /**
   * Dummy implementation
   */
  public String getIMSI() {
    assert(false /* shouldn't be invoked */);
    return "000000000000000";
  }

  public String getIMEI() {
    assert (false);
    return "000000000000000";
  }

  /**
   * Method to get next TAG value. We want use different TAG values for
   * different test instances.
   *
   * @param tag
   *    TAG value used before.
   * @return
   *    TAG value to be used.
   */
  protected int getNextTag(int tag) {
      tag++;

      //just want to avoid TAINT_CLEAR
      if (tag == 0) {
          tag++;
      }
      return tag;
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
   * Register TM*Service to hash map structure so that it can be referred with service
   * reference label(TAG string). This is to be referred from TMListerThread listening to
   * users connecting via {@link #TMListener.tmport}.
   *
   * @param svcStr
   *    Service reference label
   * @param tmSvc
   *    Instance of TM*Svc
   */
  protected void registerTmSvc(String svcStr, TMService tmSvc) {
      tmSvcHMap.put(svcStr, tmSvc);
  }

  /**
   *
   * @return
   *    tag value to be injected.
   */
  public abstract int getTag();

  /**
   * This method make connection to MonkeyRunner script (or equivalent
   * something) to initiate another "run_over" event.  With the specified {@link
   * #port}, the connection is made by calling {@link #sockClient(String, int,
   * String[])} method. {@link #param} is to direct a specific sub-action.
   *
   * This method initiates another iteration.
   *
   * @param port
   *    port number to connect GUI fuzzer outside
   * @param subcmd
   *    sub-command to direct specific sub-action.
   */
  protected abstract void run_over(int port, String subcmd);

  /**
   * Constructor method.
   *
   * @param context
   */
  public TMService(Context context) {
    super();

    mContext = context;
    tag = 1;

    //Singleton elements
    if (tmLogcat == null) {
        tmLogcat = new TMLogcat();
    }

    if (mListener == null) {
        mListener = new Thread(new TMListenerThread(Taint.tmport));
        mListener.start();

        Log.v(TAG, "mListener started: " + Taint.tmport + ":" + mListener);
    }
  }

  protected class TMListenerThread implements Runnable {
    private int tmport = 0;
    private ServerSocket serverSocket = null;
    private Socket incoming = null;

    public void run() {
      try {
        serverSocket = new ServerSocket(tmport);
        Log.v(TAG, "TMListener thread initialized for port: " + tmport);

        while (true) {
          // FIXME: Now it only supports a single connection at a time.
          incoming = serverSocket.accept();
          Log.v(TAG, "TMListener thread -- client connected");
          BufferedReader reader = new BufferedReader(
            new InputStreamReader(incoming.getInputStream()));

          PrintWriter writer = new PrintWriter(
            incoming.getOutputStream(), true);

          while (true) {
            String line = reader.readLine().trim();

            //finish connection.
            if (line.startsWith("disc")) {
              reader.close();
              writer.close();
              incoming.close();
              break;

            // Branch to initiate another 'runover' event.
            } else if(line.startsWith("runover") ||
                        line.startsWith("run_over")) {
              String[] tokens = line.split(" ");
              int port = 0;
              String svcTAG = "";
              String cmd = "";
              TMService tmSvc = null;

              if (tokens.length > 4 || tokens.length < 2) {
                  Log.v(TAG, "unexpected input: " + line);
                  continue;
              }

              switch (tokens.length) {
                  case 4:
                      cmd = tokens[3];
                  case 3:
                      try {
                          port = Integer.parseInt(tokens[2]);
                      } catch (NumberFormatException e) {
                          Log.v(TAG, "run_over: NumberFormatException" +
                                  " raised with " + tokens[1]);
                          break;
                      }
                  case 2:
                      svcTAG = tokens[1];
                      tmSvc = tmSvcHMap.get(svcTAG);
                      if (tmSvc == null) {
                          Log.v(TAG, "TMService: " + svcTAG + "not found" );
                          break;
                      }
                  default:
                      tmSvc.run_over(port, cmd);
              }
            } else if (line.startsWith("help")) {
                writer.print(helpMessage);
            } else {
              writer.println("unexpected input:" + line);
              Log.v(TAG, "unexpected input: " + line);
            }
            writer.println("RESPONSE: " + line.trim());
          }
        }
      }  catch (IOException e) {
        //FIXME: need proper error handling
        Log.v(TAG, "socket error - port: " + tmport + e.toString());
      }
      //TODO: implement thread join event here
    }

    TMListenerThread(int port) {
      tmport = port;
    }
  }
}
