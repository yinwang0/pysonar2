package org.yinwang.pysonar.visitor;


import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.ast.*;

import java.util.ArrayList;
import java.util.List;

public interface Visitor0<T> {

    default T visit(@NotNull Node node) {
        switch (node.nodeType) {
            case ALIAS:
                return visit((Alias)node);
            case ASSERT:
                return visit((Assert)node);
            case ASSIGN:
                return visit((Assign)node);
            case ATTRIBUTE:
                return visit((Attribute)node);
            case AWAIT:
                return visit((Await)node);
            case BINOP:
                return visit((BinOp)node);
            case BLOCK:
                return visit((Block)node);
            case BREAK:
                return visit((Break)node);
            case BYTES:
                return visit((Bytes)node);
            case CALL:
                return visit((Call)node);
            case CLASSDEF:
                return visit((ClassDef)node);
            case COMPREHENSION:
                return visit((Comprehension)node);
            case CONTINUE:
                return visit((Continue)node);
            case DELETE:
                return visit((Delete)node);
            case DICT:
                return visit((Dict)node);
            case DICTCOMP:
                return visit((DictComp)node);
            case DUMMY:
                return visit((Dummy)node);
            case ELLIPSIS:
                return visit((Ellipsis)node);
            case EXEC:
                return visit((Exec)node);
            case EXPR:
                return visit((Expr)node);
            case EXTSLICE:
                return visit((ExtSlice)node);
            case FOR:
                return visit((For)node);
            case FUNCTIONDEF:
                return visit((FunctionDef)node);
            case GENERATOREXP:
                return visit((GeneratorExp)node);
            case GLOBAL:
                return visit((Global)node);
            case HANDLER:
                return visit((Handler)node);
            case IF:
                return visit((If)node);
            case IFEXP:
                return visit((IfExp)node);
            case IMPORT:
                return visit((Import)node);
            case IMPORTFROM:
                return visit((ImportFrom)node);
            case INDEX:
                return visit((Index)node);
            case KEYWORD:
                return visit((Keyword)node);
            case LISTCOMP:
                return visit((ListComp)node);
            case MODULE:
                return visit((PyModule)node);
            case NAME:
                return visit((Name)node);
            case NODE:
                return visit((Node)node);
            case PASS:
                return visit((Pass)node);
            case PRINT:
                return visit((Print)node);
            case PYCOMPLEX:
                return visit((PyComplex)node);
            case PYFLOAT:
                return visit((PyFloat)node);
            case PYINT:
                return visit((PyInt)node);
            case PYLIST:
                return visit((PyList)node);
            case PYSET:
                return visit((PySet)node);
            case RAISE:
                return visit((Raise)node);
            case REPR:
                return visit((Repr)node);
            case RETURN:
                return visit((Return)node);
            case SEQUENCE:
                return visit((Sequence)node);
            case SETCOMP:
                return visit((SetComp)node);
            case SLICE:
                return visit((Slice)node);
            case STARRED:
                return visit((Starred)node);
            case STR:
                return visit((Str)node);
            case SUBSCRIPT:
                return visit((Subscript)node);
            case TRY:
                return visit((Try)node);
            case TUPLE:
                return visit((Tuple)node);
            case UNARYOP:
                return visit((UnaryOp)node);
            case UNSUPPORTED:
                return visit((Unsupported)node);
            case URL:
                return visit((Url)node);
            case WHILE:
                return visit((While)node);
            case WITH:
                return visit((With)node);
            case WITHITEM:
                return visit((Withitem)node);
            case YIELD:
                return visit((Yield)node);
            case YIELDFROM:
                return visit((YieldFrom)node);

            default:
                throw new RuntimeException("unexpected node");
        }
    }

    default T visit(Sequence node) {
        switch (node.nodeType) {
            case PYLIST:
                return visit((PyList) node);
            case PYSET:
                return visit((PySet) node);
            default: //TUPLE
                return visit((Tuple) node);
        }
    }

    default <N extends Node, O extends T> List<O> visit(List<N> list) {
        List<O> result = new ArrayList<>();
        for (N elem : list) {
            result.add((O) visit(elem));
        }
        return result;
    }

    T visit(Alias node);
    T visit(Assert node);
    T visit(Assign node);
    T visit(Attribute node);
    T visit(Await node);
    T visit(BinOp node);
    T visit(Block node);
    T visit(Break node);
    T visit(Bytes node);
    T visit(Call node);
    T visit(ClassDef node);
    T visit(Comprehension node);
    T visit(Continue node);
    T visit(Delete node);
    T visit(Dict node);
    T visit(DictComp node);
    T visit(Dummy node);
    T visit(Ellipsis node);
    T visit(Exec node);
    T visit(Expr node);
    T visit(ExtSlice node);
    T visit(For node);
    T visit(FunctionDef node);
    T visit(GeneratorExp node);
    T visit(Global node);
    T visit(Handler node);
    T visit(If node);
    T visit(IfExp node);
    T visit(Import node);
    T visit(ImportFrom node);
    T visit(Index node);
    T visit(Keyword node);
    T visit(ListComp node);
    T visit(PyModule node);
    T visit(Name node);
    T visit(Pass node);
    T visit(Print node);
    T visit(PyComplex node);
    T visit(PyFloat node);
    T visit(PyInt node);
    T visit(PyList node);
    T visit(PySet node);
    T visit(Raise node);
    T visit(Repr node);
    T visit(Return node);
    T visit(SetComp node);
    T visit(Slice node);
    T visit(Starred node);
    T visit(Str node);
    T visit(Subscript node);
    T visit(Try node);
    T visit(Tuple node);
    T visit(UnaryOp node);
    T visit(Unsupported node);
    T visit(Url node);
    T visit(While node);
    T visit(With node);
    T visit(Withitem node);
    T visit(Yield node);
    T visit(YieldFrom node);
}
