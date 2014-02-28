import sys
import itertools

from Util import tuplify
from ExecTrace import ExecTrace, BrLog, OutLog
from LCS import LCS, LCS2


CORRECT_CHANNEL = 1
ERROR_DETECTED = 1

#
# Utility methods
#


def compareOutList(outList0, outList1):
    # TODO: implement.
    if len(outList0) == len(outList1):
        for outEnt0, outEnt1 in zip(outList0, outList1):
            if outEnt0 != outEnt1:
                return False
        else:
            return True
    else:
        return False


def getAvailableInOutLocPairs(execTraceList):
    """
    @param execTraceList: list of ExecTrace instances.
    @return: list of all available input and output locations shown from
    execTraceList in the form of [(inLoc0, outLoc0), ...]
    """
    ret = set()
    for execTrc in execTraceList:
        inLocList = execTrc.inputMap.keys()
        for inLoc in inLocList:
            for outLoc in execTrc.outLog.getDistOutLocList():
                ret.add((inLoc, outLoc))

    return list(ret)


def getTaintChannel(execTraceList):
    """
    This method is to infer a list of taint channel from execution trace list.
    (Please note that a execution path can have multiple taint channel.)
    This process can be regarded as a process of inferring ground-truth

    @param execTraceList: list of ExecTrace instances.
    @return: map(dictionary) structure that has branch choices(brChoice) as
    the key and a list of input and output locations pairs as its value.
    """
    it = itertools.combinations(execTraceList, 2)
    #inOutLocList = getAvailableInOutLocPairs(execTraceList)

    taintChannelSet = set()
    noTaintChannelSet = set()
    try:
        # Comparing for all available execution trace combination pairs
        while True:
            pair = it.next()
            execTrc0, execTrc1 = pair
            for inOutLoc in getAvailableInOutLocPairs(pair):
                inLoc, outLoc = inOutLoc

                # Checks whether output value responds to the changes of the
                # input values.

                if (execTrc0.getInValFor(inLoc) !=
                    execTrc1.getInValFor(inLoc)) \
                     and not compareOutList(execTrc0.getOutValListFor(outLoc),
                                            execTrc1.getOutValListFor(outLoc)):

                    assert(execTrc0.brLog == execTrc1.brLog and
                           "we are expecting to see identical brLog")

                    taintChannelSet.append((inLoc, outLoc))

                elif (execTrc0.getInValFor(inLoc) ==
                      execTrc1.getInValFor(inLoc)) \
                    and not compareOutList(execTrc0.getOutValListFor(outLoc),
                                           execTrc1.getOutValListFor(outLoc)):
                    noTaintChannelSet.add((inLoc, outLoc))

    except StopIteration:
        taintChannelList = list(taintChannelSet.difference(noTaintChannelSet))
        taintChannelList.sort()
        return taintChannelList


def EvalChannelFN(execTraceList):
    """
    The function verifies whether taint channel delivers taintedness(tag value)
    properly.

    @param execTraceList: List of ExecTrace instances.
    @return : Map(dictionary) structure that has branch choices(brChoice) as
    a key and its correctness result as a value.
    """
    taintChannelMap = getTaintChannel(execTraceList)
    result = {}

    for brChoice in taintChannelMap:
        for inOutLoc in taintChannelMap[brChoice]:
            inLoc, outLoc = inOutLoc
            for execTrc in execTraceList:
                if execTrc.getInTagVal(inLoc) != execTrc.getOutTagVal(outLoc):
                    # Taintedness didn't correctly flowed -- maybe false
                    # negative
                    if brChoice in result:
                        result[brChoice].append(
                            (execTrc, inOutLoc, ERROR_DETECTED))
                    else:
                        result[brChoice] = [
                            (execTrc, inOutLoc, ERROR_DETECTED)]

        # Iterated over all input, output locations for brChoice
        else:
            if brChoice not in result:
                result[brChoice] = CORRECT_CHANNEL

    return result


def EvalChannelFP(execTraceList):
    """
    The function verifies whether taintedness(tag value) propagated to any
    unintended locations.

    @param execTraceList: List of ExecTrace instances.
    @return : Map(dictionary) structure that has branch choices(brChoice) as
    a key and its correctness result as a value.
    """
    taintChannelMap = getTaintChannel(execTraceList)
    result = {}

    for brChoice in taintChannelMap:
        # Build a set for all Output locations
        allOutLocs = set()
        for _, outLoc in taintChannelMap[brChoice]:
            allOutLocs.add(outLoc)

        for inOutLoc in taintChannelMap[brChoice]:
            inLoc, outLoc = inOutLoc
            # outLoc_ := complement of outLoc
            outLoc_ = allOutLocs - outLoc

            for execTrc in execTraceList:
                # unexpected flow of taintedness -- false positive
                if execTrc.getInTagVal(inLoc) == execTrc.getOutTagVal(outLoc_):
                    if brChoice in result:
                        result[brChoice].append((execTrc, inOutLoc,
                                                 ERROR_DETECTED))
                    else:
                        result[brChoice] = [(execTrc, inOutLoc,
                                             ERROR_DETECTED)]
        else:
            if brChoice not in result:
                result[brChoice] = CORRECT_CHANNEL

    return result


def parseLines(lines_):
    """
    @param lines: lines to be parsed.
    @return: list of ExecTrace.
    """
    #remove comments
    lines = filter(lambda x: not x.strip().startswith("#"), lines_)
    it = iter(lines)

    line = it.next()
    try:
        while True:
            if ExecTrace.isEventIdLine(line):
                break
            it.next()
    except StopIteration:
        return []

    execTrcList = []
    buffer = []
    try:
        while True:
            if ExecTrace.isEventIdLine(line):
                if buffer:
                    execTrcList.append(buffer)
                buffer = []
            buffer.append(line)
            line = it.next()
    except StopIteration:
        execTrcList.append(buffer)
    return execTrcList


def handleBrNoise(eTrcList, brChoiceList_=None):
    """
    We assuemt that eTrcList contains instances of ExecTrace of the same
    inMap.

    @param eTraceList: list of ExecTrace instances that share the same
    inMapKey.
    @param brChoiceList_: previous inferred brChoice.
    @return: list of fixed branch choices which are common to all ExecTrace
    instances. It is 2 dimensional array containing per-thread branch choices.
    """
    brChoiceList = brChoiceList_
    for eTrc in eTrcList:
        if brChoiceList is None:
            brChoiceList = eTrc.getBrNumRepr()
        else:
            #print "DBG - len: ", map(lambda x: len(x), brChoiceList)
            tmp = eTrc.getBrNumRepr()

            # XXX: current implementation lacks in details for multi-threaded
            # BrChoiceList support.
            assert(len(brChoiceList) == len(tmp) and
                   "Sanity check:"
                   "for now we assume that every ExecTrace instance has the "
                   "same number threads")

            brTmp = []
            for i in range(len(tmp)):
                brTmp.append(LCS(brChoiceList[i], tmp[i]))
            else:
                brChoiceList = brTmp

    #make it immutable and return
    return tuplify(brChoiceList)


def handleOutNoise(eTraceList):
    """
    We assuemt that eTrcList contains instances of ExecTrace of the same
    inMap.

    @param eTraceList: list of ExecTrace instances that share the same
    inMapKey.
    @return:
    """
    outNumRepr = None
    for eTrc in eTraceList:
        if outNumRepr is None:
            outNumRepr = eTrc.getOutNumRepr()
        else:
            tmp = eTrc.getOutNumRepr()
            # XXX: current implementation lacks in correct support for
            # multi-threaded.
            assert(len(outNumRepr) == len(tmp) and
                   "Sanity check:"
                   "for now we assume that every ExecTrace instance has the "
                   "same number threads")

            outTmp = []
            for i in range(len(tmp)):
                outTmp.append(LCS(outNumRepr[i], tmp[i]))
            else:
                outNumRepr = outTmp
    return tuplify(outNumRepr)


def processOne(eTrcList, verbose=False):
    """
    ExecTrace grouping based on input values.
    """
    """
    eTrcHash = defaultdict(list)
    for e in eTrcList:
        if verbose:
            print e
        inKey = e.getInMapKey()
        eTrcHash[inKey].append(e)


    for inKey, eTrcList in eTrcHash.items():
    """
    inKey, brChoice, outLoc = getETrcSummary(eTrcList)

    if verbose:
        print "== Summary == "
        print "Inkey::", inKey, ": count - ", len(eTrcList), "\n", \
            BrLog.numReprToStr(brChoice)
        print OutLog.numReprToStr(outLoc)
    else:
        print inKey, ":", len(eTrcList), ":", brChoice, ":", outLoc


def processTwo(eTrcList0, eTrcList1, verbose=False):
    """
    XXX:
    """
    inKey0, brChoice0, outLoc0 = getETrcSummary(eTrcList0)
    inKey1, brChoice1, outLoc1 = getETrcSummary(eTrcList1)

    if inKey0 == inKey1:
        print >> sys.stderr, "Error: can't evaluate for the same inputs."
        return

    reduced = LCS2(brChoice0, brChoice1)
    if reduced not in (brChoice0, brChoice1):
        print >> sys.stderr, "Error: can't evaluate for the different branch"
        "choices."
        print "DBG:", brChoice0, brChoice1, reduced
        return

    if len(outLoc0) == len(outLoc1):
        for tId in range(len(outLoc0)):
            if len(outLoc0[tId]) == len(outLoc1[tId]):
                pass
            else:
                print >> sys.stderr, "Warning: different "
                "thread count {0}, {1}".format(len(outLoc0), len(outLoc1))
                return
    else:
        print >> sys.stderr, "Warning: different thread count {0}, {1}"\
            .format(len(outLoc0), len(outLoc1))
        return

    print "Going out"


def getETrcSummary(eTrcList, inKey=None):
    """
    XXX:
    """
    if inKey is None:
        inKey = eTrcList[0].getInMapKey()

    for eTrc in eTrcList:
        assert(inKey == eTrc.getInMapKey() and "sanity check")

    brChoice = handleBrNoise(eTrcList)
    outLoc = handleOutNoise(eTrcList)

    return inKey, brChoice, outLoc

"""
Evaluation main
"""
if __name__ == "__main__":

    argv = []
    verbose = False
    for fname in sys.argv[1:]:
        with file(fname) as f:
            lines = f.readlines()
        lineList = parseLines(lines)
        argv.append(map(lambda x: ExecTrace(x), lineList))
    else:
        argv.append(verbose)

    if len(sys.argv[1:]) == 1:
        processOne(*argv)
    elif len(sys.argv[1:]) == 2:
        processTwo(*argv)
