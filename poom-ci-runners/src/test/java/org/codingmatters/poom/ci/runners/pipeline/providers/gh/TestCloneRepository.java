package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

import org.codingmatters.poom.ci.runners.git.CloneRepository;

import java.io.*;
import java.net.URISyntaxException;

public class TestCloneRepository extends CloneRepository {
    public TestCloneRepository(String repositoryUrl, String branch, String changeset) {
        super(repositoryUrl, branch, changeset);
    }

    @Override
    public void to(File workspace) throws AbstractGitHubPipelineContextProvider.ProcessingException {
        try {
            File repo = new File(Thread.currentThread().getContextClassLoader().getResource("repos/" + this.repositoryUrl()).toURI());
            this.copy(repo, workspace);
        } catch (URISyntaxException | IOException e) {
            throw new AbstractGitHubPipelineContextProvider.ProcessingException("failed cloning test repo", e);
        }
    }

    private void copy(File repo, File toDir) throws IOException {
        for (File file : repo.listFiles()) {
            if(file.isDirectory()) {
                new File(toDir, file.getName()).mkdirs();
                this.copy(file, new File(toDir, file.getName()));
            } else {
                this.copyFile(toDir, file);
            }
        }
    }

    private void copyFile(File workspace, File file) throws IOException {
        try(InputStream in = new FileInputStream(file); OutputStream out = new FileOutputStream(new File(workspace, file.getName()))) {
            byte [] buffer = new byte[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }
}
