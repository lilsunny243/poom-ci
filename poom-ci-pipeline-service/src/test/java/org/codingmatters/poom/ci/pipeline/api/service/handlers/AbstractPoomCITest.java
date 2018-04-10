package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.types.Pipeline;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

public class AbstractPoomCITest {

    private InMemoryRepository<Pipeline, String> pipelineRepository = new InMemoryRepository<Pipeline, String>() {
        @Override
        public PagedEntityList<Pipeline> search(String query, long startIndex, long endIndex) throws RepositoryException {
            return null;
        }
    };

    private InMemoryRepository<GithubPushEvent, String> githubPushEventRepository = new InMemoryRepository<GithubPushEvent, String>() {
        @Override
        public PagedEntityList<GithubPushEvent> search(String query, long startIndex, long endIndex) throws RepositoryException {
            return null;
        }
    };



    public Repository<Pipeline, String> pipelineRepository() {
        return pipelineRepository;
    }

    public InMemoryRepository<GithubPushEvent, String> githubPushEventRepository() {
        return githubPushEventRepository;
    }
}
