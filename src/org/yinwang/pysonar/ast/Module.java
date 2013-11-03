package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.*;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Module extends Node {

    static final long serialVersionUID = -7737089963380450802L;

    public String name;
    public Block body;

    private String file;  // input source file path
    private String md5;   // input source file md5


    public Module(Block body, int start, int end) {
        super(start, end);
        this.body = body;
        addChildren(this.body);
    }

    public void setFile(String file) throws Exception {
        this.file = file;
        this.name = Util.moduleNameFor(file);
        this.md5 = Util.getMD5(new File(file));
    }

    public void setFile(File path) throws Exception {
        file = path.getCanonicalPath();
        name = Util.moduleNameFor(file);
        md5 = Util.getMD5(path);
    }

    /**
     * Used when module is parsed from an in-memory string.
     * @param path file path
     * @param md5 md5 message digest for source contents
     */
    public void setFileAndMD5(String path, String md5) throws Exception {
        file = path;
        name = Util.moduleNameFor(file);
        this.md5 = md5;
    }

    @Override
    public String getFile() {
        return file;
    }

    public String getMD5() {
        return md5;
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        ModuleType mt = new ModuleType(Util.moduleNameFor(file), file, Indexer.idx.globaltable);
        s.put(file, new Url("file://" + file), mt, Binding.Kind.MODULE, tag);
        resolveExpr(body, mt.getTable(), tag);
        return mt;
    }


    public String toLongString() {
        return "<Module:" + body + ">";
    }

    @Override
    public String toString() {
        return "<Module:" + file + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
        }
    }
}
