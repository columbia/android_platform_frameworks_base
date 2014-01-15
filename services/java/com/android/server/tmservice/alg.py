import itertools
import re
import sys
from collections import defaultdict
from StringIO import StringIO

CORRECT_CHANNEL = 1
ERROR_DETECTED = 1


def compareObjs(objList, fieldList):
    """
    Utility/helper method that compares member fields(from fieldList) from
    object instances from objList.

    @param objList: List that contains object instances to be tested.
    @param fieldList: List of fields (in string) to compare.
    @return: boolean

    """
    def isUniformIter(c):
        """
        Inner function to check whether the input argument @c contains
        identical entries.

        @param c: Input of collection type.

        """

        try:
            it = iter(c)
        except TypeError:
            return False

        try:
            i = it.next()

            while True:
                i_ = it.next()
                if i == i_:
                    return False
                i = i_
        except StopIteration:
            return True

    ret = True

    for field in fieldList:
        ret = ret and isUniformIter(map(lambda x: x.getattribute__(field),
                                        objList))
    else:
        return ret


class BrEntry(object):
    """
    Class that represent branch choice entry.
    """
    brChoiceMap = {'>': True, '<': False}

    def __init__(self, line, brLog=None):
        """
        Constructor method that calls parseBrLine() to parse input line.
        """
        pBrLine = self.parseBrLine(line)

        self.pId = pBrLine[0]
        self._tmId = pBrLine[1]
        self._tId = pBrLine[2]
        self.offset = pBrLine[3]
        self.libPath = pBrLine[4]
        self.methodName = pBrLine[5]
        self.brType = pBrLine[6]
        self.brChoice = pBrLine[7]
        self.brLog = brLog

        #Neutralization process.
        #if self.brLog and isinstance(self.brLog.nMap, dict):
        if self.brLog:
            self.nMap = brLog.nMap
            if "TmIdBase" in self.nMap:
                if self._tmId < self.nMap["TmIdBase"]:
                    self.nMap["TmIdBase"] = self._tmId
            else:
                self.nMap = {"TmIdBase": self._tmId, "TIdMap": defaultdict(int)}
        else:
            self.nMap = {"TmIdBase": self._tmId, "TIdMap": defaultdict(int)}

        self.nMap["TIdMap"][self._tId] += 1

    def _getTmId(self):
        """
        Getter method for tmId.
        """
        return self._tmId - self.nMap["TmIdBase"]

    def _setTmId(self, tmId):
        """
        Setter method for tmId.
        """
        if tmId < self.nMap["TmIdBase"]:
            self.nMap["TmIdBase"] = tmId
        self._tmId = tmId

    def _getTId(self):
        """
        Getter method for tId.
        """
        assert(self._tId in self.nMap["TIdMap"])
        TIdList = self.nMap["TIdMap"].keys()
        TIdList.sort()

        return TIdList.index(self._tId)

    def _setTId(self, tId):
        """
        Setter method for tId.
        """
        self.nMap["TIdMap"][tId] += 1
        self._tId = tId

    # Setter/getter.
    tmId = property(_getTmId, _setTmId)
    tId = property(_getTId, _setTId)

    @classmethod
    def parseBrLine(cls, line):
        """
        Static method that parse input line.
        """
        pId = ExecTrace.getPid(line)
        tmp = re.split(":|\|", line)

        tmId = int(tmp[1])
        tId = int(tmp[2])
        offset = int(tmp[3], 16)
        tmp_ = tmp[4].split(";")
        assert("Sanity check" and (len(tmp_) == 2))
        libPath = tmp_[0].strip()
        methodName = tmp_[1].strip()

        brType = tmp[6].strip()
        brChoice = cls.brChoiceMap[tmp[7].strip()]

        return pId, tmId, tId, offset, libPath, \
            methodName, brType, brChoice

    def __eq__(self, other):
        """
        Method for equality evaluation.
        """
        compareList = ('libPath', 'methodName', 'offset', 'brType',
                        'brChoice')

        return compareObjs((self, other), compareList)

    def __str__(self):
        """
        Method for string output.
        """
        ret = "BR::{0} ({1}) :: {2} :: {3} @ {4} :: {5}".\
            format(self.pId, self.tId, self.tmId,
                   self.libPath + " " + self.methodName, self.offset,
                   self.brChoice)

        return ret


class BrLog(object):
    """
    Class that represent list of branch choice (possibly from different
    threads.)
    """
    def __init__(self, lines, execTrace=None):
        """
        Contructor method.
        """
        self.brTIdMap = {}
        brEntList = []

        self.execTrace = execTrace
        if self.execTrace:
            self.nMap = execTrace.nMap
        else:
            self.nMap = {"TmIdBase": sys.maxint + 1, "TIdMap": defaultdict(int)}

        for line in lines:
            brEnt = BrEntry(line, self)
            # Sanity check -- no duplicate map entry.
            brEntList.append(brEnt)

        tIdSet = set(map(lambda x: x.tId, brEntList))
        for tId in tIdSet:
            self.brTIdMap[tId] = filter(lambda x: x.tId == tId, brEntList)
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

    def __str__(self):
        output = StringIO()

        for tId in self.brTIdMap:
            print >> output, "== TID {0} ==".format(tId)

            for brEnt in self.brTIdMap[tId]:
                print >> output, "\t{0}".format(brEnt)

        return output.getvalue()


class OutLog(object):
    """
    TODO:
    """
    def __init__(self, lines, execTrace=None):
        """
        Constructor method.
        """
        self.execTrace = execTrace
        if self.execTrace:
            self.nMap = execTrace.nMap
        else:
            self.nMap = {"TmIdBase": sys.maxint + 1, "TIdMap": defaultdict(int)}

        outEntList = []
        # key: (tId, outLoc), value: An instance of OutputEntry
        self.outEntTIdMap = defaultdict(list)

        for line in lines:
            outEntList.append(OutEntry(line, self))

        tIdSet = set(map(lambda x: x.tId, outEntList))
        for tId in tIdSet:
            tIdOutList = filter(lambda x: x.tId == tId, outEntList)

            for outEnt in tIdOutList:
                self.outEntTIdMap[(tId, outEnt.outLoc)].append(outEnt)

        for entId in self.outEntTIdMap:
            self.outEntTIdMap[entId].sort(key=lambda x: x.tmId)


    def getOutLocListbyTId(self, tId):
        """
        @param tId:
        @return:
        """
        tIdOutLocList = []
        for entId in self.outEntTIdMap:
            if entId[0] == tId:
                tIdOutLocList += self.outEntTIdMap[entId]
        tIdOutLocList.sort(key=lambda x: x.tmId)

        return tIdOutLocList

    def getTIdList(self):
        """
        """
        tIdSet = set()
        for entId in self.outEntTIdMap:
            tIdSet.add(entId[0])
        else:
            tIdLst = list(tIdSet)
            tIdLst.sort()
            return tIdLst

    def getMatrix(self):
        """
        """
        pass


    def getGraph(self):
        """
        """

    def __eq__(self, other):
        """
        TODO:
        """
        if len(self.outEntTIdMap) != other.outEntTIMap:
            return False

    def __str__(self):
        """
        Method for string output.
        """
        output = StringIO()
        tIdList = self.getTIdList()

        for tId in tIdList:
            print >> output, "== {0} ==".format(tId)
            outEntList = self.getOutLocListbyTId(tId)
            outLocMap = defaultdict(list)
            for outEnt in outEntList:
                outLocMap[outEnt.outLoc].append(outEnt)
            else:
                outLocMap[outEnt.outLoc].sort(key=lambda x: x.tmId)

            outLocList = outLocMap.keys()
            outLocList.sort()

            for outLoc in outLocList:
                print >> output, "\t* {0} *".format(outLoc)
                for outEnt in outLocMap[outLoc]:
                    print >> output, "\t\t" + str(outEnt)

        return output.getvalue()


class OutEntry(object):
    """
    TODO:
    """
    def __init__(self, line, outLog=None):
        pId, tmId, tId, outLoc, outputVal, tagVal = self.parseOutputLine(line)

        self.pId = pId
        self._tId = tId
        self._tmId = tmId
        self.outLoc = outLoc
        self.outputVal = outputVal
        self.tagVal = tagVal
        self.outLog = outLog

        #Neutralization process.
        if self.outLog and isinstance(self.outLog.nMap, dict):
            self.nMap = outLog.nMap
            if "TmIdBase" in self.nMap:
                if self._tmId < self.nMap["TmIdBase"]:
                    self.nMap["TmIdBase"] = self._tmId
            else:
                self.nMap = {"TmIdBase": self._tmId, "TIdMap": defaultdict(int)}
        else:
            self.nMap = {"TmIdBase": self._tmId, "TIdMap": defaultdict(int)}

        self.nMap["TIdMap"][self._tId] += 1

    def _getTmId(self):
        """
        Getter method for tmId.
        """
        return self._tmId - self.nMap["TmIdBase"]

    def _setTmId(self, tmId):
        """
        Setter method for tmId.
        """
        if tmId < self.nMap["TmIdBase"]:
            self.nMap["TmIdBase"] = tmId
        self._tmId = tmId

    def _getTId(self):
        """
        Getter method for tId.
        """
        assert(self._tId in self.nMap["TIdMap"])
        TIdList = self.nMap["TIdMap"].keys()
        TIdList.sort()

        return TIdList.index(self._tId)

    def _setTId(self, tId):
        """
        Setter method for tId.
        """
        self.nMap["TIdMap"][tId] += 1
        self._tId = tId

    # Setter/getter
    tmId = property(_getTmId, _setTmId)
    tId = property(_getTId, _setTId)

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

        pId = ExecTrace.getPid(line)
        tmId = int(tmp[2])
        tId = int(tmp[3])
        outLoc = tmp[1].strip()

        assert("sanity check" and tmp[4] == '---')

        tagVal = int(tmp[5], 16)

        return pId, tmId, tId, outLoc, outputVal, tagVal

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
        return compareObjs(compareList, (self, other))

    def __str__(self):
        """
        """
        ret = "{0} ({1}):: {2}:: {3}@{4} :: {5}".\
            format(self.pId, self.tId, self.tmId, self.outLoc, self.outputVal,
                   self.tagVal)
        return ret


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

        # Neutralization map.
        self.nMap = {"TmIdBase": sys.maxint + 1, "TIdMap": defaultdict(int)}

        for line in line[1:]:
            if self.isOutputLine(line):
                outputLogLines.append(line)
            elif self.isBranchLine(line):
                brLogLines.append(line)
            else:
                assert("unexpected branch" and False)

        self.inputMap = {inLoc: inData}
        self.brChoice = BrLog(brLogLines, self)
        self.output = OutLog(outputLogLines, self)

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
    def isOutputLine(cls, line_):
        """
        Static method to identify whether the line is output line.
        """
        line = line_.strip()
        if line.startswith("W/TMLog"):
            tmp = line.split(":")[1].strip()
            tmp_ = tmp.split("|")[0].strip()
            return (tmp_ in cls.outLocList) or \
                tmp_.startswith("libcore.os")
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
            pat0 = r"W/TMLog\s+\(\s*(\d+)\):"
        elif cls.isOutputLine(line):
            pat0 = r"W/TMLog\s+\(\s*(\d+)\):"
        elif cls.isBranchLine(line):
            pat0 = r"E/dalvikvmtm\(\s*(\d+)\):"
        else:
            pat0 = r""

        p = re.compile(pat0)
        m = p.match(line)
        if m:
            return int(m.group(1))
        else:
            return 0

    def __str__(self):
        """
        TODO:
        """
        return ""


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
