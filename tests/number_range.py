# import random

def test1(random):
    x = int(random)
    y = int(random)

    if 1 < x < 2 and y < 4:
        z = x + y
        if z < 7:
            w = "true"
        else:
            w = False
    print x, y, w

def test2(random):
    x = int(random)
    y = int(random)

    if 1 < x < 2 or y < 4:
        z = x + y
        if z < 7:
            w = "true"
        else:
            w = False
    print x, y, w

