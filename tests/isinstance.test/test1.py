# test isinstance inference

x = random()


def foo():
    return int


if isinstance(x, foo()):
    y = x
else:
    z = x

print(y, z)
