package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.Repository;

public interface RelationProcessor {
    Repository[] process(String repositoryId) throws NoSuchRepositoryException, GraphManagerException;
}
