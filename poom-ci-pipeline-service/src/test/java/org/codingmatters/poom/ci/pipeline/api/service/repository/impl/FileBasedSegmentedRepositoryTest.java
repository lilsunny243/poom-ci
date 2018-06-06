package org.codingmatters.poom.ci.pipeline.api.service.repository.impl;

import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.codingmatters.poom.ci.pipeline.api.service.repository.impl.FileBasedSegmentedRepository.storageFilename;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileBasedSegmentedRepositoryTest extends AbstractBasedSegmentedRepositoryTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File storageDir;
    private FileBasedSegmentedRepository<TestKey, String, String> segmentedRepository;

    @Before
    public void setUp() throws Exception {
        this.storageDir = this.dir.newFolder();
        this.segmentedRepository = createTestSegmentedRepository(this.storageDir);
    }

    @Test
    public void givenStorageFolderDoesntExists__whenCreatingRepository__thenStorageFolderIsCreated() throws IOException {
        File unexistentStorageDir = new File(this.dir.getRoot(), "unexistent-storage");
        createTestSegmentedRepository(unexistentStorageDir);

        assertTrue(unexistentStorageDir.exists());
    }

    @Test
    public void givenStorageFolderIsAFile__whenCreatingRepository__thenIOExceptionIsThrown() throws IOException {
        File aFile = new File(this.dir.getRoot(), "a-file");
        aFile.createNewFile();

        this.thrown.expect(IOException.class);
        this.thrown.expectMessage(is("storage dir must be a directory"));

        createTestSegmentedRepository(aFile);
    }

    @Test
    public void givenUsingTheSameKey__whenAddingElement__thenElementsAreStored() throws Exception {
        this.segmentedRepository.repository(new TestKey("test")).create("one");
        this.segmentedRepository.repository(new TestKey("test")).create("two");
        this.segmentedRepository.repository(new TestKey("test")).create("three");

        assertThat(this.segmentedRepository.repository(new TestKey("test")).all(0, 10000).valueList(), contains("one", "two", "three"));
    }

    @Test
    public void givenARepositoryIsPopulated__whenSegmentedRepositoryIsStored__thenAFileIsCreated() throws Exception {
        Entity<String> one = this.segmentedRepository.repository(new TestKey("test")).create("one");
        Entity<String> two = this.segmentedRepository.repository(new TestKey("test")).create("two");
        Entity<String> three = this.segmentedRepository.repository(new TestKey("test")).create("three");

        this.segmentedRepository.store();

        File storageFile = new File(this.storageDir, storageFilename(new TestKey("test")));
        assertTrue(storageFile.exists());
        assertThat(
                this.content(storageFile),
                is(
                        one.id() + "\n" + one.version() + "\n" +
                        "one\n" +
                        two.id() + "\n" + two.version()+ "\n" +
                        "two\n" +
                        three.id() + "\n" + three.version()+ "\n" +
                        "three\n")
        );
    }

    @Test
    public void givenRepositoryStorageExists__whenRepositoryRequested__thenRepositoryIsLoadedFromStorage() throws Exception {
        File storageFile = new File(this.storageDir, storageFilename(new TestKey("test")));
        this.contentToFile(
                "001\n1\none\n002\n2\ntwo\n003\n3\nthree\n",
                storageFile);

        Repository<String, String> repository = this.segmentedRepository.repository(new TestKey("test"));
        assertThat(repository.all(0, 10000).valueList(), contains("one", "two", "three"));
        assertThat(repository.retrieve("001").value(), is("one"));
        assertThat(repository.retrieve("001").version().toString(), is("1"));
        assertThat(repository.retrieve("002").value(), is("two"));
        assertThat(repository.retrieve("002").version().toString(), is("2"));
        assertThat(repository.retrieve("003").value(), is("three"));
        assertThat(repository.retrieve("003").version().toString(), is("3"));
    }

    @Test
    public void givenTwoRepositoriesAreStored__whenOneIsChanged__thenOnlyTheChangedOneIsStored() throws Exception {
        Repository<String, String> changed = this.segmentedRepository.repository(new TestKey("changed"));
        Repository<String, String> unchanged = this.segmentedRepository.repository(new TestKey("unchanged"));

        changed.create("created");
        unchanged.create("created");

        this.segmentedRepository.store();

        long changedLastModified = new File(this.storageDir, FileBasedSegmentedRepository.storageFilename(new TestKey("changed"))).lastModified();
        long unchangedLastModified = new File(this.storageDir, FileBasedSegmentedRepository.storageFilename(new TestKey("unchanged"))).lastModified();

        changed.create("added");

        Thread.sleep(1000L);
        this.segmentedRepository.store();

        assertThat(
                "changed repository was stored",
                new File(this.storageDir, FileBasedSegmentedRepository.storageFilename(new TestKey("changed"))).lastModified(),
                is(greaterThan(changedLastModified))
        );
        assertThat(
                "unchanged repository was not stored",
                new File(this.storageDir, FileBasedSegmentedRepository.storageFilename(new TestKey("unchanged"))).lastModified(),
                is(unchangedLastModified)
        );
    }

    @Test
    public void givenRepositoryNotAccessedSinceTTL__whenPurgeIsCalled__thenRepositoryIsUnloaded() throws Exception {
        this.segmentedRepository.repository(new TestKey("accessed"));
        this.segmentedRepository.repository(new TestKey("unaccessed"));

        assertThat(this.segmentedRepository.loadedRepositoryCount(), is(2));

        Thread.sleep(500L);
        this.segmentedRepository.repository(new TestKey("accessed"));
        this.segmentedRepository.purge(200L);

        assertThat(this.segmentedRepository.loadedRepositoryCount(), is(1));

        Thread.sleep(500L);
        this.segmentedRepository.purge(200L);

        assertThat(this.segmentedRepository.loadedRepositoryCount(), is(0));
    }
}