from kitchen.oven import *

pizza = Pizza(['sauage', 'tomato', 'cheeze'])
print pizza.first_topping()

bread = Bread(10)
bread.grow()
print bread.size
