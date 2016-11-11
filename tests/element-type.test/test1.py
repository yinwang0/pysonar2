# test inferring element type from subscript assignment, and append, update methods

a = []
a[0] = 1
print a

b = {}
b[0] = "hello"
x = b[1]
print x

c = []
c.append(1)
z = c[0]
print z

d = {}
d.update({'x': 10})
d.update({'y': True})
u = d['foo']
print u
