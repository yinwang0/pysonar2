# test

def foo(f):
    return f(1), f(True)

def id(x):
    return x

a = foo(id)
print(a)


