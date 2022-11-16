package cz.atlascon.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLReference;

public class DemoObjIfaceRef implements GraphQLReference<DemoObj> {

    private final int id;

    @JsonCreator
    public DemoObjIfaceRef(@JsonProperty("id") final int id) {
        this.id = id;
    }

    @GraphQLField
    public int getId() {
        return id;
    }
}
