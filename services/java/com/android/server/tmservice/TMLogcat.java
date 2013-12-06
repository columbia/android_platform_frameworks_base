package com.android.server.tmservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.util.*;

class TMLogcat {
  private Process logcat = null;
  private String command = "/system/bin/logcat -s dalvikvmtm";
  private BufferedReader input = null;
  private PConstraint pConst = null;
 
  //singleton?
  public TMLogcat() {
    try {
      logcat = new ProcessBuilder(command.split(" ")).redirectErrorStream(true).start();

      try { logcat.getOutputStream().close(); } catch (IOException e) {}
      try { logcat.getErrorStream().close(); } catch (IOException e) {}
      input = new BufferedReader(new InputStreamReader(logcat.getInputStream()));

    } catch (IOException e) {
      System.err.println(e);
    }
  }

  public List<String> getLineList() {
    List<String> ret = new ArrayList<String>();
    for (int i = 0; ; i++) {
      try {
        String  line = input.readLine().trim();
        if (line == null) {
          break;
        } else {
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