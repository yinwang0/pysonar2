package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.Util;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.io.File;

public class Module extends Node {

    static final long serialVersionUID = -7737089963380450802L;

    public String name;
    public Block body;

    private String file;  // input source file path
    private String sha1;   // input source file sha1


    public Module(Block body, int start, int end) {
        super(start, end);
        this.body = body;
        addChildren(this.body);
    }

    public void setFile(String file) {
        this.file = file;
        this.name = Util.moduleNameFor(file);
        this.sha1 = Util.getSHA1(new File(file));
    }

    public void setFile(@NotNull File path) {
        try {
            file = path.getCanonicalPath();
        } catch (Exception e) {
            Util.msg("invalid path: " + path);
        }
        name = Util.moduleNameFor(file);
        sha1 = Util.getSHA1(path);
    }

    /**
     * Used when module is parsed from an in-memory string.
     * @param path file path
     * @param md5 sha1 message digest for source contents
     */
    public void setFileAndMD5(String path, String md5) {
        file = path;
        name = Util.moduleNameFor(file);
        this.sha1 = md5;
    }

    @Override
    public String getFile() {
        return file;
    }

    public String getMD5() {
        return sha1;
    }

    @NotNull
    @Override
    public Type resolve(@NotNull Scope s, int tag) {
        ModuleType mt = new ModuleType(Util.moduleNameFor(file), file, Indexer.idx.globaltable);
        s.put(file, new Url("file://" + file), mt, Binding.Kind.MODULE, tag);
        resolveExpr(body, mt.getTable(), tag);
        return mt;
    }


    @NotNull
    public String toLongString() {
        return "<Module:" + body + ">";
    }

    @NotNull
    @Override
    public String toString() {
        return "<Module:" + file + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
        }
    }
}
