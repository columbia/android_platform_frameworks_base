import sys
import getopt
from Util import tuplify, getDist
from ExecTrace import ExecTrace, BrLog, OutLog, OutEntry
from LCS import LCS2


CORRECT_CHANNEL = 1
ERROR_DETECTED = 1

#
# Utility methods
#


def parseLines(lines_):
    """
    @param lines_: lines to be parsed.
    @return: list of ExecTrace.
    """
    # Remove comments.
    lines__ = filter(lambda x: not x.strip().startswith("#"), lines_)

    # Remove empty line.
    lines = filter(lambda x: x.strip(), lines__)

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


def handleBrNoise(eTrcList, brChoiceList_=None, verbose=False):
    """
    We assuemt that eTrcList contains instances of ExecTrace of the same
    inMap.

    @param eTrcList: list of ExecTrace instances that share the same
    inMapKey.
    @param brChoiceList_: previous inferred brChoice.
    @return: list of fixed branch choices which are common to all ExecTrace
    instances(i.e., noise removed). It is 2 dimensional array containing
    per-thread branch choices.
    """
    brChoiceList = brChoiceList_
    for i, eTrc in enumerate(eTrcList):
        if brChoiceList is None:
            brChoiceList = eTrc.getBrNumRepr()
        else:
            tmp = eTrc.getBrNumRepr()

            # XXX: current implementation lacks in details for multi-threaded
            # BrChoiceList support -- checking for the thread count.
            assert(len(brChoiceList) == len(tmp) and
                   "Sanity check:"
                   "for now we assume that every ExecTrace instance has the "
                   "same number threads")

            print "Iteration {0}: Size: {1} - Dist {2}".\
                format(i, sum(map(lambda x: len(x), brChoiceList)),
                              getDist(brChoiceList, tmp))

            brChoiceList = LCS2(brChoiceList, tmp)
    #make it immutable and return
    return tuplify(brChoiceList)


def handleOutNoise(eTraceList):
    """
    This function remove noise from OutLogs leveraing LCS algorithm.

    @param eTraceList: list of ExecTrace instances that share the same
    inMapKey.
    @return: OutLog without noise in matrix representation.
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

            outNumRepr = LCS2(outNumRepr, tmp)
    return tuplify(outNumRepr)


def _handleNoiseImpl(eTrcList, inKey=None, verbose=False):
    """
    XXX:
    """
    if inKey is None:
        inKey = eTrcList[0].getInMapKey()

    for eTrc in eTrcList:
        assert(inKey == eTrc.getInMapKey() and "sanity check")

    brChoice = handleBrNoise(eTrcList, verbose=verbose)
    outLoc = handleOutNoise(eTrcList)

    return inKey, brChoice, outLoc


def handleNoise(eTrcList, verbose=False):
    """
    This function takes a list of ExecTrace instances with the same input
    and handles noises from both branch choices and output logs.

    @param eTrcList: list of ExecTrace instances that share the same
    inMapKey.
    """
    inKey, brChoice, outLoc = _handleNoiseImpl(eTrcList, verbose=False)

    if verbose:
        print "== Summary == "
        print "Inkey::", inKey, ": count - ", len(eTrcList), "\n", \
            BrLog.numReprToStr(brChoice)
        print OutLog.numReprToStr(outLoc)
    else:
        print inKey, ":", len(eTrcList), ":", brChoice, ":", outLoc


def evaluateChannel(eTrcList0, eTrcList1, verbose=False):
    """
    @param eTrcList0: list of ExecTrace instances that share the same
    inMapKey.
    @param eTrcList1: list of ExecTrace instances that share the same
    inMapKey, but different from that of eTrcList0.
   """
    inKey0, brChoice0, outLoc0 = _handleNoiseImpl(eTrcList0)
    inKey1, brChoice1, outLoc1 = _handleNoiseImpl(eTrcList1)

    if inKey0 == inKey1:
        print >> sys.stderr, "Error: can't evaluate for the same inputs."
        print >> sys.stderr, "Error: Going out..."
        return

    reduced = LCS2(brChoice0, brChoice1)
    if reduced not in (brChoice0, brChoice1):
        print >> sys.stderr, "Error: can't evaluate for the different branch"
        "choices."
        return

    if len(outLoc0) == len(outLoc1):
        for tId in range(len(outLoc0)):
            if len(outLoc0[tId]) == len(outLoc1[tId]):
                outList0 = outLoc0[tId]
                outList1 = outLoc1[tId]

                for i in range(len(outList0)):
                    outLoc0, outVal0, tagVal0 = \
                        OutEntry.outHashList[outList0[i]]
                    outLoc1, outVal1, tagVal1 = \
                        OutEntry.outHashList[outList1[i]]

                    if verbose:
                        print outLoc0, outVal0, tagVal0
                        print outLoc1, outVal1, tagVal1

                    # For the same output location,
                    if outLoc0 == outLoc1:
                        # Different values. i.e., Taint channel.
                        if outVal0 != outVal1:
                            # No taint tag seen from taint channel -- False
                            # Negative detected.
                            if (tagVal0 == 0) or (tagVal1 == 0):
                                print "{0} (TId: {1} seq: {2}): INCORRECT(FN)"\
                                    ": non-taint channel".\
                                    format(outLoc0, tId, i)
                            else:
                                print "{0} (TId: {1} seq: {2}): Correct: "\
                                    "taint channel".format(outLoc0, tId, i)
                        # Same values, i.e., Non-taint channel.
                        else:
                            if tagVal0 == tagVal1 == 0:
                                print "{0} (TId: {1} seq: {2}): Correct: "\
                                    "non-taint channel".format(outLoc0, tId, i)
                            else:
                                print "{0} (TId: {1} seq: {2}): INCORRECT(FP)"\
                                    " non-taint channel".\
                                    format(outLoc0, tId, i)
                    else:
                        print >> sys.stderr, "Warning: Irregular out log "\
                            "sequence {0} {1} {2} {4}".\
                            format(outLoc0, outLoc1, tId, i)

            else:
                print >> sys.stderr, "Error: different "
                "output log entries {0}, {1}".format(len(outLoc0),
                                                     len(outLoc1))
                print >> sys.stderr, "Error: Going out..."
                return
    else:
        print >> sys.stderr, "Error: different thread count {0}, {1}"\
            .format(len(outLoc0), len(outLoc1))
        print >> sys.stderr, "Error: Going out..."
        return


"""
Evaluation main
"""
help_message = '''
The help message goes here.
'''


class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

if __name__ == "__main__":
    argv = sys.argv
    verbose = False
    try:
        try:
            opts, args = getopt.getopt(argv[1:], "ho:v", ["help", "output="])
        except getopt.error, msg:
            raise Usage(msg)

        # option processing
        for option, value in opts:
            if option == "-v":
                verbose = True
            if option in ("-h", "--help"):
                raise Usage(help_message)
    except Usage, err:
        print >> sys.stderr, sys.argv[0].split("/")[-1] + ": " + str(err.msg)
        print >> sys.stderr, "\t for help use --help"
        sys.exit()

    fargv = []
    for fname in args:
        with file(fname) as f:
            lines = f.readlines()
        lineList = parseLines(lines)
        fargv.append(map(lambda x: ExecTrace(x), lineList))
    else:
        fargv.append(verbose)

    if verbose:
        for i, eTrc in enumerate(fargv[0]):
            print "*** Log {0} ***".format(i + 1)
            print eTrc

    if len(args) == 1:
        handleNoise(*fargv)
    elif len(args) == 2:
        evaluateChannel(*fargv)
    else:
        print >> sys.stderr, "Invalid usage: {0} input0 [input1]".\
            format(sys.argv[0])
