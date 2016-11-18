class Base:
    def x(self):
        return ""

class A(Base):
    def x(self):
        return "A"

class B(Base):
    def x(Base):
        return "B"

def f1(n):
    if n == 0:
        a = A()
        return a
    else:
        return f1(0).x()


k = f1(1)
print k
