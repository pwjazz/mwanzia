package org.mwanzia.test;

import org.mwanzia.JsonInclude;

public enum State {
    CA("California"), NY("New York"), TX("Texas");

    private String name;

    private State(String name) {
        this.name = name;
    }

    @JsonInclude
    public String getName() {
        return this.name;
    }
}
