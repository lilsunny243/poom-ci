package org.codingmatters.poom.ci.github.webhook;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GithubEventFilter implements Processor {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubEventFilter.class);

    static public String EVENT_HEADER = "X-GitHub-Event";

    private final Map<String, Processor> processors = new HashMap<>();
    private final Processor defaultProcessor;


    public GithubEventFilter(Processor defaultProcessor) {
        this.defaultProcessor = defaultProcessor;
    }

    public GithubEventFilter with(String event, Processor processor) {
        this.processors.put(event, processor);
        return this;
    }

    @Override
    public void process(RequestDelegate request, ResponseDelegate response) throws IOException {
        Optional<String> event = this.event(request);
        if(event.isPresent()) {
            if(this.processors.containsKey(event.get())) {
                this.processors.get(event.get()).process(request, response);
            } else {
                this.defaultProcessor.process(request, response);
            }
        } else {
            this.defaultProcessor.process(request, response);
        }
    }

    private Optional<String> event(RequestDelegate request) {
        if(! request.headers().get(EVENT_HEADER).isEmpty()) {
            return Optional.of(request.headers().get(EVENT_HEADER).get(0));
        } else {
            return Optional.empty();
        }
    }
}
