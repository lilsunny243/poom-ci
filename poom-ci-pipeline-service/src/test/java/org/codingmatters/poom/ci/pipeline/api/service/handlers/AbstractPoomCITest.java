package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;

public class AbstractPoomCITest {

    private PoomCIRepository inMemory = PoomCIRepository.inMemory();

    public PoomCIRepository repository() {
        return inMemory;
    }

}
