package com.android.server.tmservice;

import java.util.*;

class PConstraint {
  private List<PConstElement> PConstElList = null;
  private int pConstId = -1;

  public PConstraint(List<String> lineList_)  {
    PConstElList = new ArrayList<PConstElement>();

    for (String line : lineList_) {
      String[] tmp0  = line.split("[:|]");
      int tm_counter = Integer.parseInt(tmp0[1].trim());
      int tid = Integer.parseInt(tmp0[2].trim());
      int offset = Integer.parseInt(tmp0[3].trim(), 16);
      String clazz = tmp0[4];
      String instr = tmp0[5];
      int brchoice = tmp0[7].trim().equals(">") ? 1 : 0;

      PConstElList.add(new PConstElement(tm_counter, clazz, tid, offset, instr, brchoice));
    }

    Collections.sort(PConstElList, new Comparator<PConstElement>() {
      public int compare(PConstElement a, PConstElement b) {
        return a.compare(b);
      }
    });

    pConstId = PConstElList.get(0).get_tm_counter();
  }

  public boolean equals(PConstraint other) {
    return true;
  }

  public String toString() {
    String ret = "PConstID<" + pConstId + "> ";
    for (PConstElement pConst: PConstElList) {
      ret += pConst;
    }
    return ret;
  }
}

class PConstElement {
  private int tm_counter;
  private String clazz = null;
  private int tid = 0;
  private int offset = 0;
  private String instr = null;
  private int brchoice  = -1;

  public PConstElement(int tm_counter_, String clazz_, int tid_, int offset_, String instr_,
                       int brchoice_)
  {
    tm_counter = tm_counter_;
    clazz = clazz_;
    tid = tid_;
    offset = offset_;
    instr = instr_;
    brchoice = brchoice_;
  }

  public int get_tm_counter() {
    return tm_counter;
  }

  public boolean equals(PConstElement other) {
    return (clazz == other.clazz) && (offset == other.offset) &&
      (brchoice == other.brchoice);
  }

  //to have some determinism with PConstElList
  public int compare(PConstElement other) {
    return tm_counter - other.tm_counter;
  }

  public String toString() {
    String ret = tm_counter + " :: " + clazz + "@" + offset + 
      " :: (" + tid +") :: " + brchoice + "\n";
    return ret;
  }
}

