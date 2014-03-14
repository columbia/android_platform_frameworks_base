"""
Module for utility methods
"""


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


def tuplify(lst):
    """
    Utility function to make mutable object(list) immutable(tuple) recursively.
    """
    ret = []
    for el in lst:
        if isinstance(el, list):
            ret.append(tuplify(el))
        else:
            ret.append(el)
    else:
        return tuple(ret)
