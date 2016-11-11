# test lambda


def foo(f):
    y = f(1, "hi")
    return y

z = foo(lambda a,b: [a,b])
print z

w = (lambda f: f(1))(lambda x: x+1)
print w
