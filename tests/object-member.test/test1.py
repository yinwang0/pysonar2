# test reference to field created in object


class A:
    x = 0

a1 = A()
a1.y = "hi"

print a1.x
print a1.y
