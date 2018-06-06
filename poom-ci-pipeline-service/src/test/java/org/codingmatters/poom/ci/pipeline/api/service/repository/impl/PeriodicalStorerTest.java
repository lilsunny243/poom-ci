package org.codingmatters.poom.ci.pipeline.api.service.repository.impl;

import org.codingmatters.poom.ci.pipeline.api.service.repository.PeriodicalOperator;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PeriodicalStorerTest extends AbstractBasedSegmentedRepositoryTest {

    static public final long STORAGE_DELAY = 200L;
    static public final TimeUnit STORAGE_DELAY_UNIT = TimeUnit.MILLISECONDS;

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private FileBasedSegmentedRepository<AbstractBasedSegmentedRepositoryTest.TestKey, String, String> segmentedRepository;

    private PeriodicalOperator storer;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Before
    public void setUp() throws Exception {
        this.segmentedRepository = createTestSegmentedRepository(this.dir.getRoot());
        this.storer = this.createStorer(STORAGE_DELAY, STORAGE_DELAY_UNIT).start();
    }

    @After
    public void tearDown() throws Exception {
        this.storer.stop();
    }

    @Test
    public void givenRepositoryChanged__whenStorageDelayIsPassed__thenRepositoryIsStored() throws Exception {
        AbstractBasedSegmentedRepositoryTest.TestKey key = new AbstractBasedSegmentedRepositoryTest.TestKey("test");
        Entity<String> stored = this.segmentedRepository.repository(key).create("created");

        assertThat("repository is not yet stored", createTestSegmentedRepository(this.dir.getRoot()).repository(key).retrieve(stored.id()), is(nullValue()));

        Thread.sleep(STORAGE_DELAY + 50L);
        assertThat("repository has been stored", createTestSegmentedRepository(this.dir.getRoot()).repository(key).retrieve(stored.id()), is(stored));

        Entity<String> modified = this.segmentedRepository.repository(key).update(stored, "changed");

        Thread.sleep(STORAGE_DELAY + 50L);
        assertThat("repository has been stored", createTestSegmentedRepository(this.dir.getRoot()).repository(key).retrieve(stored.id()), is(modified));
    }

    private PeriodicalOperator<AbstractBasedSegmentedRepositoryTest.TestKey, String, String> createStorer(long storageDelay, TimeUnit delayUnit) {
        return new PeriodicalOperator<>(
                this.segmentedRepository,
                PeriodicalOperator.PeriodicalOperations.storer(),
                this.scheduler,
                storageDelay,
                delayUnit);
    }
}