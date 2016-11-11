# Test constructor __init__


class TestInit1:
    def test(self):
        x = self.my_field

    def test2(self):
        y = self.my_field

    def __init__(self, x):
        self.my_field = x

# with invocation
y = TestInit1(5)
z = TestInit1(6)
y.my_field = z.my_field


# without invocation
class TestInit2:
    def test(self):
        x = self.my_field

    def __init__(self, x):
        self.my_field = x

    def test2(self):
        y = self.my_field


# without invocation and other methods
class TestInit3:
    def __init__(self):
        self.my_field = 3
        print self.my_field
