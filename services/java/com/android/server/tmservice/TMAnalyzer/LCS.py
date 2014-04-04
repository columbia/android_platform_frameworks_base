"""
This is an implementation for LCS(Longest Common Subsequence) algorithm.
Source: http://www.algorithmist.com/index.php/Longest_Common_Subsequence
"""
from Util import tuplify


def LCS(x, y, reconFlag=True):
    """
    """
    n = len(x)
    m = len(y)
    table = dict()  # a hashtable, but we'll use it as a 2D array here

    for i in range(n + 1):      # i=0,1,...,n
        for j in range(m + 1):  # j=0,1,...,m
            if i == 0 or j == 0:
                table[i, j] = 0
            elif x[i - 1] == y[j - 1]:
                table[i, j] = table[i - 1, j - 1] + 1
            else:
                table[i, j] = max(table[i - 1, j], table[i, j - 1])

    # Now, table[n, m] is the length of LCS of x and y.

    # Let's go one step further and reconstruct
    # the actual sequence from DP table:

    def recon(i, j):
        if i == 0 or j == 0:
            return []
        elif x[i - 1] == y[j - 1]:
            return recon(i - 1, j - 1) + [x[i - 1]]

        # index out of bounds bug here: what if the first elements in the
        # sequences aren't equal
        elif table[i - 1, j] > table[i, j - 1]:
            return recon(i - 1, j)
        else:
            return recon(i, j - 1)
    if reconFlag:
        return recon(n, m)
    else:
        return table[n, m]


def LCS2(x, y):
    """
    """
    assert(len(x) == len(y) and "sanity check")
    ret = []
    for i in range(len(x)):
        ret.append(LCS(x[i], y[i]))
    return tuplify(ret)

# simple testing.
if __name__ == "__main__":
    l0 = (1, 3, 4, 9, 10, 12, 14, 24, 26, 6)
    l1 = (1, 3, 4, 9, 10, 12, 14, 24, 26, 6, 6, 7, 6)
    print LCS(l0, l1)
