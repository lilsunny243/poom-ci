package org.codingmatters.poom.ci.apps.releaser;

import org.codingmatters.poom.services.support.Env;

import java.io.File;
import java.util.UUID;

public class Workspace {
    static public final String RELEASER_WORKSPACE_PARENT_DIR = "RELEASER_WORKSPACE_PARENT_DIR";

    static public Workspace temporary() {
        File directory = new File(new File(
                Env.optional(RELEASER_WORKSPACE_PARENT_DIR).orElse(new Env.Var(System.getProperty("java.io.tmpdir"))).asString()
        ), UUID.randomUUID().toString());
        directory.mkdir();
        return new Workspace(directory);
    }

    private final File directory;

    public Workspace(File directory) {
        this.directory = directory;
    }

    public File mkdir(String name) {
        File result = new File(this.directory, name);
        result.mkdirs();
        return result;
    }

    public void delete() {
        this.recursiveDelete(this.directory);
    }

    private void recursiveDelete(File root) {
        if(root.isDirectory()) {
            for (File file : root.listFiles()) {
                this.recursiveDelete(file);
            }
        }
        root.delete();
    }

    public String path() {
        return this.directory.getAbsolutePath();
    }
}
