package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar._;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.io.File;


public class Module extends Node
{

    public String name;
    public Block body;

    private String file;  // input source file path
    private String sha1;   // input source file sha1


    public Module(Block body, int start, int end)
    {
        super(start, end);
        this.body = body;
        addChildren(this.body);
    }


    public void setFile(String file)
    {
        this.file = file;
        this.name = _.moduleNameFor(file);
        this.sha1 = _.getSHA1(new File(file));
    }


    public void setFile(@NotNull File path)
    {
        try
        {
            file = _.unifyPath(path);
        }
        catch (Exception e)
        {
            _.msg("invalid path: " + path);
        }
        name = _.moduleNameFor(file);
        sha1 = _.getSHA1(path);
    }


    /**
     * Used when module is parsed from an in-memory string.
     *
     * @param path file path
     * @param md5  sha1 message digest for source contents
     */
    public void setFileAndMD5(String path, String md5)
    {
        file = path;
        name = _.moduleNameFor(file);
        this.sha1 = md5;
    }


    @Override
    public String getFile()
    {
        return file;
    }


    public String getMD5()
    {
        return sha1;
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s)
    {
        ModuleType mt = new ModuleType(_.moduleNameFor(file), file, Indexer.idx.globaltable);
        s.insert(_.moduleQname(file), this, mt, Binding.Kind.MODULE);
        resolveExpr(body, mt.getTable());
        return mt;
    }


    @NotNull
    public String toLongString()
    {
        return "<Module:" + body + ">";
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Module:" + file + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        if (v.visit(this))
        {
            visitNode(body, v);
        }
    }
}
