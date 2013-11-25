class A:
  a = 1


class B:
  a = 'hi'

def g(x):
  return x


def f(x):
  v = 1
  if (x > 2):
#    v = A()
    g(v.a)
#    return 1
  else:
    v = B()
    g(v.a)
  g(v.a)
  return True


f(random())

