package org.yinwang.pysonar.types;

import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;


public class BoolType extends Type {

    public enum Value {
        True,
        False,
        Undecided
    }


    private Value value;
    private State s1;
    private State s2;


    public BoolType(Value value) {
        this.value = value;
    }


    public BoolType(State s1, State s2) {
        this.value = Value.Undecided;
        this.s1 = s1;
        this.s2 = s2;
    }


    public Value getValue() {
        return value;
    }


    public void setValue(Value value) {
        this.value = value;
    }


    public State getS1() {
        return s1;
    }


    public void setS1(State s1) {
        this.s1 = s1;
    }


    public State getS2() {
        return s2;
    }


    public void setS2(State s2) {
        this.s2 = s2;
    }

    public BoolType swap() {
        return new BoolType(s2, s1);
    }


    @Override
    public boolean equals(Object other) {
        return (other instanceof BoolType);
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        if (Analyzer.self.debug) {
            return "bool(" + value + ")";
        } else {
            return "bool";
        }
    }
}
