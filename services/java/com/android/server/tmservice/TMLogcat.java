package com.android.server.tmservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.util.*;

public class TMLogcat {
  private Process logcat = null;
  private String command = null;
  private BufferedReader input = null;
  private PConstraint pConst = null;

  TMLogcat() {
    this("/system/bin/logcat -s dalvikvmtm");
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

  public List<String> getLineList() {
    //FIXME: making it somewhat non-blocking
    //need some improvements.
    long end=System.currentTimeMillis() + 60*10;
    List<String> ret = new ArrayList<String>();

    while (System.currentTimeMillis() < end) {
      try {
        if (input.ready()) {
          end=System.currentTimeMillis() + 60*10;
          String  line = input.readLine().trim();
          ret.add(line);
        }
      } catch(IOException e) {
        break;
      }
    }
    return ret;
  }

  public PConstraint getPConstList() {
    if (pConst == null) {
      pConst = new PConstraint(getLineList());
    }
    return pConst;
  }
}

