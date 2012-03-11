package org.mwanzia.test;

import org.codehaus.jackson.annotate.JsonProperty;

public enum State {
    CA("California"), NY("New York"), TX("Texas");

    private String name;

    private State(String name) {
        this.name = name;
    }

    @JsonProperty
    public String getName() {
        return this.name;
    }
}
