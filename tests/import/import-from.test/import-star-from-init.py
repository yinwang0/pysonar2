# import * from __init__.py of kitchen package

from kitchen import *

print spoon(1)
print fork(2)

# knife is not export from __init__.py
# should not be able to find it
print knife(3)
