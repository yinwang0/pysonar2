from kitchen import *
from kitchen.oven import *

pizza = Pizza(['sauage', 'tomato', 'cheeze'])
t1 = pizza.first_topping()
print spoon(t1)
print fork(True)

bread = Bread(10)
bread.grow()
print bread.size
