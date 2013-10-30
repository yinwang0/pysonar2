import sys
import ast
from json import JSONEncoder
from improve_ast import *


# Is it Python 3?
is_python3 = (sys.version_info.major == 3)


class Encoder(JSONEncoder):
    def default(self, o):
        d = o.__dict__
        d['ast_type'] = o.__class__.__name__
        return d


if is_python3:
    encoder = Encoder()
else:
    encoder = Encoder(encoding='latin1')


def parse(filename):
    f = open(filename)
    lines = f.read()
    f.close()
    return parse_string(lines, filename)


def parse_string(lines, filename=None):
    tree = ast.parse(lines)
    improve_ast(tree, lines)
    if filename:
        tree.filename = filename
    return tree


def parse_file(file, output, end_mark):
    try:
        tree = parse(file)
        f = open(output, "w")
        f.write(encoder.encode(tree))
        f.close()
    finally:
        # write marker file to signal write end
        f = open(end_mark, "w")
        f.close()
