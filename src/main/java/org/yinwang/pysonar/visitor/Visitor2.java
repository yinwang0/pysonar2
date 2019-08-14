package org.yinwang.pysonar.visitor;


import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.ast.*;

import java.util.ArrayList;
import java.util.List;

public interface Visitor2<T, P, Q> {

    default T visit(@NotNull Node node, P param1, Q param2) {
        switch (node.nodeType) {
            case ALIAS:
                return visit((Alias)node, param1, param2);
            case ASSERT:
                return visit((Assert)node, param1, param2);
            case ASSIGN:
                return visit((Assign)node, param1, param2);
            case ATTRIBUTE:
                return visit((Attribute)node, param1, param2);
            case AWAIT:
                return visit((Await)node, param1, param2);
            case BINOP:
                return visit((BinOp)node, param1, param2);
            case BLOCK:
                return visit((Block)node, param1, param2);
            case BREAK:
                return visit((Break)node, param1, param2);
            case BYTES:
                return visit((Bytes)node, param1, param2);
            case CALL:
                return visit((Call)node, param1, param2);
            case CLASSDEF:
                return visit((ClassDef)node, param1, param2);
            case COMPREHENSION:
                return visit((Comprehension)node, param1, param2);
            case CONTINUE:
                return visit((Continue)node, param1, param2);
            case DELETE:
                return visit((Delete)node, param1, param2);
            case DICT:
                return visit((Dict)node, param1, param2);
            case DICTCOMP:
                return visit((DictComp)node, param1, param2);
            case DUMMY:
                return visit((Dummy)node, param1, param2);
            case ELLIPSIS:
                return visit((Ellipsis)node, param1, param2);
            case EXEC:
                return visit((Exec)node, param1, param2);
            case EXPR:
                return visit((Expr)node, param1, param2);
            case EXTSLICE:
                return visit((ExtSlice)node, param1, param2);
            case FOR:
                return visit((For)node, param1, param2);
            case FUNCTIONDEF:
                return visit((FunctionDef)node, param1, param2);
            case GENERATOREXP:
                return visit((GeneratorExp)node, param1, param2);
            case GLOBAL:
                return visit((Global)node, param1, param2);
            case HANDLER:
                return visit((Handler)node, param1, param2);
            case IF:
                return visit((If)node, param1, param2);
            case IFEXP:
                return visit((IfExp)node, param1, param2);
            case IMPORT:
                return visit((Import)node, param1, param2);
            case IMPORTFROM:
                return visit((ImportFrom)node, param1, param2);
            case INDEX:
                return visit((Index)node, param1, param2);
            case KEYWORD:
                return visit((Keyword)node, param1, param2);
            case LISTCOMP:
                return visit((ListComp)node, param1, param2);
            case MODULE:
                return visit((PyModule)node, param1, param2);
            case NAME:
                return visit((Name)node, param1, param2);
            case PASS:
                return visit((Pass)node, param1, param2);
            case PRINT:
                return visit((Print)node, param1, param2);
            case PYCOMPLEX:
                return visit((PyComplex)node, param1, param2);
            case PYFLOAT:
                return visit((PyFloat)node, param1, param2);
            case PYINT:
                return visit((PyInt)node, param1, param2);
            case PYLIST:
                return visit((PyList)node, param1, param2);
            case PYSET:
                return visit((PySet)node, param1, param2);
            case RAISE:
                return visit((Raise)node, param1, param2);
            case REPR:
                return visit((Repr)node, param1, param2);
            case RETURN:
                return visit((Return)node, param1, param2);
            case SEQUENCE:
                return visit((Sequence)node, param1, param2);
            case SETCOMP:
                return visit((SetComp)node, param1, param2);
            case SLICE:
                return visit((Slice)node, param1, param2);
            case STARRED:
                return visit((Starred)node, param1, param2);
            case STR:
                return visit((Str)node, param1, param2);
            case SUBSCRIPT:
                return visit((Subscript)node, param1, param2);
            case TRY:
                return visit((Try)node, param1, param2);
            case TUPLE:
                return visit((Tuple)node, param1, param2);
            case UNARYOP:
                return visit((UnaryOp)node, param1, param2);
            case UNSUPPORTED:
                return visit((Unsupported)node, param1, param2);
            case URL:
                return visit((Url)node, param1, param2);
            case WHILE:
                return visit((While)node, param1, param2);
            case WITH:
                return visit((With)node, param1, param2);
            case WITHITEM:
                return visit((Withitem)node, param1, param2);
            case YIELD:
                return visit((Yield)node, param1, param2);
            case YIELDFROM:
                return visit((YieldFrom)node, param1, param2);

            default:
                throw new RuntimeException("unexpected node");
        }
    }

    default T visit(Sequence node, P param) {
        switch (node.nodeType) {
            case PYLIST:
                return visit((PyList) node, param);
            case PYSET:
                return visit((PySet) node, param);
            default: //TUPLE
                return visit((Tuple) node, param);
        }
    }

    default <N extends Node, O extends T> List<O> visit(List<N> list, P param1, Q param2) {
        List<O> result = new ArrayList<>();
        for (N elem : list) {
            result.add((O) visit(elem, param1, param2));
        }
        return result;
    }

    T visit(Alias node, P param1, Q param2);
    T visit(Assert node, P param1, Q param2);
    T visit(Assign node, P param1, Q param2);
    T visit(Attribute node, P param1, Q param2);
    T visit(Await node, P param1, Q param2);
    T visit(BinOp node, P param1, Q param2);
    T visit(Block node, P param1, Q param2);
    T visit(Break node, P param1, Q param2);
    T visit(Bytes node, P param1, Q param2);
    T visit(Call node, P param1, Q param2);
    T visit(ClassDef node, P param1, Q param2);
    T visit(Comprehension node, P param1, Q param2);
    T visit(Continue node, P param1, Q param2);
    T visit(Delete node, P param1, Q param2);
    T visit(Dict node, P param1, Q param2);
    T visit(DictComp node, P param1, Q param2);
    T visit(Dummy node, P param1, Q param2);
    T visit(Ellipsis node, P param1, Q param2);
    T visit(Exec node, P param1, Q param2);
    T visit(Expr node, P param1, Q param2);
    T visit(ExtSlice node, P param1, Q param2);
    T visit(For node, P param1, Q param2);
    T visit(FunctionDef node, P param1, Q param2);
    T visit(GeneratorExp node, P param1, Q param2);
    T visit(Global node, P param1, Q param2);
    T visit(Handler node, P param1, Q param2);
    T visit(If node, P param1, Q param2);
    T visit(IfExp node, P param1, Q param2);
    T visit(Import node, P param1, Q param2);
    T visit(ImportFrom node, P param1, Q param2);
    T visit(Index node, P param1, Q param2);
    T visit(Keyword node, P param1, Q param2);
    T visit(ListComp node, P param1, Q param2);
    T visit(PyModule node, P param1, Q param2);
    T visit(Name node, P param1, Q param2);
    T visit(Pass node, P param1, Q param2);
    T visit(Print node, P param1, Q param2);
    T visit(PyComplex node, P param1, Q param2);
    T visit(PyFloat node, P param1, Q param2);
    T visit(PyInt node, P param1, Q param2);
    T visit(PyList node, P param1, Q param2);
    T visit(PySet node, P param1, Q param2);
    T visit(Raise node, P param1, Q param2);
    T visit(Repr node, P param1, Q param2);
    T visit(Return node, P param1, Q param2);
    T visit(SetComp node, P param1, Q param2);
    T visit(Slice node, P param1, Q param2);
    T visit(Starred node, P param1, Q param2);
    T visit(Str node, P param1, Q param2);
    T visit(Subscript node, P param1, Q param2);
    T visit(Try node, P param1, Q param2);
    T visit(Tuple node, P param1, Q param2);
    T visit(UnaryOp node, P param1, Q param2);
    T visit(Unsupported node, P param1, Q param2);
    T visit(Url node, P param1, Q param2);
    T visit(While node, P param1, Q param2);
    T visit(With node, P param1, Q param2);
    T visit(Withitem node, P param1, Q param2);
    T visit(Yield node, P param1, Q param2);
    T visit(YieldFrom node, P param1, Q param2);
}
