import ast
import codecs
import re
import sys

from json import JSONEncoder
from ast import *


# Is it Python 3?
is_python3 = (sys.version_info.major == 3)


class Encoder(JSONEncoder):
    def default(self, o):
        if hasattr(o, '__dict__'):
            d = o.__dict__
            d['ast_type'] = o.__class__.__name__
            return d
        else:
            return str(o)


if is_python3:
    encoder = Encoder()
else:
    encoder = Encoder(encoding="latin1")


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
        f1 = open(output, "w")
        f1.write(encoder.encode(tree))
        f1.close()
    finally:
        # write marker file to signal write end
        f2 = open(end_mark, "w")
        f2.write("END")
        f2.close()


def detect_encoding(path):
    fin = open(path, 'rb')
    prefix = fin.read(80)
    encs = re.findall('#.*coding\s*:\s*([\w\d\-]+)\s+', prefix)
    return encs[0] if encs else 'utf8'


## contents of improve_ast

#-------------------------------------------------------------
#                   improvements to the AST
#-------------------------------------------------------------


# Is it Python 3?
is_python3 = (sys.version_info.major == 3)


def improve_ast(node, s):
    idxmap = build_index_map(s)
    improve_node(node, s, idxmap)


def improve_node(node, s, idxmap):

    if isinstance(node, list):
        for n in node:
            improve_node(n, s, idxmap)

    elif isinstance(node, AST):

        find_node_start(node, s, idxmap)
        find_node_end(node, s, idxmap)
        add_missing_names(node, s, idxmap)

        for f in node_fields(node):
            improve_node(f, s, idxmap)



def find_node_start(node, s, idxmap):
    ret = None    # default value

    if hasattr(node, 'node_start'):
        ret = node.node_start

    elif isinstance(node, list):
        if node != []:
            ret = find_node_start(node[0], s, idxmap)

    elif isinstance(node, Module):
        if node.body != []:
            ret = find_node_start(node.body[0], s, idxmap)

    elif isinstance(node, BinOp):
        leftstart = find_node_start(node.left, s, idxmap)
        if leftstart != None:
            ret = leftstart
        else:
            ret = map_idx(idxmap, node.lineno, node.col_offset)

    elif hasattr(node, 'lineno'):
        if node.col_offset >= 0:
            ret = map_idx(idxmap, node.lineno, node.col_offset)
        else:                           # special case for """ strings
            i = map_idx(idxmap, node.lineno, node.col_offset)
            while i > 0 and i+2 < len(s) and s[i:i+3] != '"""':
                i -= 1
            ret = i
    else:
        return None

    if ret == None and hasattr(node, 'lineno'):
        raise TypeError("got None for node that has lineno", node)

    if isinstance(node, AST) and ret != None:
        node.node_start = ret

    return ret



def find_node_end(node, s, idxmap):

    the_end = None

    if hasattr(node, 'node_end'):
        return node.node_end

    elif isinstance(node, list):
        if node != []:
            the_end = find_node_end(node[-1], s, idxmap)

    elif isinstance(node, Module):
        if node.body != []:
            the_end = find_node_end(node.body[-1], s, idxmap)

    elif isinstance(node, Expr):
        the_end = find_node_end(node.value, s, idxmap)

    elif isinstance(node, Str):
        i = find_node_start(node, s, idxmap)
        while s[i] != '"' and s[i] != "'":
            i += 1

        if i+2 < len(s) and s[i:i+3] == '"""':
            q = '"""'
            i += 3
        elif s[i] == '"':
            q = '"'
            i += 1
        elif s[i] == "'":
            q = "'"
            i += 1
        else:
            print("illegal quote:", i, s[i])
            q = ''

        if q != '':
            the_end = end_seq(s, q, i)

    elif isinstance(node, Name):
        the_end = find_node_start(node, s, idxmap) + len(node.id)

    elif isinstance(node, Attribute):
        the_end = end_seq(s, node.attr, find_node_end(node.value, s, idxmap))

    elif isinstance(node, FunctionDef):
        the_end = find_node_end(node.body, s, idxmap)

    elif isinstance(node, Lambda):
        the_end = find_node_end(node.body, s, idxmap)

    elif isinstance(node, ClassDef):
        the_end = find_node_end(node.body, s, idxmap)

    # print will be a Call in Python 3
    elif not is_python3 and isinstance(node, Print):
        the_end = start_seq(s, '\n', find_node_start(node, s, idxmap))

    elif isinstance(node, Call):
        start = find_node_end(node.func, s, idxmap)
        if start != None:
            the_end = match_paren(s, '(', ')', start)

    elif isinstance(node, Yield):
        the_end = find_node_end(node.value, s, idxmap)

    elif isinstance(node, Return):
        if node.value != None:
            the_end = find_node_end(node.value, s, idxmap)
        else:
            the_end = find_node_start(node, s, idxmap) + len('return')

    elif (isinstance(node, For) or
          isinstance(node, While) or
          isinstance(node, If) or
          isinstance(node, IfExp)):
        if node.orelse != []:
            the_end = find_node_end(node.orelse, s, idxmap)
        else:
            the_end = find_node_end(node.body, s, idxmap)

    elif isinstance(node, Assign) or isinstance(node, AugAssign):
        the_end = find_node_end(node.value, s, idxmap)

    elif isinstance(node, BinOp):
        the_end = find_node_end(node.right, s, idxmap)

    elif isinstance(node, BoolOp):
        the_end = find_node_end(node.values[-1], s, idxmap)

    elif isinstance(node, Compare):
        the_end = find_node_end(node.comparators[-1], s, idxmap)

    elif isinstance(node, UnaryOp):
        the_end = find_node_end(node.operand, s, idxmap)

    elif isinstance(node, Num):
        the_end = find_node_start(node, s, idxmap) + len(str(node.n))

    elif isinstance(node, List):
        the_end = match_paren(s, '[', ']', find_node_start(node, s, idxmap));

    elif isinstance(node, Subscript):
        the_end = match_paren(s, '[', ']', find_node_start(node, s, idxmap));

    elif isinstance(node, Tuple):
        if node.elts != []:
            the_end = find_node_end(node.elts[-1], s, idxmap)

    elif isinstance(node, Dict):
        the_end = match_paren(s, '{', '}', find_node_start(node, s, idxmap));

    elif ((not is_python3 and isinstance(node, TryExcept)) or
          (is_python3 and isinstance(node, Try))):
        if node.orelse != []:
            the_end = find_node_end(node.orelse, s, idxmap)
        elif node.handlers != []:
            the_end = find_node_end(node.handlers, s, idxmap)
        else:
            the_end = find_node_end(node.body, s, idxmap)

    elif isinstance(node, ExceptHandler):
        the_end = find_node_end(node.body, s, idxmap)

    elif isinstance(node, Pass):
        the_end = find_node_start(node, s, idxmap) + len('pass')

    elif isinstance(node, Break):
        the_end = find_node_start(node, s, idxmap) + len('break')

    elif isinstance(node, Continue):
        the_end = find_node_start(node, s, idxmap) + len('continue')

    elif isinstance(node, Global):
        the_end = start_seq(s, '\n', find_node_start(node, s, idxmap))

    elif isinstance(node, Import):
        the_end = find_node_start(node, s, idxmap) + len('import')

    elif isinstance(node, ImportFrom):
        the_end = find_node_start(node, s, idxmap) + len('from')

    else:   # can't determine node end, set to 3 chars after start
        start = find_node_start(node, s, idxmap)
        if start != None:
            the_end = start + 3

    if isinstance(node, AST) and the_end != None:
        node.node_end = the_end

    return the_end



def add_missing_names(node, s, idxmap):

    if hasattr(node, 'extraAttribute'):
        return

    if isinstance(node, list):
        for n in node:
            add_missing_names(n, s, idxmap)

    elif isinstance(node, ClassDef):
        start = find_node_start(node, s, idxmap) + len('class')
        if start != None:
            node.name_node = str_to_name(s, start, idxmap)
            node._fields += ('name_node',)

    elif isinstance(node, FunctionDef):
        start = find_node_start(node, s, idxmap) + len('def')
        if start != None:
            node.name_node = str_to_name(s, start, idxmap)
            node._fields += ('name_node',)

        # keyword_start = find_node_start(node, s, idxmap)
        # node.keyword_node = str_to_name(s, keyword_start, idxmap)
        # node._fields += ('keyword_node',)

        if node.args.vararg != None:
            if len(node.args.args) > 0:
                vstart = find_node_end(node.args.args[-1], s, idxmap)
            else:
                vstart = find_node_end(node.name_node, s, idxmap)
            if vstart != None:
                vname = str_to_name(s, vstart, idxmap)
                node.vararg_name = vname
        else:
            node.vararg_name = None
        node._fields += ('vararg_name',)

        if node.args.kwarg != None:
            if len(node.args.args) > 0:
                kstart = find_node_end(node.args.args[-1], s, idxmap)
            else:
                kstart = find_node_end(node.vararg_name, s, idxmap)
            if kstart:
                kname = str_to_name(s, kstart, idxmap)
                node.kwarg_name = kname
        else:
            node.kwarg_name = None
        node._fields += ('kwarg_name',)

    elif isinstance(node, Attribute):
        start = find_node_end(node.value, s, idxmap)
        if start is not None:
            name = str_to_name(s, start, idxmap)
            node.attr_name = name
            node._fields = ('value', 'attr_name')  # remove attr for node size accuracy

    elif isinstance(node, Compare):
        start = find_node_start(node, s, idxmap)
        if start is not None:
            node.opsName = convert_ops(node.ops, s, start, idxmap)
            node._fields += ('opsName',)

    elif (isinstance(node, BoolOp) or
          isinstance(node, BinOp) or
          isinstance(node, UnaryOp) or
          isinstance(node, AugAssign)):
        if hasattr(node, 'left'):
            start = find_node_end(node.left, s, idxmap)
        else:
            start = find_node_start(node, s, idxmap)
        if start is not None:
            ops = convert_ops([node.op], s, start, idxmap)
        else:
            ops = []
        if ops != []:
            node.op_node = ops[0]
            node._fields += ('op_node',)

    # elif isinstance(node, Import):
    #    name_nodes = []
    #    next = find_node_start(node, s, idxmap) + len('import')
    #    if next is not None:
    #        name = str_to_name(s, next, idxmap)
    #        while name is not None and next < len(s) and s[next] != '\n':
    #            name_nodes.append(name)
    #            next = name.node_end
    #            name = str_to_name(s, next, idxmap)
    #        node.name_nodes = name_nodes
    #        node._fields += ('name_nodes',)

    node.extraAttribute = True



#-------------------------------------------------------------
#              utilities used by improve AST functions
#-------------------------------------------------------------

# find a sequence in a string s, returning the start point
def start_seq(s, pat, start):
    try:
        return s.index(pat, start)
    except ValueError:
        return len(s)



# find a sequence in a string s, returning the end point
def end_seq(s, pat, start):
    try:
        return s.index(pat, start) + len(pat)
    except ValueError:
        return len(s)



# find matching close paren from start
def match_paren(s, open, close, start):
    while start < len(s) and s[start] != open:
        start += 1
    if start >= len(s):
        return len(s)

    left = 1
    i = start + 1
    while left > 0 and i < len(s):
        if s[i] == open:
            left += 1
        elif s[i] == close:
            left -= 1
        i += 1
    return i



# build table for lineno <-> index oonversion
def build_index_map(s):
    line = 0
    col = 0
    idx = 0
    idxmap = [0]
    while idx < len(s):
        if s[idx] == '\n':
            idxmap.append(idx + 1)
            line += 1
        idx += 1
    return idxmap



# convert (line, col) to offset index
def map_idx(idxmap, line, col):
    return idxmap[line-1] + col



# convert offset index into (line, col)
def map_line_col(idxmap, idx):
    line = 0
    for start in idxmap:
        if idx < start:
            break
        line += 1
    col = idx - idxmap[line-1]
    return (line, col)



# convert string to Name
def str_to_name(s, start, idxmap):
    i = start;
    while i < len(s) and not is_alpha(s[i]):
        i = i + 1
    name_start = i

    ret = []
    while i < len(s) and is_alpha(s[i]):
        ret.append(s[i])
        i += 1
    name_end = i

    id1 = ''.join(ret)
    if id1 == '':
        return None
    else:
        name = Name(id1, None)
        name.node_start = name_start
        name.node_end = name_end
        name.lineno, name.col_offset = map_line_col(idxmap, name_start)
        return name



def convert_ops(ops, s, start, idxmap):
    syms = []
    for op in ops:
        if type(op) in ops_map:
            syms.append(ops_map[type(op)])
        else:
            print("[WARNING] operator %s is missing from ops_map, " \
                  "please report the bug on GitHub" % op)

    i = start
    j = 0
    ret = []
    while i < len(s) and j < len(syms):
        oplen = len(syms[j])
        if s[i:i+oplen] == syms[j]:
            op_node = Name(syms[j], None)
            op_node.node_start = i
            op_node.node_end = i+oplen
            op_node.lineno, op_node.col_offset = map_line_col(idxmap, i)
            ret.append(op_node)
            j += 1
            i = op_node.node_end
        else:
            i += 1
    return ret


# lookup table for operators for convert_ops
ops_map = {
    # compare:
    Eq     : '==',
    NotEq  : '!=',
    LtE    : '<=',
    Lt     : '<',
    GtE    : '>=',
    Gt     : '>',
    NotIn  : 'not in',
    In     : 'in',
    IsNot  : 'is not',
    Is     : 'is',

    # BoolOp
    Or  : 'or',
    And : 'and',
    Not : 'not',
    Invert : '~',

    # bit operators
    BitOr : '|',
    BitAnd : '&',
    BitXor : '^',
    RShift : '>>',
    LShift : '<<',


    # BinOp
    Add  : '+',
    Sub  : '-',
    Mult : '*',
    Div  : '/',
    FloorDiv : '//',
    Mod  : '%',
    Pow : '**',

    # UnaryOp
    USub : '-',
    UAdd : '+',
}

# get list of fields from a node
def node_fields(node):
    ret = []
    for field in node._fields:
        if field != 'ctx' and hasattr(node, field):
            ret.append(getattr(node, field))
    return ret



# get full source text where the node is from
def node_source(node):
    if hasattr(node, 'node_source'):
        return node.node_source
    else:
        return None



# utility for getting exact source code part of the node
def src(node):
    return node.node_source[node.node_start: node.node_end]



def node_start(node):
    if (hasattr(node, 'node_start')):
        return node.node_start
    else:
        return 0



def node_end(node):
    if hasattr(node, 'node_end'):
        return node.node_end
    else:
        return None


def is_alpha(c):
    return (c == '_'
            or ('0' <= c <= '9')
            or ('a' <= c <= 'z')
            or ('A' <= c <= 'Z'))
