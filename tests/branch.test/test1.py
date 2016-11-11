# test if branches


def test1(x):
    a = "hi"
    if x > 1:
        a = 3
    print a


def test2(x):
    a = "hi"
    if x > 1:
        a = 3
    else:
        a = True
    print a


def test3(x):
    a = "hi"
    if x > 1:
        a = 3
    else:
        if x < 10:
            a = True
        else:
            a = "hi"
    print a


def test4(x):
    a = "hi"
    b = "foo"
    if x > 1:
        a = 3
        b = 4
    else:
        if x < 10:
            b = True
        else:
            a = "hi"
            b = False
    print a, b
