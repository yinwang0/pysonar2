# test


class A:
    def __init__(self, value):
        self.value = value

    def __sub__(self, other):
        if isinstance(other, int):
            return 2 * other

    def __lt__(self, other):
        if isinstance(other, int):
            return len(str(self.value)) < other
        else:
            return False


def fib(n):
    if n < 2:
        return 1
    else:
        return fib(n - 1) + fib(n - 2)


x = A("foo")
print fib(x)
