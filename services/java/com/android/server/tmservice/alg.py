import itertools


class ExecTrace(ojbect):
    """
    Class that represent a single execution instance.
    The class has members that represents
        i) Input locations / values
        ii) Branch choices
        iii) Output locations / values
    """
    def __init__(self, input, brChoice, output):
        """
        constructor method
        """
        self.input = input
        self.brChoice = brChoice
        self.output = output

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

                if execTrc0.getInValFor(inLoc) != execTrc1.getInValFor(inLoc) and \
                        execTrc0.getOutValFor(outLoc) != execTrc1.getOutValFor(outLoc):

                    # assert(execTrc0.brChoice == execTrc1.brChoice)
                    if execTrc0.brChoice in taintChannelList:
                        taintChannelList[execTrc0.brChoice].append(
                            (inLoc, outLoc))
                    else:
                        taintChannelList[execTrc0.brChoice] = [(inLoc, outLoc)]
                    break
            else:
                #inLoc, outLoc is not a Taint channel
                pass

    except StopIteration:
        return taintChannelList


def EvaluateChannelFN(execTraceList):
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
                        result[brChoice].append((execTrc, inOutLoc, ERROR_DETECTED))
                    else:
                        result[brChoice] = [(execTrc, inOutLoc, ERROR_DETECTED)]

        # Iterated over all input, output locations for brChoice
        else:
            if brChoice not in result:
                result[brChoice] = CORRECT_CHANNEL

    return result


def EvaluateChannelFP(execTraceList):
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
                #unexpected flow of taintedness -- false positive
                if execTrc.getInTagVal(inLoc) == execTrc.getOutTagVal(outLoc_):
                    if brChoice in result:
                        result[brChoice].append((execTrc, inOutLoc, ERROR_DETECTED))
                    else:
                        result[brChoice] = [(execTrc, inOutLoc, ERROR_DETECTED)]
        else:
            if brChoice not in result:
                result[brChoice] = CORRECT_CHANNEL

    return result


"""
Evaluation main
"""
if __name__ == "__main__":
    resultFN = EvaluateChannelFN(execTraceList)
    resultFP = EvaluateChannelFP(execTraceList)

    # For branch choice(brChoice) that we want to evaluate.
    if resultFN[brChoice] == resultFP[brChoice] == CORRECT_CHANNEL:
        print  brChoice + " is CORRECT."
    else:
        print brChoice + " is INCORRECT."

