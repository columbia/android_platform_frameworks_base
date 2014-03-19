import sys
import bisect
import re
from Util import compareObjs
from StringIO import StringIO
from collections import defaultdict


class BrEntry(object):
    """
    Class that represent a branch choice entry.
    """
    brChoiceMap = {'>': True, '<': False}
    brHashList = []

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
                self.nMap = {"TmIdBase": self._tmId,
                                "TIdMap": defaultdict(int)}
        else:
            self.nMap = {"TmIdBase": self._tmId, "TIdMap": defaultdict(int)}

        self.nMap["TIdMap"][self._tId] += 1

        self.hkey = (self.libPath, self.methodName, self.offset)
        self.updateHashList((self.libPath, self.methodName, self.offset))

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
        # XXX: not optimal to sort() every time. Fix it.
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

    @classmethod
    def numReprToStr(cls, brEnt):
        output = StringIO()
        libPath, methodName, offset = cls.brHashList[brEnt / 2]
        brChoice = brEnt % 2
        print >> output, "{0} {1} {2} {3}".format(libPath, methodName, offset,
                                                  brChoice)
        ret = output.getvalue()
        output.close()
        return ret

    def getNumRepr(self):
        assert(self.hkey in self.brHashList and "sanity check")

        return self.brHashList.index(self.hkey) * 2 + self.brChoice

    def updateHashList(self, hkey):
        """
        @param hkey: Tuple composed of library name, method name, and offset.
        This is added to cls.brHashList in sorted order.
        """
        if self.hkey not in self.brHashList:
            bisect.insort(self.brHashList, self.hkey)

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
    Class that represent list of branch choice (from different
    threads.)
    """
    def __init__(self, lines, execTrace=None):
        """
        Contructor method.
        """
        self.brTIdMap = {}
        brEntList = []

        self.execTrace = execTrace

        #init. nuetralization map.
        if self.execTrace:
            self.nMap = execTrace.nMap
        else:
            self.nMap = {"TmIdBase": sys.maxint + 1,
                            "TIdMap": defaultdict(int)}

        for line in lines:
            brEnt = BrEntry(line, self)
            # Sanity check -- no duplicate map entry.
            brEntList.append(brEnt)

        tIdSet = set(map(lambda x: x.tId, brEntList))
        for tId in tIdSet:
            self.brTIdMap[tId] = filter(lambda x: x.tId == tId, brEntList)
            self.brTIdMap[tId].sort(key=lambda x: x.tmId)  # Sort by tmId

    def getNumRepr(self, tId=None):
        """
        @param tId: if tId is set to a specific value, return 1-dimensional
        array that represents the event sequence for thread id, otherwise
        returns 2-dimensional array for all threads.

        @return: numeric representation of branch choices.
        """
        keyLst = range(len(self.nMap["TIdMap"]))

        if tId is None:
            ret = [[] for _ in keyLst]
            for tId_ in keyLst:
                if tId_ in self.brTIdMap:
                    ret[tId_] = map(lambda x: x.getNumRepr(),
                                    self.brTIdMap[tId_])
            return ret
        else:
            if tId in keyLst:
                return map(lambda x: x.getNumRepr(), self.brTIdMap[tId])
            else:
                return []

    @classmethod
    def numReprToStr(cls, numRepr):
        """
        """
        output = StringIO()
        for tId, brList in enumerate(numRepr):
            print >> output, "== TId({0}) ==".format(tId)
            for brEnt in brList:
                print >> output, "\t", BrEntry.numReprToStr(brEnt),

        ret = output.getvalue()
        output.close()
        return ret

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
            print >> output, "== BR (TID {0}) ==".format(tId)

            for brEnt in self.brTIdMap[tId]:
                print >> output, "\t{0}".format(brEnt)

        return output.getvalue()

    def getTIdList(self):
        """
        @return: a list of thread IDs in branch choices.
        """
        return range(len(self.nMap["TIdMap"]))


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
            self.nMap = {"TmIdBase": sys.maxint + 1,
                            "TIdMap": defaultdict(int)}

        outEntList = []
        # key:tId, value: An instance of OutputEntry
        self.outEntTIdMap = defaultdict(list)

        for line in lines:
            outEntList.append(OutEntry(line, self))

        tIdSet = set(map(lambda x: x.tId, outEntList))
        for tId in tIdSet:
            tIdOutList = filter(lambda x: x.tId == tId, outEntList)
            tIdOutList.sort(key=lambda x: x.tmId)
            self.outEntTIdMap[tId] = tIdOutList

    def getOutLocListbyTId(self, tId=None):
        """
        @param tId: thread Id or None
        @return: if tId is None returns a list that contains all OutEntry
        instances otherwise returns OutEntry instances with TId
        """
        if tId is None:
            tIdOutLocList = []
            for tId_ in self.outEntTIdMap:
                tIdOutLocList += self.outEntTIdMap[tId_]
            tIdOutLocList.sort(key=lambda x: x.tmId)
            return tIdOutLocList
        else:
            return self.outEntTIdMap[tId]

    def getDistOutLocList(self):
        """
        return: List of distinct output locations appeared from OutLog.
        """
        ret = set()
        for outEntList in self.outEntTIdMap.values():
            tmpList = map(lambda x: x.outLoc, outEntList)
            ret.union(tmpList)
        ret_ = list(ret)
        ret_.sort()
        return ret_

    def getTIdList(self):
        """
        TODO:
        """
        return range(len(self.nMap["TIdMap"]))

    def __eq__(self, other):
        """
        TODO:
        """
        return self.getNumExpr() == other.getNumExpr()

    def __getOutXListForImpl(self, X, outLoc, tId=None):
        """
        helper method.
        return: List
        """
        assert("OutLoc sanity check" and outLoc in ExecTrace.outLocList)

        ret = self.getOutLocListByTId(self, tId)
        ret_ = map(X, filter(lambda x: x.outLoc == outLoc, ret))
        return ret_

    def getOutValListFor(self, outLoc, tId=None):
        return self.__getOutXListForImpl(lambda x: x.outputVal, outLoc, tId)

    def getOutTagListFor(self, outLoc, tId=None):
        return self.__getOutXListForImpl(lambda x: x.tagVal, outLoc, tId)

    @classmethod
    def numReprToStr(self, numRepr):
        """
        """
        output = StringIO()
        for tId, outList in enumerate(numRepr):
            print >> output, "== OutList - {0} ==".format(tId)
            for outEnt in outList:
                print >> output, "\t", OutEntry.numReprToStr(outEnt),

        ret = output.getvalue()
        output.close()
        return ret

    def getNumRepr(self, tId=None):
        """
        Return numeric representation of branch choices.
        """
        keyLst = range(len(self.nMap["TIdMap"]))
        if tId is None:
            ret = [[] for _ in keyLst]
            for tId_, val in self.outEntTIdMap.items():
                ret[tId_] = map(lambda x: x.getNumRepr(), val)
            return ret
        else:
            if tId in keyLst:
                return map(lambda x: x.getNumRepr(), self.outEntTIdMap[tId])
            else:
                return []

    def __str__(self):
        """
        Method for string output.
        """
        output = StringIO()
        tIdList = self.getTIdList()

        for tId in tIdList:
            print >> output, "== OutLog (TID {0}) ==".format(tId)
            outEntList = self.getOutLocListbyTId(tId)

            #no outEnt for tId, just continue.
            if not outEntList:
                continue

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
    outHashList = []

    def __init__(self, line, outLog=None):
        pId, tmId, tId, outLoc, outputVal, tagVal = self.parseOutputLine(line)

        self.pId = pId
        self._tId = tId
        self._tmId = tmId
        self.outLoc = outLoc
        self.outputVal = outputVal
        self._tagVal = tagVal
        self.outLog = outLog

        #Neutralization process.
        if self.outLog and isinstance(self.outLog.nMap, dict):
            self.nMap = outLog.nMap
            if "TmIdBase" in self.nMap:
                if self._tmId < self.nMap["TmIdBase"]:
                    self.nMap["TmIdBase"] = self._tmId
            else:
                self.nMap = {"TmIdBase": self._tmId,
                                "TIdMap": defaultdict(int)}
        else:
            self.nMap = {"TmIdBase": self._tmId, "TIdMap": defaultdict(int)}

        self.nMap["TIdMap"][self._tId] += 1

        self.hkey = (self.outLoc, self.outputVal, self.tagVal)
        self.updateHashList()

    def _getTagVal(self):
        if self.outLog is not None and self.outLog.execTrace is not None:
            inputMap = self.outLog.execTrace.inputMap
            inList = sorted(inputMap.items(), key=lambda x: x[0])

            # not tainted return 0.
            if self._tagVal == 0:
                return 0

            for i in range(len(inList)):
                if self._tagVal == inList[i][1][1]:
                    return i + 1
            else:
                # tainted with some unexpected value. Return 0
                return -1
        else:
            return self._tagVal

    def _setTagVal(self, tagVal):
        self._tagVal = tagVal

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
    tagVal = property(_getTagVal, _setTagVal)

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

    @classmethod
    def numReprToStr(cls, outEnt):
        output = StringIO()
        outLoc, outputVal, tag = cls.outHashList[outEnt]
        print >> output, "{0} {1} {2}".format(outLoc, outputVal, tag)
        ret = output.getvalue()
        output.close()
        return ret

    def updateHashList(self):
        """
        """
        if self.hkey not in self.outHashList:
            bisect.insort(self.outHashList, self.hkey)

    def getNumRepr(self):
        assert(self.hkey in self.outHashList and "sanity check")

        return self.outHashList.index(self.hkey)

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

        # init. Neutralization map.
        tIdSet, initTMId = self._initNMap(lines)
        self.nMap["TmIdBase"] = initTMId
        for tId in tIdSet:
            self.nMap["TIdMap"][tId] = 0

        # NOTE: tmId here is an event identifier that is different from that
        # of BrLog or OutLog

        self.tmId = tmId

        for line in lines[1:]:
            if self.isOutputLine(line):
                outputLogLines.append(line)
            elif self.isBranchLine(line):
                brLogLines.append(line)
            else:
                print line
                assert("unexpected branch" and False)

        self.inputMap = {inLoc: (inData.strip(), tag)}
        self.brChoice = BrLog(brLogLines, self)
        self.outLog = OutLog(outputLogLines, self)

        self.tIdMatchMap = None

    @classmethod
    def _initNMap(cls, lines):
        """
        @param lines:
        @return: TIdSet, tmId
        """
        tIdSet = set()
        tmId = sys.maxint + 1

        for line in lines:
            tId = cls.parseTId(line)
            if tId is not None:
                tIdSet.add(tId)
            tmId_ = cls.parseTMId(line)
            if tmId_ is not None:
                tmId = min(tmId_, tmId)
        return tIdSet, tmId

    @classmethod
    def parseTId(cls, line):
        if cls.isOutputLine(line):
            tId = OutEntry.parseOutputLine(line)[2]
        elif cls.isBranchLine(line):
            tId = BrEntry.parseBrLine(line)[2]
        else:
            return None
        return tId

    @classmethod
    def parseTMId(cls, line):
        """
        """
        if cls.isOutputLine(line):
            tmId = OutEntry.parseOutputLine(line)[1]
        elif cls.isBranchLine(line):
            tmId = BrEntry.parseBrLine(line)[1]
        else:
            return None
        return tmId

    def getInMapKey(self):
        tmp = []
        for key, val in self.inputMap.items():
            tmp.append((key, val[0]))
        return tuple(tmp)

    def getInValFor(self, inLoc):
        """
        @param inLoc: input location.
        @return: input value witnessed from input location of inLoc.
        """
        if inLoc in self.inputMap:
            return self.inputMap[inLoc][0]
        else:
            return None

    def getInTagFor(self, inLoc):
        """
        @param inLoc: input location.
        @return: tag value in integer witnessed from input location of inLoc.
        """
        if inLoc in self.inputMap:
            return int(self.inputMap[inLoc][1], 16)
        else:
            #XXX: Should we just return '0' here?
            return None

    def getOutValListFor(self, outLoc, tId):
        """
        @param tId:
        @param outLoc: output location
        @return: output value witnessed from output location of outLoc
        """
        self.outLog.getOutValListFor(outLoc, tId)

    def getOutTagListFor(self, tId, outLoc):
        """
        @param tId:
        @param outLoc: output location
        @return: tag value witnessed from output location of outLoc
        """
        self.outLog.getOutTaglListFor(outLoc, tId)

    @classmethod
    def parseEventIdLine(cls, line):
        """
        @param line:
        @return: (tmId, inLoc, inData, tag)
        """
        assert("sanity check" and cls.isEventIdLine(line))

        tmp_ = line.split("|")
        # eliminating empty entries.
        tmp = filter(lambda x: x.strip(), tmp_)
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

    def getBrNumRepr(self, tId=None):
        """
        """
        brNumRepr = self.brChoice.getNumRepr(tId=tId)

        if self.tIdMatchMap:
            retRepr = [[] for _ in range(len(self.tIdMatchMap))]
            for newTId, oldTId in self.tIdMatchMap.items():
                print "DBG0:", self.tIdMatchMap
                print "DBG1:", newTId, retRepr
                print "DBG2:", oldTId, brNumRepr

                retRepr[newTId] = brNumRepr[oldTId]

            #assert(filter(lambda x: not x, retRepr) == [] and
            #       "Sanity check -- not expecting to see any remaining []")

            return retRepr
        else:
            return brNumRepr

    def getOutNumRepr(self, tId=None):
        """
        """
        outNumRepr = self.outLog.getNumRepr()
        if self.tIdMatchMap:
            retRepr = [[] for _ in range(len(self.tIdMatchMap))]
            for newTId, oldTId in self.tIdMatchMap.items():
                retRepr[newTId] = outNumRepr[oldTId]

            #assert(filter(lambda x: not x, retRepr) == [] and
            #       "Sanity check -- not expecting to see any remaining []")

            return retRepr
        else:
            return outNumRepr

    def getNumRepr(self, tId=None):
        """
        FIXME: now, it only consider BrChoice as ExecTrace's signature.
        we need a way to combine BrChoice and OutLoc into a single
        representation.
        """
        return self.getBrNumRepr(tId=tId)

    def _getInMapStr(self):
        """
        @return: String representation of input value (self.inputMap).
        """
        ret = ""
        output = StringIO()
        for i, inKey in enumerate(self.inputMap):
            val, tag = self.inputMap[inKey]
            if tag > 0:
                tag_ = i + 1
            print >> output, "Input Loc - {0}:: Value ({1}), Tag ({2})".\
                format(inKey, val, tag_),
        ret = output.getvalue()
        output.close()
        return ret

    def getTIdList(self):
        """
        @return: Number of threads in the execution trace.
        """
        #return sorted(self.nMap["TIdMap"].keys())
        return range(len(self.nMap["TIdMap"].keys()))

    def getTIdMap(self):
        """
        """
        tIdList = self.getTIdList()
        tIdMap = {}
        for i, tId in enumerate(tIdList):
            tIdMap[i] = tId
        return tIdMap

    def __str__(self):
        """
        """
        ret = ""
        output = StringIO()
        print >> output, "== Input =="
        print >> output, self._getInMapStr()
        print >> output, self.brChoice
        print >> output, self.outLog
        ret = output.getvalue()
        output.close()
        return ret
