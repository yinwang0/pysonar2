package org.yinwang.pysonar.types;


import org.yinwang.pysonar.Analyzer;

public class StrType extends Type {

    private String value;


    public StrType(String value) {
        this.value = value;
    }


    @Override
    public boolean equals(Object other) {
        return (other instanceof StrType);
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        if (Analyzer.self.debug && value != null) {
            return "str(" + value + ")";
        } else {
            return "str";
        }
    }
}
