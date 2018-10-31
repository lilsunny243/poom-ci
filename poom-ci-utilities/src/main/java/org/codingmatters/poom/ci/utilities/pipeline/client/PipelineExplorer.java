package org.codingmatters.poom.ci.utilities.pipeline.client;

import org.codingmatters.poom.ci.pipeline.api.types.Stage;
import org.codingmatters.poom.ci.utilities.pipeline.client.actions.LogsReader;
import org.codingmatters.poom.ci.utilities.pipeline.client.actions.PipelinesReader;
import org.codingmatters.poom.ci.utilities.pipeline.client.actions.StagesReader;

import java.util.Optional;

public class PipelineExplorer {
    public static void main(String[] args) {
        if(args.length <= 0) {
            throw new RuntimeException("usage : <base url> {pipeline id} {stage type} {stage name}");
        }

        //https://pipelines.ci.flexio.io/pipelines/v1/
        String baseUrl = argOrNull(args, 0).get();
        Optional<String> pipeline = argOrNull(args, 1);
        Optional<String> stageType = argOrNull(args, 2);
        Optional<String> stage = argOrNull(args, 3);

        Action action = Action.from(baseUrl, pipeline, stageType, stage);
        try {
            action.run(baseUrl, pipeline, stageType, stage);
        } catch (Exception e) {
            throw new RuntimeException("error running action " + action, e);
        }
    }

    private static Optional<String> argOrNull(String[] args, int index) {
        return Optional.ofNullable(args.length > index ? args[index] : null);
    }

    enum Action implements PipelineExplorerRunnable {
        NOOP {
            @Override
            public void run(String baseUrl, Optional<String> pipeline, Optional<String> stageType, Optional<String> stage) throws Exception {
            }
        },
        PRINT_PIPELINES {
            @Override
            public void run(String baseUrl, Optional<String> pipeline, Optional<String> stageType, Optional<String> stage) throws Exception {
                new PipelinesReader(baseUrl).readPipelines(
                        (pipe, trigger) -> {
                            System.out.printf("%s (%s) - %s ()\n", pipe.id(), pipe.status(), pipe.name(), pipe.trigger().type());
                        }
                );
            }
        },
        PRINT_STAGES {
            @Override
            public void run(String baseUrl, Optional<String> pipeline, Optional<String> stageType, Optional<String> stage) throws Exception {
                System.out.println("stages for " + stageType.get());
                new StagesReader(baseUrl, pipeline.get(), stageType.get()).readStages(st -> System.out.printf(
                        "\t%s (%s / %s)\n", st.name(), st.status().run(), st.status().exit(), st.status()
                ));
            }
        },
        PRINT_ALL_STAGES {
            @Override
            public void run(String baseUrl, Optional<String> pipeline, Optional<String> stageType, Optional<String> stage) throws Exception {
                for (Stage.StageType type : Stage.StageType.values()) {
                    PRINT_STAGES.run(baseUrl, pipeline, Optional.of(type.name().toLowerCase()), stage);
                }
            }
        },
        PRINT_LOGS {
            @Override
            public void run(String baseUrl, Optional<String> pipeline, Optional<String> stageType, Optional<String> stage) throws Exception {
                new LogsReader(baseUrl, pipeline.get(), stageType.get(), stage.get()).readLines(
                        logLine -> System.out.printf("%04d %s\n", logLine.line(), logLine.content())
                );
            }
        };

        static public Action from(String baseUrl, Optional<String> pipeline, Optional<String> stageType, Optional<String> stage) {
            if(! pipeline.isPresent()) {
                return PRINT_PIPELINES;
            }
            if(! stageType.isPresent()) {
                return PRINT_ALL_STAGES;
            }
            if(! stage.isPresent()) {
                return PRINT_STAGES;
            }
            return PRINT_LOGS;
        }
    }

    @FunctionalInterface
    interface PipelineExplorerRunnable {
        void run(String baseUrl, Optional<String> pipeline, Optional<String> stageType, Optional<String> stage) throws Exception;
    }
}
