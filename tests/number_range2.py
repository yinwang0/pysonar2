# import random

# returns min(x,100)
def atmost100(x):
    if x > 100:
        return 100
    else:
        return x

# --------------------- example 1 ------------------------

# random number range from 0..999
rand1000 = int(random.random() * 1000);

# z will be at most 100 no matter what the input is
z = atmost100(rand1000)

# since z will be at most 100, z < 200 is always true
if z < 200:
    w1 = 42
else:
    w1 = "false"

# w1 can only be 42, and only refers to the w on the true branch
print w1


# --------------------- example 1a ------------------------

# random number range from 0..999
rand1000 = int(random.random() * 1000);

# z will be at most 100 no matter what the input is
z = atmost100(rand1000)

# z will be at most 100, but we don't know whether z<10 is true
if z < 10:
    w1a = 42
else:
    w1a = "false"

# w1a can be either 42 or "false"
print w1a


# --------------------- example 2 ------------------------
z = atmost100(10)

if z < 20:
    w2 = 42
else:
    w2 = "false"

# w2 will be 10, and only refers to the w2 on the true branch
print w2


# --------------------- example 3 ------------------------
z = atmost100(10)

if z < 4:
    w3 = 42
else:
    w3 = "false"

# w3 will be 10, and only refers to the w3 on the false branch
print w3
