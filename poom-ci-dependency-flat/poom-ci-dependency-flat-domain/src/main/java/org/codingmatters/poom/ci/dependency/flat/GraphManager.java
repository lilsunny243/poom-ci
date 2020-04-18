package org.codingmatters.poom.ci.dependency.flat;

import org.codingmatters.poom.ci.dependency.api.types.FullRepository;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.DependsOnRelation;
import org.codingmatters.poom.ci.dependency.flat.domain.spec.ProducesRelation;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.RepositoryIterator;
import org.codingmatters.poom.servives.domain.entities.Entity;

public class GraphManager {

    public static final int DEFAULT_PAGE_SIZE = 1000;
    private final Repository<org.codingmatters.poom.ci.dependency.api.types.Repository, PropertyQuery> repositories;
    private final Repository<ProducesRelation, PropertyQuery> producesRelation;
    private final Repository<DependsOnRelation, PropertyQuery> dependsOnRelation;
    private final int pageSize;

    public GraphManager(
            Repository<org.codingmatters.poom.ci.dependency.api.types.Repository, PropertyQuery> repositories,
            Repository<ProducesRelation, PropertyQuery> producesRelation,
            Repository<DependsOnRelation, PropertyQuery> dependsOnRelation) {
        this(repositories, producesRelation, dependsOnRelation, DEFAULT_PAGE_SIZE);
    }

    public GraphManager(Repository<org.codingmatters.poom.ci.dependency.api.types.Repository, PropertyQuery> repositories, Repository<ProducesRelation, PropertyQuery> producesRelation, Repository<DependsOnRelation, PropertyQuery> dependsOnRelation, int pageSize) {
        this.repositories = repositories;
        this.producesRelation = producesRelation;
        this.dependsOnRelation = dependsOnRelation;
        this.pageSize = pageSize;
    }

    public GraphManager index(FullRepository fullRepository) throws GraphManagerException {
        try {
            this.cleanup(fullRepository);
        } catch (RepositoryException e) {
            throw new GraphManagerException("failed cleaning for repository indexing", e);
        }
        try {
            Entity<org.codingmatters.poom.ci.dependency.api.types.Repository> repositoryEntity = this.repositories.createWithId(fullRepository.id(), org.codingmatters.poom.ci.dependency.api.types.Repository.builder()
                    .id(fullRepository.id())
                    .name(fullRepository.name())
                    .checkoutSpec(fullRepository.checkoutSpec())
                    .build());

            if(fullRepository.produces() != null) {
                for (Module produce : fullRepository.produces()) {
                    this.producesRelation.create(ProducesRelation.builder()
                            .repository(repositoryEntity.value())
                            .module(produce)
                            .build());
                }
            }
            if(fullRepository.dependencies() != null) {
                for (Module produce : fullRepository.dependencies()) {
                    this.dependsOnRelation.create(DependsOnRelation.builder()
                            .repository(repositoryEntity.value())
                            .module(produce)
                            .build());
                }
            }

        } catch (RepositoryException e) {
            throw new GraphManagerException("failed indexing graph", e);
        }
        return this;
    }

    private void cleanup(FullRepository fullRepository) throws RepositoryException {
        this.repositories.deleteFrom(PropertyQuery.builder().filter(String.format("id == '%s'", fullRepository.id())).build());
        this.producesRelation.deleteFrom(PropertyQuery.builder().filter(String.format("repository.id == '%s'", fullRepository.id())).build());
        this.dependsOnRelation.deleteFrom(PropertyQuery.builder().filter(String.format("repository.id == '%s'", fullRepository.id())).build());
    }

    public Module[] producedBy(org.codingmatters.poom.ci.dependency.api.types.Repository repository) throws GraphManagerException {
        try {
            Entity<org.codingmatters.poom.ci.dependency.api.types.Repository> repoEntity = this.repositories.retrieve(repository.id());
            if(repoEntity != null) {
                return RepositoryIterator.searchStreamed(this.producesRelation, this.relatedToRepository(repository), this.pageSize)
                        .map(rel -> rel.value().module())
                        .toArray(size -> new Module[size]);
            } else {
                throw new GraphManagerException("no such repository : " + repository);
            }
        } catch (RepositoryException e) {
            throw new GraphManagerException("error accessing repositories repository", e);
        }
    }

    public Module[] dependenciesOf(org.codingmatters.poom.ci.dependency.api.types.Repository repository) throws GraphManagerException {
        try {
            Entity<org.codingmatters.poom.ci.dependency.api.types.Repository> repoEntity = this.repositories.retrieve(repository.id());
            if(repoEntity != null) {
                return RepositoryIterator.searchStreamed(this.dependsOnRelation, this.relatedToRepository(repository), this.pageSize)
                        .map(rel -> rel.value().module())
                        .toArray(size -> new Module[size]);
            } else {
                throw new GraphManagerException("no such repository : " + repository);
            }
        } catch (RepositoryException e) {
            throw new GraphManagerException("error accessing repositories repository", e);
        }
    }

    private PropertyQuery relatedToRepository(org.codingmatters.poom.ci.dependency.api.types.Repository repository) {
        return PropertyQuery.builder().filter(String.format("repository.id == '%s'", repository.id())).build();
    }
}
