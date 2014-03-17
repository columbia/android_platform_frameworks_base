#!/usr/bin/env python
import sys
import unittest
sys.path.append("../")

from ExecTrace import BrEntry, BrLog, OutEntry, OutLog, ExecTrace


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


class TestOutEntry(unittest.TestCase):
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

    def testLog0(self):
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
                print eTrc
                tIdMap = eTrc.getTIdMap()
                print tIdMap

    def testTIdMap(self):
        tIdMap_ = {0: 1, 1: 12, 2: 13, 3: 18}
        with file(self.basedir + "/etrace0.txt") as f:
            lines = f.readlines()
            eTrc = ExecTrace(lines)
            print eTrc
            tIdMap = eTrc.getTIdMap()
            self.assertEqual(tIdMap, tIdMap_)

    def tearDown(self):
        pass

if __name__ == "__main__":
    unittest.main()
