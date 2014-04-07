#!/usr/bin/env python
import itertools
import sys

from collections import defaultdict
from LCS import LCS


class matrix(dict):
    """
    dict type that has row and column elements to represent matrix object.
    """
    def __init__(*args, **kwargs):
        if len(args) == 3:  # [self, m, n]
            self, m, n = args
            dict.__init__(*[self], **kwargs)
            self.m = m
            self.n = n
        elif len(args) == 4:  # [self, dict, m, n]
            self, d, m, n = args
            dict.__init__(*[self, d], **kwargs)
            self.m = m
            self.n = n
        else:
            dict.__init__(*args, **kwargs)
            self = args[0]
            self.m = 0  # row count
            self.n = 0  # column count


def get_LCS_tab(eTrc0, eTrc1):
    """
    Build and return LCS table.
    """
    n = len(eTrc0.getTIdList())
    m = len(eTrc1.getTIdList())
    LCS_tab = matrix(n, m)

    #Setting LCS_Table
    for i in range(n):
        for j in range(m):
            LCS_tab[i, j] = LCS(eTrc0.getNumRepr(tId=i),
                                eTrc1.getNumRepr(tId=j),
                                False)
    return LCS_tab


def get_max_from_column(P, i, j):
    """
    @param P: matrix
    @param i: (i-th) column that we want to get the value from.
    @param j: the column to be excluded from the consideration.
    @return:
    """
    tmp = []
    for k in range(P.m):
        val, hist = P[i, k]
        tmp.append((val, hist))
    else:
        if tmp:
            tmp.sort(key=lambda x: x[0])
            return tmp[-1]
        else:
            return None


def ThreadMatcher(eTrc0_, eTrc1_):
    """
    Getting the thread combination that would maximize the maximum match.

    @param eTrc0_:
    @param eTrc1_:
    @return:
    """
    if len(eTrc0_.getTIdList()) <= (eTrc1_.getTIdList()):
        switched = False
        eTrc0 = eTrc0_
        eTrc1 = eTrc1_
    else:
        switched = True
        eTrc0 = eTrc1_
        eTrc1 = eTrc0_

    n = len(eTrc0.getTIdList())
    m = len(eTrc1.getTIdList())

    # n <= m.

    LCS_tab = matrix(n, m)
    P = matrix(n, m)

    LCS_tab = get_LCS_tab(eTrc0, eTrc1)

    print_tab(LCS_tab, n, m)

    # init the left-most column.
    for j in range(m):
        P[0, j] = LCS_tab[0, j], [j]
        print "P[0, {0}]:".format(j), P[0, j]

    print "Log:", n, m

    for i in range(1, n):
        for j in range(0, m):
            val, hist_ = get_max_from_column(i - 1, j)
            hist = hist_ + [j]
            print "DBG:1", hist_, j, "->", hist

            P[i, j] = val + LCS_tab[i, j], hist
            print "P[{0}, {1}]:".format(i, j), P[i, j]

    if switched:
        return
    else:
        return None


def print_tab(tab):
    for j in range(tab.n):
        print ""
        for i in range(tab.m):
            print "{:3d}".format(tab[i, j]),
    print ""


def SimpleMatcher(eTrcList):
    """
    """
    minTIdLen = sys.maxint
    for eTrc in eTrcList:
        tIdList = eTrc.getTIdList()
        minTIdLen = min(len(tIdList), minTIdLen)

    tIdMatchMap = {}
    for eTrc in eTrcList:
        for i in range(minTIdLen):
            tIdMatchMap[i] = tIdList[i]
        else:
            eTrc.tIdMatchMap = tIdMatchMap

    return eTrcList


def ExponentialMatcher(eTrc0_, eTrc1_):
    """
    """
    if len(eTrc0_.getTIdList()) <= (eTrc1_.getTIdList()):
        switched = False
        eTrc0 = eTrc0_
        eTrc1 = eTrc1_
    else:
        switched = True
        eTrc0 = eTrc1_
        eTrc1 = eTrc0_

    # n <= m.

    LCS_tab = get_LCS_tab(eTrc0, eTrc1)
    return ExponentialMatcherImpl(LCS_tab, switched)


def ExponentialMatcherImpl(LCS_tab, switched=False):
    assert(LCS_tab.m <= LCS_tab.n)
    it = itertools.permutations(range(LCS_tab.n), LCS_tab.m)
    retMap = defaultdict(list)
    try:
        while True:
            l = it.next()
            sum = 0
            match = []
            for i, j  in enumerate(l):
                sum += LCS_tab[i, j]
                if switched:
                    match.append((j, i))
                else:
                    match.append((i, j))
            else:
                retMap[sum].append(match)
    except StopIteration:
        # getting out of iteration.
        #print "iteration finished"
        pass

    if retMap:
        maxVal = sorted(retMap.keys())[-1]
        return maxVal, retMap[maxVal]
    return 0, None


def MatcherForMany(eTrcLst_, matcher=ExponentialMatcher):
    """
    """
    eTrcList = sorted(eTrcLst_, key=lambda x: len(x.getTIdList()))
    # eTrc0 to be the anchor ExecTrace.
    eTrc0 = eTrcList[0]
    l = len(eTrc0.getTIdList())
    for i in range(l):
        eTrc0.tIdMatchMap[i] = i
    for eTrc1 in eTrcList[1:]:
        _, mLst = matcher(eTrc0, eTrc1)
        #XXX: what we gonna do? if there's more than one?
        for m in mLst:
            for i, j in m:
                eTrc1.tIdMatchMap[j] = i

    return eTrcLst_

if __name__ == "__main__":
    pass
