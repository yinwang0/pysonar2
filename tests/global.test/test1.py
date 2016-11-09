x = 1
y = True

def f():
    global x
    x = False
    y = 42
    print x
    print y


def g():
    x = 'hi'
    print x
    y = 'foo'
    print y
    global x

print y
