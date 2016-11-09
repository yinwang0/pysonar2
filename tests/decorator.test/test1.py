# test decorators for staticmethod and classmethod

class A:

    def normalm(self, x):
        return x

    @staticmethod
    def staticm(x, y):
        return x, y

    @classmethod
    def classm(cls, y):
        return cls, y

a = A()
a.normalm(10)
a.staticm(10, "hi")
a.classm("hi")
