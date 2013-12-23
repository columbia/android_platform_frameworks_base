package com.android.server.tmservice;

import com.android.server.tmservice.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Class to represent an execution instance combining input, output, and 
 * path constraint list.
 *
 * @author Kangkook Jee
 */
class ExecTrace {
  /* TMeasure service related informations */
  private int tmSvcId = -1;
  private int tmSvcProcId = 0;
  Tuple<Double, Double, Integer> inputVal  = null;

  /* APP related informations */
  private List<ETraceElement> ETraceElList = null;
  private List<BrElement> brElList = null;
  private List<OutputElement> outputElList = null;
  private Map<Integer, ArrayList<BrElement>> tIdBrElMap = null;


  private int ETraceId = -1;
  private int appProcId = 0;

  //Constants to specify channel type
  public static final int DONT_KNOW = 0;
  public static final int CORRECT_CHANNEL = 1;
  public static final int FP_CHANNEL = 2;
  public static final int FN_CHANNEL = 3;
  public static final int ERR_UNKOWN = 4;

  /* List that contains valid output locations */
  private static final String[] outputLocations = 
          new String[] {
          "libcore.os.read0",
          "libcore.os.read1",
          "libcore.os.sendto0", 
          "libcore.os.pwrite0", 
          "libcore.os.pwrite1", 
          "libcore.os.write0",
          "libcore.os.write1"
          /* more to come */
          };

  public static String getOutputStr(int outType) {
    String ret = "INVALID OutType";

    switch(outType) {
      case DONT_KNOW: ret =  "DONT_KNOW";
        break;
      case CORRECT_CHANNEL: ret = "CORRECT_CHANNEL";
        break;
      case FP_CHANNEL: ret =  "FP_CHANNEL";
        break;
      case FN_CHANNEL: ret =  "FN_CHANNEL";
        break;
      case ERR_UNKOWN: ret =  "ERR_UNKOWN";
        break;
      default:
        break;
    }
    return ret;
  }

  public int getTmSvcId() {
    return tmSvcId;
  }

  /**
   * Checks whether line indicates new iteration begins.
   * @param line
   * @return
   */
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

  /**
   * Checks whether line represent the output event.
   *
   * @param line
   * @return
   */
  static public boolean isOutputLine(String line) {
    if (line == null) return false;

    String[] tmp = line.split("[|:]");
    String pat = "W/TMLog\\s*\\(\\s*(.+)\\)";
    int pId = getPIdFromLine(tmp[0], pat);
    if (pId < 0) {
      return false;
    }

    if (Arrays.asList(outputLocations).contains(tmp[1].trim())) {
      return true;
    }
    return false;
  }

  /**
   * Checks whether line represents the branch choice event.
   *
   * @param line
   * @return
   */
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

  /**
   * Extract pid from the line.
   *
   * @param line
   * @param pat regex pattern for line entry.
   * @return
   */
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
   * Constructor method for the class.
   */
  public ExecTrace(List<String> lineList_)  {
    ETraceElList = new ArrayList<ETraceElement>();
    brElList = new ArrayList<BrElement>();
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

        ETraceElList.add(new BrElement(pId, tm_id, clazz, tid, offset,
                                       instr, brchoice));
      } else if (isOutputLine(line)) {
        String[] tmp0  = line.split("[:|\\|]");

        String outputLoc = tmp0[1].trim();
        int tm_id = Integer.parseInt(tmp0[2].trim());
        int tid = Integer.parseInt(tmp0[3].trim());
        String data = tmp0[4].trim();
        int tag_ = Integer.parseInt(tmp0[5].trim().replace("0x","") ,16);
        ETraceElList.add(new OutputElement(pId, tm_id, tid, outputLoc, tag_, data));
      }
    }

    Collections.sort(ETraceElList, new Comparator<ETraceElement>() {
      public int compare(ETraceElement a, ETraceElement b) {
        return a.compare(b);
      }
    });

    /* FIXME: clean-up here a bit. */
    if (ETraceElList.size() > 0) {
      ETraceId = ETraceElList.get(0).get_tm_id();

      for (ETraceElement el_: ETraceElList) {
        if (OutputElement.class.isInstance(el_)) {
          OutputElement el = (OutputElement) el_;
          outputElList.add(el);
        } else if (BrElement.class.isInstance(el_)) {
          BrElement el = (BrElement) el_;

          //Build brElList
          brElList.add(el);
          //Build tIdBrElMap
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
    System.out.println("=== ETraceElList : " + ETraceElList.size() + "===" );
    for (ETraceElement ETraceEl: ETraceElList) {
      System.out.print(ETraceEl);
    }
    System.out.println("=== outputElList : "+outputElList.size()+ "===" );
    for (OutputElement outputEl: outputElList) {
      System.out.print(outputEl);
    }

    System.out.println("=== tIdBrElMap : "+ tIdBrElMap.size() + "===" );
    TMLocationAnalyzer.printMap(tIdBrElMap);
    System.out.println("=== End ===");
  }

  private boolean compareInput(ExecTrace other) {
    return inputVal.equals(other.inputVal);
  }

  private boolean compareBrElement(ExecTrace other) {
    //FIXME: now it isn't considering threads and scheduling factors. Fix it
    //using tIdBrElMap.

    if (brElList.size() != other.brElList.size())
      return false;

    boolean ret = true;
    for (int i = 0; i < brElList.size(); i++) {
      if (!brElList.get(i).equals( other.brElList.get(i)) ) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  private boolean compareOutput(ExecTrace other) {
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

  public boolean equals(ExecTrace other) {
    return compareInput(other) && compareBrElement(other)
      && compareOutput(other);

  }

  public String toString() {
    String ret = "ETraceID<" + tmSvcId + "::" + ETraceId + "> \n";
    for (ETraceElement ETrace: ETraceElList) {
      ret += ETrace;
    }
    return ret;
  }


/**
 * Function that implements actual algorithm to detect FN, FP by comparing
 * itself with other ExecTrace instance. Major improvements required to make
 *  the algorithm run as intended. Currently we have the following issues.
 *
 * 1) Comparing branch choices with different thread scheduling/interleaving.
 * 2) Relating output values from itself and other instance. Two factors to
 * consider at this point -- output locations (sendByteTo, ... ) and thread
 * IDs.
 *
 * For the same path constraint(branch choice) sequences, we may observe
 * different output combinations differ in various ways. We need to come up
 * with a systematic solution to compare itself and the other instance.
 *
 * @param other An instance of {@link #ExecTrace(List)} to compare with
 * @return One of the following constants {@value #DONT_KNOW}
 *   {@value #CORRECT_CHANNEL} {@value #FP_CHANNEL} {@value #FP_CHANNEL}
 *
 */
  public int  isCorrectChannel(ExecTrace other) {
    //A case for different path, different inputs -- don't care
    if (!compareBrElement(other) && inputVal.equals(other.inputVal)) {
      return DONT_KNOW;

    //same paths, same inputs -- don't care
    } else if (compareBrElement(other) && inputVal.equals(other.inputVal)) {
      //Outputs should match -- deterministic execution model.
      assert(compareOutput(other));
      return DONT_KNOW;
      //same path for different inputs
    } else if (compareBrElement(other) && !inputVal.equals(other.inputVal)) {
      //tag value should differ
      if (inputVal.tag != other.inputVal.tag) {
        if (outputElList.size() ==  other.outputElList.size()) {
          //outElList sorted by TM id.
          for (int i = 0 ; i < outputElList.size(); i++) {
            OutputElement outLoc0 = outputElList.get(i);
            OutputElement outLoc1 = other.outputElList.get(i);

            //The same output for same output location with different input values.
            if ( outLoc0.getOutputLoc().equals(outLoc1.getOutputLoc())
              && outLoc0.getData().equals(outLoc1.getData()))  {
              //TODO: For now, we just discard the case. Think more about this case.
              continue;
            }

            //Change made to output data by input values for the same output location.
            else if (outLoc0.getOutputLoc().equals(outLoc1.getOutputLoc())
                  && (!outLoc0.getData().equals(outLoc1.getData())) ) {
              //FP detected -- input's tag value not propagated.
              if (outLoc0.getTag() != inputVal.tag) {
                return FP_CHANNEL;
              }
              continue;

            // Different inputs invoked changes to the same output locations.
            } else if (!outLoc0.getOutputLoc().equals(outLoc1.getOutputLoc())) {
              if (outLoc0.getTag() != inputVal.tag) {
                return FN_CHANNEL;
              }
            }
          }
        // The size of outputLoc list differ.
        // TODO: think how to deal with this case.
        } else {
        return DONT_KNOW;
        }

      //tag value doesn't differ between 'thi's and 'other'
      } else {
        //tag value should vary.
        return DONT_KNOW;
      }
    //different paths visited for same inputs -- doesn't seem likely
    } else {
      assert(false);
      return DONT_KNOW;
    }
    return CORRECT_CHANNEL;
  }

  /**
   * Getter method for TM*Svc process id
   * @return String
   */
  public int getTmSvcProcId() {
    return tmSvcProcId;
  }
}

/**
 * Abstract class that sub-classes the element class represents branch choice
 *  and output events.
 *
 * @author Kangkook Jee
 */
abstract class ETraceElement {
  protected int pid;
  protected int tm_id;
  protected int tid = 0;

  //to have some determinism with PCostElList
  public int compare(ETraceElement other) {
    return tm_id - other.tm_id;
  }
  public int get_tm_id() {
    return tm_id;
  }
  abstract public boolean equals(ETraceElement other);
}

/**
 * Class that represents branch choice entry.
 *
 * @author Kangkook Jee
 */
class BrElement extends ETraceElement {
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

  public String getInstr() {
    return instr;
  }

  public boolean equals(ETraceElement other_) {
    if (BrElement.class.isInstance(other_)) {
      BrElement other = (BrElement) other_;

      return (clazz.equals(other.clazz)) && (offset == other.offset) &&
        (brchoice == other.brchoice);
    }
    return false;
  }

  public String toString() {
    String ret = "BR::" + pid + " (" + tid + ") :: " + tm_id + " :: " + clazz 
      + "@" + offset + " :: (" + tid +") :: " + brchoice + "\n";
    return ret;
  }
}

/**
 * Class that represents output entry.
 *
 * @author Kangkook Jee
 */
class OutputElement extends ETraceElement {
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

  public boolean equals(ETraceElement other_) {
    if (OutputElement.class.isInstance(other_)) {
      OutputElement other = (OutputElement) other_;
      return (outputLoc.equals(other.outputLoc))
        && (data.equals(other.data))
        // && (tag == other.tag)
        ;
    }
    return false;
  }

  public String getOutputLoc() {return outputLoc;}
  public String getData() {return data;}
  public int getTag() {return tag;}

  public String toString() {
    String ret = "OUT::" + pid + " (" + tid + ") :: "  + tm_id + "::" + 
      outputLoc + "::" + tag +"::" + data + "\n";
    return ret;
  }
}