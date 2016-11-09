u = 1
v = 'hi'


def f(x, y=None):
    if x < 5:
        return u
    else:
        print v
    return True
    y = 'hi'
    print y

y = f(42)
print y
