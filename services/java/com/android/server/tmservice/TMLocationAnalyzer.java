package com.android.server.tmservice;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.android.server.tmservice.ExecTrace;

class TMLocationAnalyzer {
  static private Map<Integer, ExecTrace> hMap = new HashMap<Integer, ExecTrace>();
  static private ExecTrace predETrace = null;

  public static void main(String[] args) {
    String fname  = null;

    if (args.length == 1) {
      fname = args[0];

      try {
        FileInputStream fis = new FileInputStream(fname);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        ArrayList<String> lineList = new ArrayList<String>();
        String line = br.readLine();

        while(line !=null) {
          lineList.add(line);
          line = br.readLine();
        }
        br.close();

        processLineList(lineList);

      } catch (IOException ie) {
        System.err.println("Error processing " + fname + " " + ie.toString());
      }
    } else {
      TMLogcat tmLogcat = new TMLogcat("adb logcat -s dalvikvmtm -s  TMLog");
      List<String> lineList = tmLogcat.getLineList();
      processLineList(lineList);
    }

    //printMap(hm);
  }

  /**
   * Takes list of string to parse and to update {@link #hMap} with
   * {@link ExecTrace} instances. Along the way, it invokes
   * {@link #isCorrectChannel(ExecTrace)} to examine the correctness
   * of taint channel represented by a {@link ExecTrace} instance.
   *
   * @param inputList List of {@link #java.lang.String} to be parsed to
   * create a list of ExecTrace instances.
   */
  private static void processLineList(List<String> inputList) {
    ArrayList<String> lineList = new ArrayList<String>();
    Iterator<String> it = inputList.iterator();

    String line = null;
    try {
      line = it.next();
    } catch (NoSuchElementException ne) {
      //no line
      return;
    }

    while(line !=null && !ExecTrace.isHeaderLine(line)) {
      line = it.next();
    }

    //eat up lines until we meet the first header line
    while(true) {
      if (ExecTrace.isHeaderLine(line)) {
        if (lineList.size() != 0) {
          ExecTrace ETrace = new ExecTrace(lineList);
          if (predETrace == null) {
            predETrace = ETrace;
          } else {
            System.out.print("Comparing ETrace(" + ETrace.getTmSvcId() +
                  ") and ETrace(" + predETrace.getTmSvcId() + "): ");
            System.out.println(ExecTrace.getOutputStr(
                  ETrace.isCorrectChannel(predETrace)));
            predETrace = ETrace;
          }

          hMap.put(ETrace.getTmSvcId(), ETrace);
        }

        lineList = new ArrayList<String>();
        lineList.add(line);
      } else if (ExecTrace.isBrLine(line) ||
                 ExecTrace.isOutputLine(line)) {
        lineList.add(line);
      }
      try {
        line = it.next();
      } catch (NoSuchElementException ne) {break;}
    }
    if (lineList.size() != 0) {
      ExecTrace ETrace = new ExecTrace(lineList);

      if (predETrace != null) {
        System.out.print("Comparing ETrace(" + ETrace.getTmSvcId() +
              ") and ETrace(" + predETrace.getTmSvcId() + "): ");
        System.out.println(ExecTrace.getOutputStr(
              ETrace.isCorrectChannel(predETrace)));
      }

      hMap.put(ETrace.getTmSvcId(), ETrace);
    }
  }

  /**
   * Utility method that outputs the content of MAP.
   * @param mp: map data structure
   */
  public static void printMap(Map mp) {
    Iterator it = mp.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pairs = (Map.Entry)it.next();
      System.out.println(pairs.getKey() + " = " + pairs.getValue());
      it.remove(); // avoids a ConcurrentModificationException
    }
  }
}
