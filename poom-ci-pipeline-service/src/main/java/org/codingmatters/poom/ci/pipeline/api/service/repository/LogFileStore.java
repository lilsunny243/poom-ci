package org.codingmatters.poom.ci.pipeline.api.service.repository;


import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLog;
import org.codingmatters.poom.ci.pipeline.api.service.storage.StageLogQuery;
import org.codingmatters.poom.ci.pipeline.api.types.LogLine;
import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.MutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.io.*;
import java.util.LinkedList;

public class LogFileStore implements LogStore {
    private final File dir;

    public LogFileStore(File dir) {
        this.dir = dir;
        this.dir.mkdirs();
    }

    @Override
    public Segment segment(String pipelineId, Stage.StageType stageType, String stageName) {
        return new FileSegment(pipelineId, stageType, stageName, this.dir);
    }

    static private String normalize(String level) {
        return level.replaceAll("\\s", "_").toLowerCase();
    }

    static public class FileSegment implements Segment {
        private final File file;
        private final String pipelineId;
        private final Stage.StageType stageType;
        private final String stageName;

        public FileSegment(String pipelineId, Stage.StageType stageType, String stageName, File dir) {
            this.pipelineId = pipelineId;
            this.stageType = stageType;
            this.stageName = stageName;
            this.file = new File(new File(new File(dir, normalize(pipelineId)), normalize(stageType.name())), normalize(stageName));
            this.file.getParentFile().mkdirs();
        }

        public void append(String ... lines) throws IOException {
            try(Writer writer = new FileWriter(this.file, true)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.write("\n");
                }
            }
        }

        @Override
        public PagedEntityList<StageLog> all(long startIndex, long endIndex) throws RepositoryException {
            LinkedList<Entity<StageLog>> result = new LinkedList<>();
            long current = 0;
            if(! this.file.exists()) {
                return new PagedEntityList.DefaultPagedEntityList<>(
                        0,
                        0,
                        0,
                        result);
            }
            try(BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
                for(String line = reader.readLine() ; line != null ; line = reader.readLine()) {
                    if(current >= startIndex && current <= endIndex) {
                        result.add(
                                new MutableEntity<>("" + current, StageLog.builder()
                                        .pipelineId(this.pipelineId)
                                        .stageType(this.stageType)
                                        .stageName(this.stageName)
                                        .log(LogLine.builder()
                                            .content(line)
                                            .line(current + 1).build()).build())
                        );
                    }
                    current++;
                }
            } catch (IOException e) {
                throw new RepositoryException("failed reading segment file " + this.file.getAbsolutePath(), e);
            }
            return new PagedEntityList.DefaultPagedEntityList<>(
                    startIndex,
                    startIndex + result.size() - 1,
                    current,
                    result);
        }

        @Override
        public PagedEntityList<StageLog> search(StageLogQuery query, long startIndex, long endIndex) throws RepositoryException {
            return all(startIndex, endIndex);
        }
    }


}
