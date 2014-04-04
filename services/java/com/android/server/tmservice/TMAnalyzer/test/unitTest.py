#!/usr/bin/env python
import sys
import unittest
import StringIO

sys.path.append("../")
import ExecTrace
from TMAnalyzer import parseLines, handleNoise
from ThreadMatch import SimpleMatcher, ExponentialMatcher, \
    ExponentialMatcherImpl, MatcherForMany, print_tab, matrix


class TestBrLine(unittest.TestCase):
    basedir = "brLines/"

    def setUp(self):
        reload(ExecTrace)

    def testBrLine0(self):
        with file(self.basedir + "/" + "brLines0.in") as f:
            lines = f.readlines()

        with file(self.basedir + "/" + "brLines0.out") as f:
            out0 = f.read()
        with file(self.basedir + "/" + "brLogs0.out") as f:
            out1 = f.read()

        buf0 = StringIO.StringIO()
        buf1 = StringIO.StringIO()

        for line in lines:
            if line.strip():
                brLine = ExecTrace.BrEntry(line)
                print >> buf0, brLine

        brLog = ExecTrace.BrLog(lines)
        print >> buf1, brLog

        print buf1.getvalue()

        self.assertEqual(out0, buf0.getvalue())
        self.assertEqual(out1, buf1.getvalue())

    def tearDown(self):
        pass


class TestOutEntry(unittest.TestCase):
    basedir = "outLines/"
    fnameList = ["outLine0.txt"]

    def setUp(self):
        reload(ExecTrace)

    def _testEntry0(self):
        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                for line in lines:
                    print ExecTrace.OutEntry(line)

    def tearDown(self):
        pass


class TestOutLog(unittest.TestCase):
    basedir = "outLocs/"
    fnameList = ["outLoc0.txt"]

    def setUp(self):
        reload(ExecTrace)

    def testLog0(self):
        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                print ExecTrace.OutLog(lines)

    def tearDown(self):
        pass


class TestExecTrace(unittest.TestCase):
    basedir = "eTraces/"
    fnameList = ["etrace0.txt"]
    tIdMapHash = {"etrace0.txt": {0: 1, 1: 12, 2: 13, 3: 18}}

    def setUp(self):
        reload(ExecTrace)

    def testLog0(self):
        for fname in self.fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                eTrc = ExecTrace.ExecTrace(lines)
                tIdMap = eTrc.getTIdMap()
                self.assertEqual(tIdMap, self.tIdMapHash["etrace0.txt"])

    def tearDown(self):
        pass


class _TestThreadMatch(unittest.TestCase):
    basedir = "eTraces/"

    def setUp(self):
        reload(ExecTrace)

    def testMatch0(self):
        br0 = [[6, 1, 3, 4, 25, 26, 28, 30, 36],
               [8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 11, 15, 17,
                19, 21, 23, 12, 13],
               [], [32, 34, 35, 32, 34, 35, 32, 34, 35, 33]]
        out0 = [[], [3], [2], [1, 4, 5]]

        br1 = [[1, 3, 4, 25, 26, 28, 30, 36, 6], [],
               [8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 11, 15, 17,
                19, 21, 23, 12, 13],
               [32, 34, 35, 32, 34, 35, 32, 34, 35, 33]]
        out1 = [[], [0], [3], [1, 4, 5]]

        fnameList = ["tmatch0.txt"]
        for fname in fnameList:
            with file(self.basedir + "/" + fname) as f:
                lines = f.readlines()
                linesList = parseLines(lines)

                self.assertEqual(len(linesList), 2)

                eTrc0 = ExecTrace.ExecTrace(linesList[0])
                eTrc1 = ExecTrace.ExecTrace(linesList[1])
                self.assertEqual(eTrc0.getBrNumRepr(), br0)

                self.assertEqual(eTrc0.getOutNumRepr(), out0)
                self.assertEqual(eTrc1.getBrNumRepr(), br1)
                self.assertEqual(eTrc1.getOutNumRepr(), out1)

                eTrcList0 = SimpleMatcher([eTrc0, eTrc1])
                eTrcList1 = SimpleMatcher([eTrc1, eTrc0])
                self.assertEqual(handleNoise(eTrcList0),
                                 handleNoise(eTrcList1))

                maxVal, mLst = ExponentialMatcher(*eTrcList0)
                self.assertEqual(maxVal, 42)
                self.assertEqual(mLst, [[(0, 0), (1, 2), (2, 1), (3, 3)]])

                eTrcList0_ = MatcherForMany(eTrcList0)

                self.assertEqual(eTrcList0_[0].tIdMatchMap,
                                 {0: 0, 1: 2, 2: 1, 3: 3})
                self.assertEqual(eTrcList0_[1].tIdMatchMap,
                                 {0: 0, 1: 2, 2: 1, 3: 3})

    def testMatch1(self):
        """
        test method for 4x4 matrix.
        """
        d_ = {(0, 0): 2, (0, 1): 8, (0, 2): 1, (0, 3): 3,
                (1, 0): 10, (1, 1): 2, (1, 2): 1, (1, 3): 3,
                (2, 0): 2, (2, 1): 1, (2, 2): 2, (2, 3): 2,
                (3, 0): 3, (3, 1): 0, (3, 2): 2, (3, 3): 2}
        d = matrix(d_, 4, 4)
        # print_tab(d)
        maxVal, mLst = ExponentialMatcherImpl(d)
        self.assertEqual(maxVal, 22)
        self.assertEqual(mLst,
                         [[(0, 1), (1, 0), (2, 2), (3, 3)],
                          [(0, 1), (1, 0), (2, 3), (3, 2)]]
                         )

    def testMatch2(self):
        """
        test method for 4x4 matrix.
        """
        d_ = {(0, 0): 2, (0, 1): 8, (0, 2): 1, (0, 3): 3, (0, 4): 3,
                (1, 0): 10, (1, 1): 2, (1, 2): 1, (1, 3): 3, (1, 4): 3,
                (2, 0): 2, (2, 1): 1, (2, 2): 2, (2, 3): 2, (2, 4): 2,
                (3, 0): 3, (3, 1): 0, (3, 2): 2, (3, 3): 2, (2, 4): 2}
        d = matrix(d_, 5, 4)
        print_tab(d)
        maxVal, mLst = ExponentialMatcherImpl(d)
        self.assertEqual(maxVal, 22)
        self.assertEqual(mLst,
                         [[(0, 1), (1, 0), (2, 2), (3, 3)],
                          [(0, 1), (1, 0), (2, 3), (3, 2)]]
                         )

    def tearDown(self):
        pass


if __name__ == "__main__":
    unittest.main()
