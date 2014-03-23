#!/usr/bin/env python
import sys

from LCS import LCS


def ThreadMatcher(eTrc0_, eTrc1_):
    """
    Getting the thread combination that would maximize the maximum match.

    @param eTrc0_:
    @param eTrc1_:
    @return:
    """
    if len(eTrc0_.getTIdList()) >= (eTrc1_.getTIdList()):
        switched = False
        eTrc0 = eTrc0_
        eTrc1 = eTrc1_
    else:
        switched = True
        eTrc0 = eTrc1_
        eTrc1 = eTrc0_

    n = len(eTrc0.getTIdList())
    m = len(eTrc1.getTIdList())

    # n >= m.

    LCS_tab = {}
    P = {}

    def get_max_from_P(j, i):
        """
        Inner function.

        @param i:
        @param j:
        @return:
        """
        print "DBG:", j, i
        tmp = []
        for k in range(m):
            val, hist = P[j, k]
            if j + 1 not in hist:
                tmp.append((val, hist))
        else:
            tmp.sort(key=lambda x: x[0])
            print "DBG:xx:", tmp, j, i
            return tmp[-1]

    #Setting LCS_Table
    for i in range(n):
        for j in range(m):
            LCS_tab[i, j] = LCS(eTrc0.getNumRepr(tId=i),
                                eTrc1.getNumRepr(tId=j), False)

    print_tab(LCS_tab, n, m)

    # init the left-most column.
    for j in range(m):
        P[0, j] = LCS_tab[0, j], [j]
        print "P[0, {0}]:".format(j), P[0, j]

    print "Log:", n, m

    for i in range(1, n):
        for j in range(0, m):
            val, hist_ = get_max_from_P(i - 1, j)
            hist = hist_ + [j]
            print "DBG:1", hist_, j, "->", hist

            P[i, j] = val + LCS_tab[i, j], hist
            print "P[{0}, {1}]:".format(i, j), P[i, j]

    if switched:
        return
    else:
        return None


def print_tab(tab, n, m):
    for i in range(n):
        print ""
        for j in range(m):
            print tab[i, j],
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

if __name__ == "__main__":
    pass
