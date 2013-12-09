package com.android.server.tmservice;
import java.io.*;
import java.util.*;
//import com.android.server.tmservice.PConstraint;

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
      String line = br.readLine();
      ArrayList<String> lineList = new ArrayList<String>();

      while(line != null) {
        if (line.startsWith("V/")) {
          if (lineList.size() != 0) {
            PConstraint pConst = new PConstraint(lineList);
            System.out.println("<DBG:PConst>\n" +  pConst);
            pConstList.add(pConst);
          }
          lineList = new ArrayList<String>();
        } else if (line.startsWith("E/")) {
          lineList.add(line);
        }
        line = br.readLine();

      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}