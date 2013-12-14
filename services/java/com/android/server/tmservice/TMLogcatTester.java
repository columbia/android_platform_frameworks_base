package com.android.server.tmservice;
import java.io.*;
import java.util.*;
import com.android.server.tmservice.PConstraint;

class TMLogcatTester {
  public static void main(String[] args) {
    String fname  = null;
    List<PConstraint> pConstList = new ArrayList<PConstraint>();

    if (args.length == 1) {
      fname = args[0];
    } else {
      System.err.println("Invalid usage");
      System.exit(0);
    }

    try {
      FileInputStream fis = new FileInputStream(fname);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));
      ArrayList<String> lineList = new ArrayList<String>();
      String line = br.readLine();

      //eat up lines until we meet the first header line 
      while(!PConstraint.isHeaderLine(line))  
        line = br.readLine();

      while(line != null) {
        if (PConstraint.isHeaderLine(line)) {
          if (lineList.size() != 0) {
            PConstraint pConst = new PConstraint(lineList);
            pConstList.add(pConst);
          }
          lineList = new ArrayList<String>();
          lineList.add(line);
        } else if (PConstraint.isBrLine(line) || 
                   PConstraint.isOutputLine(line)) {
          lineList.add(line);
        }
        line = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }


    TMLogcat tmLogcat = new TMLogcat("adb logcat -s dalvikvmtm");
    List<String> lineList = tmLogcat.getLineList();
 
    for(String line: lineList) {
      System.out.println("DBG:" + line);
    }
  }
}