from kitchen import oven
from kitchen import spoon

pizza = oven.Pizza(['mushroom', 'sauage', 'cheeze'])
t1 = pizza.first_topping()
print spoon(t1)
