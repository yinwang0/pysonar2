package org.yinwang.pysonar.types;


import org.yinwang.pysonar.Analyzer;

public class StrType extends Type {

    public String value;


    public StrType(String value) {
        this.value = value;
    }


    @Override
    public boolean typeEquals(Object other) {
        return (other instanceof StrType);
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        if (Analyzer.self.hasOption("debug") && value != null) {
            return "str(" + value + ")";
        } else {
            return "str";
        }
    }
}
