def foo(x):
    return x


def bar(y):
    return foo(y)


def baz1():
    return bar(1)


def baz2():
    return bar('hi')


baz1()
baz2()
