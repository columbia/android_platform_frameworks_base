package com.android.server.tmservice;
import java.io.*;
import java.util.*;
import com.android.server.tmservice.PConstraint;

class TMLocationAnalyzer {

  static private Map<Integer, PConstraint> hm = new HashMap<Integer, PConstraint>();

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

        processLineList(lineList);
        
      } catch (IOException ie) {
        System.err.println("Error processing " + fname + " " + ie.toString());
      }
    } else {
      TMLogcat tmLogcat = new TMLogcat("adb logcat -s dalvikvmtm -s  TMLog");
      List<String> lineList = tmLogcat.getLineList();
      processLineList(lineList); 
    }
   
    printMap(hm);
  }

  public static void printMap(Map mp) {
    Iterator it = mp.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pairs = (Map.Entry)it.next();
      System.out.println(pairs.getKey() + " = " + pairs.getValue());
      it.remove(); // avoids a ConcurrentModificationException
    }
  }

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

    while(line !=null && !PConstraint.isHeaderLine(line)) {
      line = it.next();
    }

    //eat up lines until we meet the first header line 
    while(true) {
      if (PConstraint.isHeaderLine(line)) {
        if (lineList.size() != 0) {
          PConstraint pConst = new PConstraint(lineList);
          hm.put(pConst.getTmSvcId(), pConst);
        }

        lineList = new ArrayList<String>();
        lineList.add(line);
      } else if (PConstraint.isBrLine(line) || 
                 PConstraint.isOutputLine(line)) {
        lineList.add(line);
      }
      try {
        line = it.next();
      } catch (NoSuchElementException ne) {break;}
    }
    if (lineList.size() != 0) {
      PConstraint pConst = new PConstraint(lineList);
      hm.put(pConst.getTmSvcId(), pConst);
    }
  }
}