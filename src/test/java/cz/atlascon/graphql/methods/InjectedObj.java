package cz.atlascon.graphql.methods;

public class InjectedObj {

    private final String value;

    public InjectedObj(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
