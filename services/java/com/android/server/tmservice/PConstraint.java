package com.android.server.tmservice;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class PConstraint {
  private List<PConstElement> PConstElList = null;
  private int pConstTMId = -1;
  private int pId = 0;

  public PConstraint(List<String> lineList_)  {
    PConstElList = new ArrayList<PConstElement>();

    for (String line : lineList_) {
      String[] tmp0  = line.split("[:|\\|]");

      //extracting pid
      String pat = "./.+\\( (.+)\\)";
      Pattern r = Pattern.compile(pat);
      Matcher m = r.matcher(tmp0[0]);
      int pid = 0;

      try {
        pid = Integer.parseInt(m.group(1));
      } catch (NumberFormatException ne) {
        //TODO: have proper error handling here.
        throw ne;
      }

      //setting / checking pId for this object
      if (pId == 0) {
        pId = pid;
      } else {
        assert(pId == pid);
      }
      

      int tm_id = Integer.parseInt(tmp0[1].trim());
      int tid = Integer.parseInt(tmp0[2].trim());
      int offset = Integer.parseInt(tmp0[3].trim(), 16);
      String clazz = tmp0[4];
      String instr = tmp0[5];
      int brchoice = tmp0[7].trim().equals(">") ? 1 : 0;

      PConstElList.add(new PConstElement(pid, tm_id, clazz, tid, offset, 
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

class PConstElement {
  private int pid;
  private int tm_id;
  private String clazz = null;
  private int tid = 0;
  private int offset = 0;
  private String instr = null;
  private int brchoice  = -1;

  public PConstElement(int pid_, int tm_id_, String clazz_, int tid_, 
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

  public int get_tm_id() {
    return tm_id;
  }

  public boolean equals(PConstElement other) {
    return (clazz == other.clazz) && (offset == other.offset) &&
      (brchoice == other.brchoice);
  }

  //to have some determinism with PConstElList
  public int compare(PConstElement other) {
    return tm_id - other.tm_id;
  }

  public String toString() {
    String ret = tm_id + " :: " + clazz + "@" + offset + 
      " :: (" + tid +") :: " + brchoice + "\n";
    return ret;
  }
}
