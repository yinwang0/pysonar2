class Pizza:
  def __init__(self, toppings):
    self.toppings = toppings

  def first_topping(self):
    return self.toppings[0]


class Bread:
  def __init__(self, size):
    self.size = size

  def grow(self):
    self.size *= 2
