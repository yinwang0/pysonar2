# test assign into field

class A:
    x = 1

a = A()
a.x = "foo"
a.y = 2      # create field in object here

print a.x, a.y
