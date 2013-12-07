# returns min(x,10)
def max10(x):
    if x > 10:
        return 10
    else:
        return x

z = max10(4)

if z < 5:
    w = 42
else:
    w = "false"

print w
