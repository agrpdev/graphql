package cz.atlascon.graphql.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLReference;

public class PojoRequest implements GraphQLReference<PojoObject> {

    private final int id;
    private final Boolean bs;

    @JsonCreator
    public PojoRequest(@JsonProperty("id") final int id,
                       @JsonProperty("bs") final Boolean bs) {
        this.id = id;
        this.bs = bs;
    }

    @GraphQLField
    public int getId() {
        return id;
    }

    @GraphQLField
    public Boolean isBs() {
        return bs;
    }

}
