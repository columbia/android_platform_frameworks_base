package com.android.server.tmservice;

import com.android.server.tmservice.Tuple;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class PConstraint {
  /* TMeasure service related infos */
  private int tmSvcId = -1;
  private int tmSvcProcId = 0;
  Tuple<Double, Double, Integer> inputVal  = null;

  /* APP related infos */
  private List<PConstElement> pConstElList = null;
  private List<OutputElement> outputElList = null;
  private Map<Integer, ArrayList<BrElement>> tIdBrElMap = null;


  private int pConstId = -1;
  private int appProcId = 0;

  public int getTmSvcId() {
    return tmSvcId;
  }

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
   * Constructor.
   */
  public PConstraint(List<String> lineList_)  {
    pConstElList = new ArrayList<PConstElement>();
    outputElList = new ArrayList<OutputElement>();
    tIdBrElMap = new HashMap<Integer, ArrayList<BrElement>>();

    //Parse header line.
    String hline = lineList_.get(0);
    assert(isHeaderLine(hline));
    tmSvcProcId = getPIdFromLine(hline);

    String[] tmp = hline.split("[\\|]");

    tmSvcId = Integer.parseInt(tmp[1].trim());
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
      
      if (isBrLine(line)) {
        String[] tmp0  = line.split("[:|\\|]");

        int tm_id = Integer.parseInt(tmp0[1].trim());
        int tid = Integer.parseInt(tmp0[2].trim());
        int offset = Integer.parseInt(tmp0[3].trim(), 16);
        String clazz = tmp0[4];
        String instr = tmp0[5];
        int brchoice = tmp0[7].trim().equals(">") ? 1 : 0;

        pConstElList.add(new BrElement(pId, tm_id, clazz, tid, offset, 
                                       instr, brchoice));
      } else if (isOutputLine(line)) {
        String[] tmp0  = line.split("[:|\\|]");

        String outputLoc = tmp0[1].trim();
        int tm_id = Integer.parseInt(tmp0[2].trim());
        int tid = Integer.parseInt(tmp0[3].trim());
        String data = tmp0[4].trim();
        int tag_ = Integer.parseInt(tmp0[5].trim().replace("0x","") ,16);
        pConstElList.add(new OutputElement(pId, tm_id, tid, outputLoc, tag_, data));
      }
    }

    Collections.sort(pConstElList, new Comparator<PConstElement>() {
      public int compare(PConstElement a, PConstElement b) {
        return a.compare(b);
      }
    });
    if (pConstElList.size() > 0) {
      pConstId = pConstElList.get(0).get_tm_id();
 
      for (PConstElement el_: pConstElList) {
        if (OutputElement.class.isInstance(el_)) {
          OutputElement el = (OutputElement) el_;
          outputElList.add(el);
        } else if (BrElement.class.isInstance(el_)) {
          BrElement el = (BrElement) el_;
          int tid = el.getTId();
          if (tIdBrElMap.containsKey(tid)) {
            ArrayList<BrElement> brList = tIdBrElMap.get(tid);
            brList.add(el);
          } else {
            ArrayList<BrElement> brList = new ArrayList<BrElement>();
            brList.add(el);
            tIdBrElMap.put(tid, brList);
          }
        } else {
          assert( false /* not valid branch */);
        }
      }
    }
  }

  public void dbgOutput() {
    System.out.println("=== pConstElList : " + pConstElList.size() + "===" );
    for (PConstElement pConstEl: pConstElList) {
      System.out.print(pConstEl);
    }
    System.out.println("=== outputElList : "+outputElList.size()+ "===" );
    for (OutputElement outputEl: outputElList) {
      System.out.print(outputEl);
    }

    System.out.println("=== tIdBrElMap : "+ tIdBrElMap.size() + "===" );
    printMap(tIdBrElMap);
    System.out.println("=== End ===");
  }

  public static void printMap(Map mp) {
    Iterator it = mp.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pairs = (Map.Entry)it.next();
      System.out.println(pairs.getKey() + " = " + pairs.getValue());
      it.remove(); // avoids a ConcurrentModificationException
    }
  }

  private boolean compareInput(PConstraint other) {
    return inputVal.equals(other.inputVal);
  }


  private boolean compareBrElement(PConstraint other) {
    //FIXME: now it isn't considering threads and scheduling factors. Fix it
    //using tIdBrElMap.
   
    if (pConstElList.size() != other.pConstElList.size()) 
      return false;

    boolean ret = true;
    for (int i = 0; i < pConstElList.size(); i++) {
      if (!pConstElList.get(i).equals( other.pConstElList.get(i)) ) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  private boolean compareOutput(PConstraint other) {
    //Here, we care about the execution sequence of output
    if (outputElList.size() != other.outputElList.size()) 
      return false;

    boolean ret = true;
    for (int i = 0; i < outputElList.size(); i++) {
      if (outputElList.get(i).equals(other.outputElList.get(i)) ) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  public boolean equals(PConstraint other) {
    return compareInput(other) && compareBrElement(other)
      && compareOutput(other);

  }

  public String toString() {
    String ret = "PConstID<" + tmSvcId + "::" + pConstId + "> \n";
    for (PConstElement pConst: pConstElList) {
      ret += pConst;
    }
    return ret;
  }

  public boolean isFN(PConstraint other) {
    return true;
  } 

  public boolean isFP(PConstraint other) {
    //different path, different inputs -- don't care
    if (!compareBrElement(other) && inputVal != other.inputVal) {
      return false;

      //same paths, same inputs -- don't care 
    } else if (compareBrElement(other) && inputVal == other.inputVal) {
      //outputs should match -- deterministic execution model.
      assert(compareOutput(other));
      return false;
      //same path for different inputs 
    } else if (compareBrElement(other) && inputVal != other.inputVal) {
      //tag value differs
      if (inputVal.z != other.inputVal.z) {
        //output compare -- todo
     
      } else {
        return false;
      }
      //different paths for same inputs -- doesn't seem likely
    }  else {
      assert(false);
      return false;
    }
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
  abstract public boolean equals(PConstElement other);
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

  public int getTId() {
    return tid;
  }

  public boolean equals(PConstElement other_) {
    if (BrElement.class.isInstance(other_)) {
      BrElement other  = (BrElement) other_;
      return (clazz == other.clazz) && (offset == other.offset) &&
        (brchoice == other.brchoice);
    } 
    return false;
  }

  public String toString() {
    String ret = "BR::" + pid + " (" + tid + ") :: " + tm_id + " :: " + clazz + "@" + offset + 
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

  public boolean equals(PConstElement other_) {
    if (OutputElement.class.isInstance(other_)) {
      OutputElement other = (OutputElement) other_;
      return (outputLoc == other.outputLoc)
        && (data == other.data) 
        // && (tag == other.tag) 
        ;
    }
    return false;
  }

  public String toString() {
    String ret = "OUT::" + pid + " (" + tid + ") :: "  + tm_id + "::" + outputLoc + "::"
      + tag +"::" + data + "\n";
    return ret;
  }
}
