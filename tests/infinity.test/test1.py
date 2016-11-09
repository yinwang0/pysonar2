#!/usr/bin/python
# -*- coding: utf-8 -*-

x = int()

if 1.5 < x and x < 10:
    if x < 6.2:
        w = x # [2, 6]
    else:
        w = x # [7, 10)
else:
    w = x # (-∞, 1] [10, +∞)

print w
