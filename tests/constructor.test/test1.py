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


# without invocation
class TestInit2:
    def test(self):
        x = self.my_field

    def __init__(self, x):
        self.my_field = x

    def test2(self):
        y = self.my_field
