#!/usr/bin/env python
import unittest

from alg import BrEntry, BrLog, OutEntry, OutLog


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


if __name__ == "__main__":
    unittest.main()
