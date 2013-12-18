package com.android.server.tmservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Class bound to logcat channel to read events generated.
 *
 * @author Kangkook Jee
 */
public class TMLogcat {
  private Process logcat = null;
  private String command = null;
  private BufferedReader input = null;


  TMLogcat() {
    this("/system/bin/logcat -s dalvikvmtm -s TMLog");
  }

  TMLogcat(String cmd) {
    try {
      command = cmd;
      logcat = new ProcessBuilder(command.split(" ")).
        redirectErrorStream(true).start();

      try { logcat.getOutputStream().close(); } catch (IOException e) {}
      try { logcat.getErrorStream().close(); } catch (IOException e) {}
      input = new BufferedReader(
        new InputStreamReader(logcat.getInputStream()));
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  /**
   * Read logcat events non-blocking way.
   * @return
   */
  public List<String> getLineList() {
    //FIXME: The method emulates non-blocking return of the call.
    //This can be done in better way.
    long end=System.currentTimeMillis() + 60 * 10;
    List<String> ret = new ArrayList<String>();

    while (System.currentTimeMillis() < end) {
      try {
        if (input.ready()) {
          end=System.currentTimeMillis() + 60 * 10;
          String  line = input.readLine().trim();
          ret.add(line);
        }
      } catch(IOException e) {
        break;
      }
    }
    return ret;
  }
}