# t4


class A:
    value = 1

    def __init__(self, value):
        self.value = value

    def __sub__(self, other):
        if isinstance(other, int):
            return self.value * other
        else:
            return 0

    def __lt__(self, other):
        if isinstance(other, int):
            return self.value + other
        else:
            return False


def fib(n):
    if n < 2:
        return 1
    else:
        return fib(n - 1) + fib(n - 2)


x = A(42)
y = x.value
print fib(x)

u = x < 2
print u
