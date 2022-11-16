package cz.atlascon.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLReference;

public class DemoObjRef implements GraphQLReference<DemoObjImpl> {

    private final int id;

    @JsonCreator
    public DemoObjRef(@JsonProperty("id") final int id) {
        this.id = id;
    }

    @GraphQLField
    public int getId() {
        return id;
    }
}
