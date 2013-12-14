package com.android.server.tmservice;

import com.android.server.tmservice.Tuple;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class PConstraint {
  /* TMeasure service related infos */
  private int tmSvcTmId = 0;
  private int tmSvcProcId = 0;
  Tuple<Double, Double, Integer> inputVal  = null;

  /* APP related infos */
  private List<PConstElement> PConstElList = null;
  private int pConstTMId = -1;
  private int appProcId = 0;

  static public boolean isHeaderLine(String line) {

    if (line == null) return false;

    String[] tmp = line.split("[|:]");
    String pat = "W/TMLog\\s*\\(\\s*(.+)\\)";
    int pId = getPIdFromLine(tmp[0], pat);
    if (pId < 0) {
      return false;
    }

    if (tmp[1].trim().equals("runover")) {
      return true;
    }
    return false;
  }

  static public boolean isOutputLine(String line) {
    if (line == null) return false;

    String[] outputList = new String[] {"sendtoBytes" 
                                      /* other output locations will follow
                                       * here */
    };

    String[] tmp = line.split("[|:]");
    String pat = "W/TMLog\\s*\\(\\s*(.+)\\)";
    int pId = getPIdFromLine(tmp[0], pat);
    if (pId < 0) {
      return false;
    }
    
    if (Arrays.asList(outputList).contains(tmp[1].trim())) {
      return true;
    }
    return false;
  }

  static public boolean isBrLine(String line) {
    if (line == null) return false;

    String[] tmp  = line.split("[:|\\|]");

    String pat = "E/dalvikvmtm\\s*\\(\\s*(.+)\\)";
    int pId = getPIdFromLine(tmp[0], pat);
    if (pId < 0) {
      return false;
    }
    return true;
  }

  static private int getPIdFromLine(String line) {
    String pat = "./.+\\s*\\(\\s*(.+)\\)";
    return getPIdFromLine(line, pat);
  }

  static private int getPIdFromLine(String line, String pat) {
    String tok = null;
    int pid = -1;
    if (Arrays.asList(line).contains(":")) {
      tok = line.split(":")[0];
    } else {
      tok = line;
    }
    //System.err.println("DBG:" + tok + ":" + pat);
    Pattern r = Pattern.compile(pat);
    Matcher m = r.matcher(tok.trim());
    
    if (!m.find()) {return pid;};
    try {
      pid = Integer.parseInt(m.group(1));
    } catch (NumberFormatException ne) {
      /* parse failed */
      ;
    }
    return pid;
  }

  /**
   * Constructor method.
   */
  public PConstraint(List<String> lineList_)  {
    PConstElList = new ArrayList<PConstElement>();

    //Parse header line.
    String hline = lineList_.get(0);
    assert(isHeaderLine(hline));
    tmSvcProcId = getPIdFromLine(hline);

    String[] tmp = hline.split("[\\|]");

    tmSvcTmId = Integer.parseInt(tmp[1].trim());
    double latitude = Double.parseDouble(tmp[2].trim());
    double longitude = Double.parseDouble(tmp[3].trim());
    int tag = Integer.parseInt(tmp[4].trim());

    inputVal = new Tuple<Double, Double, Integer>
      (new Double(latitude), new Double(longitude),
       new Integer(tag));

    for (int i = 1;  i < lineList_.size(); i++) {
      String line = lineList_.get(i);
      int pId = getPIdFromLine(line);
      
      if (appProcId == 0) {
        appProcId = pId;
      } else {
        assert(pId == appProcId);
      }
      
      System.out.println("DBG: " + line + ":" + isBrLine(line) + ":" + isOutputLine(line));
      String[] tmp0  = line.split("[:|\\|]");

      int tm_id = Integer.parseInt(tmp0[1].trim());
      int tid = Integer.parseInt(tmp0[2].trim());
      int offset = Integer.parseInt(tmp0[3].trim(), 16);
      String clazz = tmp0[4];
      String instr = tmp0[5];
      int brchoice = tmp0[7].trim().equals(">") ? 1 : 0;

      PConstElList.add(new BrElement(pId, tm_id, clazz, tid, offset, 
                                         instr, brchoice));
    }

    Collections.sort(PConstElList, new Comparator<PConstElement>() {
      public int compare(PConstElement a, PConstElement b) {
        return a.compare(b);
      }
    });
    pConstTMId = PConstElList.get(0).get_tm_id();
  }

  public boolean equals(PConstraint other) {
    /* TODO: */
    return true;
  }

  public String toString() {
    String ret = "PConstTMID<" + pConstTMId + "> ";
    for (PConstElement pConst: PConstElList) {
      ret += pConst;
    }
    return ret;
  }
}

/** 
 * Abstract class 
 */
abstract class PConstElement {
  protected int pid;
  protected int tm_id;
  protected int tid = 0;

  //to have some determinism with PCostElList
  public int compare(PConstElement other) {
    return tm_id - other.tm_id;
  }
  public int get_tm_id() {
    return tm_id;
  }
}

class BrElement extends PConstElement {
  private String clazz = null;
  private int offset = 0;
  private String instr = null;
  private int brchoice  = -1;

  public BrElement(int pid_, int tm_id_, String clazz_, int tid_, 
                       int offset_, String instr_, int brchoice_)
  {
    pid = pid_;
    tm_id = tm_id_;
    clazz = clazz_;
    tid = tid_;
    offset = offset_;
    instr = instr_;
    brchoice = brchoice_;
  }


  public boolean equals(BrElement other) {
    return (clazz == other.clazz) && (offset == other.offset) &&
      (brchoice == other.brchoice);
  }

  public String toString() {
    String ret = tm_id + " :: " + clazz + "@" + offset + 
      " :: (" + tid +") :: " + brchoice + "\n";
    return ret;
  }
}
  
class OutputElement extends PConstElement {
  String outputLoc = null;
  int tag = -1;
  String data = null;

  public OutputElement (int pid_, int tm_id_, int tid_, String outputLoc_, 
                        int tag_, String data_) {
    pid = pid_;
    tm_id = tm_id_;
    tid = tid_;
    outputLoc = outputLoc_;
    tag = tag_;
    data = data_;
  }
  public String toString() {
    String ret = pid + " (" + tid + ") :: "  + tm_id + "::" + outputLoc + "::"
      + tag +"::" + data + "\n";
    return ret;
  }
}
