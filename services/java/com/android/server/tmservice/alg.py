import itertools
import re
import sys
from collections import defaultdict


CORRECT_CHANNEL = 1
ERROR_DETECTED = 1


def compareObj(objList, fieldList):
    """
    Utility/helper method.
    TODO:
    """
    def isUniformList(lst):
        """
        TODO:
        """
        i = lst[0]
        for i_ in lst[1:]:
            if i != i_:
                return False
            i = i_
        else:
            return True

    ret = True

    for field in fieldList:
        ret = ret and isUniformList(map(lambda x: x.getattribute__(field),
                                        objList))
    else:
        return ret


class BrLine(object):
    """
    TODO:
    """
    brChoiceMap = {'>': True, '<': False}

    def __init__(self, line):
        """
        """
        pBrLine = self.parseBrLine(line)

        self.pId = pBrLine[0]
        self.tmId = pBrLine[1]
        self.tId = pBrLine[2]
        self.offset = pBrLine[3]
        self.libPath = pBrLine[4]
        self.methodName = pBrLine[5]
        self.brType = pBrLine[6]
        self.brChoice = pBrLine[7]

    @classmethod
    def parseBrLine(cls, line):
        """
        TODO:
        """
        pId = ExecTrace.getPid(line)
        tmp = re.split(":|\|", line)

        tmId = int(tmp[1])
        tId = int(tmp[2])
        offset = int(tmp[3], 16)
        tmp_ = tmp[4].split(";")
        assert("Sanity check" and (len(tmp_) == 2))
        libPath = tmp_[0].trim()
        methodName = tmp_[1].trim()

        brType = tmp[6].trim()
        brChoice = cls.brChoiceMap[tmp[7]]

        return pId, tmId, tId, offset, libPath, \
            methodName, brType, brChoice

    def __eq__(self, other):
        """
        TODO:
        """
        compareList = ('libPath', 'methodName', 'offset', 'brType',
                        'brChoice')

        return compareObj((self, other), compareList)


class BrChoice(object):
    """
    TODO:
    """
    def __init__(self, lines):
        """
        Contructor method.
        """
        self.brTIdMap = {}
        brLineList = []
        for line in lines:
            brLine = BrLine(line)
            # Sanity check -- no duplicate map entry.
            brLineList.append(brLine)

        tIdSet = set(map(lambda x: x.tId, brLineList))
        for tId in tIdSet:
            self.brTIdMap[tId] = filter(lambda x: x.tId == tId, brLineList)
            self.brTIdMap[tId].sort(key=lambda x: x.tmId)  # Sort by tmId

    @classmethod
    def isBranchLine(cls, line):
        """0
        Static method to identify whether the line is branch choice log.

        @param cls:
        @return:
        """
        return ExecTrace.isBranchLine(line)

    def __eq__(self, other):
        """
        Equality evaluation.
        """
        # First, we compare keys
        keys = self.brTIdMap.keys()
        keys_ = other.brTIdMap.keys()

        if len(keys) == len(keys_):
            keys.sort()
            keys_.sort()

            for k, i in enumerate(keys):
                if k != keys[i]:
                    return False
        else:
            return False

        for k in keys:
            brList = self.brTIdMap[k]
            brList_ = other.brTIdMap[k]
            if len(brList) != len(brList_):
                return False

            for i, brChoice in enumerate(brList):
                if brChoice != brList_[i]:
                    return False
        else:
            return True


class OutLog(object):
    """
    TODO:
    """
    def __init__(self, lines):
        """
        Constructor method.
        """
        outEntList = []
        # key: (tId, outLoc), value: An instance of OutputEntry
        self.outEntTIdMap = defaultdict(list)

        for line in lines:
            outEntList.append(OutEntry(line))

        tIdSet = set(map(lambda x: x.tId, outEntList))
        for tId in tIdSet:
            tIdOutList = filter(lambda x: x.tId == tId, outEntList)

            for outEnt in tIdOutList:
                self.outEntTIdMap[(tId, outEnt.outLoc)].append(outEnt)

    def __eq__(self, other):
        """
        TODO:
        """
        return False


class OutEntry(object):
    """
    TODO:
    """
    def __init__(self, line):
        tmId, tId, outLoc, outputVal, tagVal = self.parseOutputLine(line)

        self.tId = tId
        self.tmId = tmId
        self.outLoc = outLoc
        self.outputVal = outputVal
        self.tagVal = tagVal

    @classmethod
    def parseOutputLine(self, line):
        """
        Data field is enclosed by '|{' ... '|}' and we have some lines to
        take care of it.
        """

        assert("sanity check" and self.isOutputLine(line))

        pat0 = r'\|{(.*)}\|'
        m = re.search(pat0, line)
        assert("search must find something." and m)
        outputVal = m.group(0)
        line_ = re.sub(pat0, '|---|', line)
        tmp = re.split(r":|\|", line_)

        tmId = int(tmp[2])
        tId = int(tmp[3])
        outLoc = tmp[1].trim()

        assert("sanity check" and tmp[4] == '---')

        tagVal = int(tmp[5], 16)

        return tmId, tId, outLoc, outputVal, tagVal

    @classmethod
    def isOutputLine(cls, line):
        """
        TODO:
        """
        return ExecTrace.isOutputLine(line)

    def __eq__(self, other):
        """
        Method for equality check.
        """
        compareList = ('outLoc', 'outputVal')
        return compareObj(compareList, (self, other))


class ExecTrace(object):
    """
    Class that represent a single execution instance.
    The class has members that represents
        i) Input locations / values
        ii) Branch choices
        iii) Output locations / values
    """

    outLocList = ['libcore.os.read0',
                      'libcore.os.read1',
                      'libcore.os.sendto0',
                      'libcore.os.pwrite0',
                      'libcore.os.pwrite1',
                      'libcore.os.write0',
                      'libcore.os.write1']

    def __init__(self, lines):
        """
        constructor method.
        @param lines: lines to be parsed.
        """
        assert("sanity check" and self.isEventIdLine(lines[0]))
        tmId, inLoc, inData, tag = self.parseEventIdLine(lines[0])

        brLogLines = []
        outputLogLines = []

        for line in line[1:]:
            if self.isOutputLine(line):
                outputLogLines.append(line)
            elif self.isBranchLine(line):
                brLogLines.append(line)
            else:
                assert("unexpected branch" and False)

        self.inputMap = {inLoc: inData}
        self.brChoice = BrChoice(brLogLines)
        self.output = OutLog(outputLogLines)

    def getInVal(self, inLoc):
        """
        @param inLoc: input location
        @return: input value witnessed from input location of inLoc
        """
        pass

    def getInTag(self, inLoc):
        """
        @param inLoc: input location
        @return: tag value witnessed from input location of inLoc
        """
        pass

    def getOutVal(self, outLoc):
        """
        @param outLoc: output location
        @return: output value witnessed from output location of outLoc
        """
        pass

    def getOutTag(self, outLoc):
        """
        @param outLoc: output location
        @return: tag value witnessed from output location of outLoc
        """
        pass

    @classmethod
    def parseEventIdLine(cls, line):
        """
        @param line:
        @return: (tmId, inLoc, inData, tag)
        """
        assert("sanity check" and cls.isEventIdLine(line))

        tmp = line.split("|")
        assert("sanity check" and len(tmp) == 5)

        tmid = int(tmp[1])
        inLoc = tmp[2]
        inData = tmp[3]
        tag = int(tmp[4], 16)
        return tmid, inLoc, inData, tag

    @classmethod
    def isEventIdLine(cls, line):
        """
        Static method to identify whether the line is an event identifier.
        @return: boolean
        """
        return line.startswith("W/TMLog") and ("runover" in line)

    @classmethod
    def isOutputLine(cls, line):
        """
        Static method to identify whether the line is output line.
        """
        if line.startswith("W/TMLog"):
            tmp_ = line.split(":")[1]
            tmp_.split("|")[0].trim()
            return (tmp_[0] in cls.outLocList) or \
                tmp_[0].startswith("libcore.os")
        else:
            return False

    @classmethod
    def isBranchLine(cls, line):
        """
        Static method to identify whether the line is branch choice log.

        @param cls:
        @return:
        """
        return line.startswith("E/dalvikvmtm")

    @classmethod
    def getPid(cls, line):
        """
        @param cls:
        @return:
        """
        if cls.isEventIdLine(line):
            pat0 = r"W/TMLog\s+\(\s*\d+\):"
        elif cls.isOutputLine(line):
            pat0 = r"W/TMLog\s+\(\s*\d+\):"
        elif cls.isBranchLine(line):
            pat0 = r"E/dalvikvmtm\(\s*\d+\):"
        else:
            pat0 = r""

        p = re.compile(pat0)
        m = p.match(line)
        if m:
            return int(m.group(2))
        else:
            return 0


def getAvailableInOutLocPairs(execTraceList):
    """
    @param execTraceList: list of ExecTrace instances.
    @return: list of all available input and output locations shown from
    execTraceList in the form of [(inLoc0, outLoc0), ...]
    """
    pass


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
    inOutLocList = getAvailableInOutLocPairs(execTraceList)

    taintChannelList = {}
    try:
        # Comparing for all available execution trace combination pairs
        while True:
            pair = it.next()
            execTrc0, execTrc1 = pair
            for inOutLoc in inOutLocList:
                inLoc, outLoc = inOutLoc

                # Checks whether output value responds to the changes of the
                # input values.

                if (execTrc0.getInValFor(inLoc) !=
                    execTrc1.getInValFor(inLoc)) \
                     and (execTrc0.getOutValFor(outLoc)
                          != execTrc1.getOutValFor(outLoc)):

                    # assert(execTrc0.brChoice == execTrc1.brChoice)
                    if execTrc0.brChoice in taintChannelList:
                        taintChannelList[execTrc0.brChoice].append(
                            (inLoc, outLoc))
                    else:
                        taintChannelList[execTrc0.brChoice] = [(inLoc, outLoc)]
                    break
            else:
                # inLoc, outLoc is not a Taint channel
                pass

    except StopIteration:
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


def parseLines(lines):
    """
    @param lines: lines to be parsed.
    @return: list of ExecTrace.
    """
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

"""
Evaluation main
"""
if __name__ == "__main__":
    fname = sys.argv[1]
    with file(fname) as f:
        lines = f.readlines()

    execTraceList = parseLines(lines)

    resultFN = EvalChannelFN(execTraceList)
    resultFP = EvalChannelFP(execTraceList)

    # For branch choice(brChoice) that we want to evaluate.
    brChoice = None
    if resultFN[brChoice] == resultFP[brChoice] == CORRECT_CHANNEL:
        print brChoice + " is CORRECT."
    else:
        print brChoice + " is INCORRECT."
