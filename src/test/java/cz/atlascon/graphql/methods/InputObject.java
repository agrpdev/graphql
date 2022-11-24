package cz.atlascon.graphql.methods;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.invoke.ContextInject;

public class InputObject {

    private final String value;
    private final String injected;

    @JsonCreator
    public InputObject(@JsonProperty("value") String value,
                       @ContextInject String injected) {
        this.value = value;
        this.injected = injected;
    }

    public String getInjected() {
        return injected;
    }

    public String getValue() {
        return value;
    }
}
