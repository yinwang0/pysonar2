# test union inside tuple

def x(q):
    if q == 0:
        return (2, True)
    else:
        return ("hi", False)

y, z = x(3)
