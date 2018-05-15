package org.codingmatters.poom.ci.runners.pipeline.providers.gh;

public class Ref {
    private final String ref;

    public Ref(String ref) {
        this.ref = ref;
    }

    public String branch() {
        return this.ref.substring("refs/heads/".length());
    }
}
