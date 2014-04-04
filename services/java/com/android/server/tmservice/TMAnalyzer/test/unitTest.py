#!/usr/bin/env python
import sys
import unittest

sys.path.append("../")
from ExecTrace import BrEntry, BrLog, OutEntry, OutLog, ExecTrace
from TMAnalyzer import parseLines, handleNoise
from ThreadMatch import SimpleMatcher, ExponentialMatcher, MatcherForMany, \
    print_tab, matrix


class TestBrLine(unittest.TestCase):
    basedir = "brLines/"

    def setUp(self):
        pass

    def testBrLine0(self):
        with file(self.basedir + "/" + "brLines0.txt") as f:
            lines = f.readlines()

        for line in lines:
            if line.strip():
                brLine = BrEntry(line)
                print brLine

    def tearDown(self):
        pass


class TestBrChoice(unittest.TestCase):
    basedir = "brChoices/"
    fnameList = ["brchoice0.txt"]

    def setUp(self):
        pass

    def testBrChoice0(self):
        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                print BrLog(lines)

    def tearDown(self):
        pass


class _TestOutEntry(unittest.TestCase):
    basedir = "outLines/"
    fnameList = ["outLine0.txt"]

    def setUp(self):
        pass

    def testEntry0(self):
        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                for line in lines:
                    print OutEntry(line)

    def tearDown(self):
        pass


class TestOutLog(unittest.TestCase):
    basedir = "outLocs/"
    fnameList = ["outLoc0.txt"]

    def setUp(self):
        pass

    def _testLog0(self):
        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                print OutLog(lines)

    def tearDown(self):
        pass


class TestExecTrace(unittest.TestCase):
    basedir = "eTraces/"
    fnameList = ["etrace0.txt"]
    tIdMapHash = {"etrace0.txt": {0: 1, 1: 12, 2: 13, 3: 18}}

    def setUp(self):
        pass

    def testLog0(self):
        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                eTrc = ExecTrace(lines)
                tIdMap = eTrc.getTIdMap()
                self.assertEqual(tIdMap, self.tIdMapHash["etrace0.txt"])

    def tearDown(self):
        pass


class TestThreadMatch(unittest.TestCase):
    basedir = "eTraces/"
    fnameList = ["tmatch0.txt"]

    def setUp(self):
        pass

    def testMatch0(self):
        br0 = [[6, 1, 3, 4, 25, 26, 28, 30, 38],
               [8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 11, 15, 17,
                19, 21, 23, 12, 13],
               [], [34, 36, 37, 34, 36, 37, 34, 36, 37, 35]]
        out0 = [[], [5], [4], [3, 7, 8]]

        br1 = [[1, 3, 4, 25, 26, 28, 30, 38, 6], [],
              [8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 11, 15, 17,
               19, 21, 23, 12, 13],
              [34, 36, 37, 34, 36, 37, 34, 36, 37, 35]]
        out1 = [[], [0], [5], [3, 7, 8]]

        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                linesList = parseLines(lines)

                self.assertEqual(len(linesList), 2)

                eTrc0 = ExecTrace(linesList[0])
                eTrc1 = ExecTrace(linesList[1])
                self.assertEqual(eTrc0.getBrNumRepr(), br0)
                self.assertEqual(eTrc0.getOutNumRepr(), out0)
                self.assertEqual(eTrc1.getBrNumRepr(), br1)
                self.assertEqual(eTrc1.getOutNumRepr(), out1)

                eTrcList0 = SimpleMatcher([eTrc0, eTrc1])
                eTrcList1 = SimpleMatcher([eTrc1, eTrc0])
                self.assertEqual(handleNoise(eTrcList0),
                                 handleNoise(eTrcList1))

                print eTrc0
                print eTrc1

                maxVal, mLst = ExponentialMatcher(*eTrcList0)
                self.assertEqual(maxVal, 42)
                self.assertEqual(mLst, [[(0, 0), (1, 2), (2, 1), (3, 3)]])

                eTrcList0_ = MatcherForMany(eTrcList0)
                for eTrc in eTrcList0_:
                    print eTrc

    def testMatch1(self):
        """
        """
        d_ = {(0, 0): 2, (0, 1): 8, (0, 2): 1, (0, 3): 3,
                (1, 0): 10, (1, 1): 2, (1, 2): 1, (1, 3): 3,
                (2, 0): 2, (2, 1): 1, (2, 2): 2, (2, 3): 2,
                (3, 0): 3, (3, 1): 0, (3, 2): 2, (3, 3): 2}
        d = matrix(d_, 4, 4)
        print print_tab(d)

    def tearDown(self):
        pass


if __name__ == "__main__":
    unittest.main()
