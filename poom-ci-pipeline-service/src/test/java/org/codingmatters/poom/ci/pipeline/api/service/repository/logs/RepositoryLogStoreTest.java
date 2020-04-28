package org.codingmatters.poom.ci.pipeline.api.service.repository.logs;

import org.codingmatters.poom.ci.pipeline.api.service.repository.LogStore;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.DateMatchers;
import org.codingmatters.poom.services.tests.Eventually;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;


public class RepositoryLogStoreTest {

    private final Repository<StageLog, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(StageLog.class);
    private final RepositoryLogStore store = new RepositoryLogStore(this.repository);

    @Test
    public void givenNoLogStored__whenGettingStageLog__thenNoLogReturned() throws Exception {
        assertThat(this.store.segment("pipeline-id", Stage.StageType.MAIN, "a-stage").all(0, 0).total(), is(0L));
    }

    @Test
    public void givenNoLogStored__whenStoringOneLineInStage__thenOneLineStoredAtIndex1() throws Exception {
        LogStore.Segment segment = this.store.segment("pipeline-id", Stage.StageType.MAIN, "a-stage");
        segment.append("a log line");

        Eventually.defaults().assertThat(() ->this.repository.all(0, 1000).total(), is(1L));

        StageLog actual = this.repository.all(0, 0).valueList().get(0);
        assertThat(actual.pipelineId(), is("pipeline-id"));
        assertThat(actual.stageType(), is(Stage.StageType.MAIN));
        assertThat(actual.stageName(), is("a-stage"));
        assertThat(actual.when(), is(around(UTC.now())));

        assertThat(actual.log().line(), is(1L));
        assertThat(actual.log().content(), is("a log line"));
    }

    @Test
    public void givenNoLogStored__whenStoringTwoLineInStage__thenTwoLinesStoredAtIndex1and2() throws Exception {
        LogStore.Segment segment = this.store.segment("pipeline-id", Stage.StageType.MAIN, "a-stage");
        segment.append("a log line", "another log line");

        Eventually.defaults().assertThat(() -> this.repository.all(0, 1000).total(), is(2L));

        List<StageLog> actual = this.repository.all(0, 1).valueList();

        assertThat(actual.get(0).log().line(), is(1L));
        assertThat(actual.get(0).log().content(), is("a log line"));
        assertThat(actual.get(1).log().line(), is(2L));
        assertThat(actual.get(1).log().content(), is("another log line"));
    }

    @Test
    public void givenNoLogStored__whenStoringOneLineThenAnotherInStage__thenTwoLinesStoredAtIndex1and2() throws Exception {
        LogStore.Segment segment = this.store.segment("pipeline-id", Stage.StageType.MAIN, "a-stage");
        segment.append("a log line");
        segment.append("another log line");

        Eventually.defaults().assertThat(() ->this.repository.all(0, 1000).total(), is(2L));

        List<StageLog> actual = this.repository.all(0, 1).valueList();

        assertThat(actual.get(0).log().line(), is(1L));
        assertThat(actual.get(0).log().content(), is("a log line"));
        assertThat(actual.get(1).log().line(), is(2L));
        assertThat(actual.get(1).log().content(), is("another log line"));
    }

    @Test
    public void givenNoLogStored__whenStoringTwoLogsInDifferentSegments__thenTwoLogsStored_andIndexAreIndependent() throws Exception {
        this.store.segment("pipeline-id", Stage.StageType.MAIN, "a-stage").append("a log line");
        this.store.segment("pipeline-id", Stage.StageType.SUCCESS, "a-stage").append("another log line");

        Eventually.defaults().assertThat(() ->this.repository.all(0, 1000).total(), is(2L));

        List<StageLog> actual = this.repository.all(0, 1).valueList();

        assertThat(actual.get(0).stageType(), is(Stage.StageType.MAIN));
        assertThat(actual.get(0).log().line(), is(1L));
        assertThat(actual.get(0).log().content(), is("a log line"));

        assertThat(actual.get(1).stageType(), is(Stage.StageType.SUCCESS));
        assertThat(actual.get(1).log().line(), is(1L));
        assertThat(actual.get(1).log().content(), is("another log line"));
    }

    @Test
    public void givenTwoLogsStoredInDifferentSegments__whenGettingStageLogs__thenOneLogInEachSegment() throws Exception {
        this.store.segment("pipeline-id", Stage.StageType.MAIN, "a-stage").append("a log line");
        this.store.segment("pipeline-id", Stage.StageType.SUCCESS, "a-stage").append("another log line");

        Eventually.defaults().assertThat(() ->this.store.segment("pipeline-id", Stage.StageType.MAIN, "a-stage").all(0, 0).total(), is(1L));
        Eventually.defaults().assertThat(() ->this.store.segment("pipeline-id", Stage.StageType.SUCCESS, "a-stage").all(0, 0).total(), is(1L));
    }
}