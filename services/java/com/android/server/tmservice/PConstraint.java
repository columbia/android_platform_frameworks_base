package com.android.server.tmservice;

import java.util.*;

class PConstraint {
  //private List<String> lineList = null;
  private List<PConstElement> PConstList = null;

  public PConstraint(List<String> lineList_)  {
    //lineList = lineList_;

    PConstList = new ArrayList<PConstElement>();

    for (String line : lineList_) {
      String[] tmp0 = line.split(":")[1].split("|");

      int tid = Integer.parseInt(tmp0[0]);
      int offset = Integer.parseInt(tmp0[1]);
      String clazz = tmp0[2];
      String instr = tmp0[3];
      int brchoice = tmp0[4] == ">" ? 1 : 0;

      PConstList.add(new PConstElement(clazz, tid, offset, instr, brchoice));
    }

    Collections.sort(PConstList, new Comparator<PConstElement>() {
      public int compare(PConstElement a, PConstElement b) {
        return a.compare(b);
      }
    });
  }

  public boolean equals(PConstraint other) {
    return true;
  }
}

class PConstElement {
  private String clazz = null;
  private int tid = 0;
  private int offset = 0;
  private String instr = null;
  private int brchoice  = -1;

  public PConstElement(String clazz_, int tid_, int offset_, String instr_,
                       int brchoice_)
{
    clazz = clazz_;
    tid = tid_;
    offset = offset_;
    instr = instr_;
    brchoice = brchoice_;
  }

  public boolean equals(PConstElement other) {
    return (clazz == other.clazz) && (offset == other.offset) &&
      (brchoice == other.brchoice);
  }

  //to have some determinism with PConstList
  public int compare(PConstElement other) {
    int clazzCmp = clazz.compareTo(other.clazz);
    if (clazzCmp == 0) {
      if (offset > other.offset)  {
        return 1;
      } else if (offset < other.offset) {
        return -1;
      } else {
        if (brchoice > other.brchoice)  {
          return 1;
        } else if (brchoice < other.brchoice) {
          return -1;
        } else {
          return 0;
        }
      }
    } else {
      return clazzCmp;
    }
  }
}

