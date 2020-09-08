package org.codingmatters.poom.ci.apps.releaser.task;

import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextPropagationCandidatesGetRequest;
import org.codingmatters.poom.ci.dependency.api.RepositoryJustNextPropagationCandidatesGetResponse;
import org.codingmatters.poom.ci.dependency.client.PoomCIDependencyAPIClient;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class SelectForPropagationTask implements Callable<TaskResult> {
    private final String repository;
    private final PoomCIDependencyAPIClient dependencyClient;

    public SelectForPropagationTask(String repository, PoomCIDependencyAPIClient dependencyClient) {
        this.repository = repository;
        this.dependencyClient = dependencyClient;
    }

    @Override
    public TaskResult call() throws Exception {
        List<String> selected = new LinkedList<>();

//        RepositoryJustNextPropagationCandidatesGetResponse response = this.dependencyClient.repositories().repository().repositoryPropagationCandidates().repositoryJustNextPropagationCandidates().get(RepositoryJustNextPropagationCandidatesGetRequest.builder()
//                .repositoryId()
//                .build());

        return null;
    }
}
