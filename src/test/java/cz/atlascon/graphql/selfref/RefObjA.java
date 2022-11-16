package cz.atlascon.graphql.selfref;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.ng.GraphQLReference;
import cz.atlascon.graphql.ng.GraphQLTypeName;

@GraphQLTypeName("RefA")
public class RefObjA implements GraphQLReference<ObjA> {

    private final int id;

    @JsonCreator
    public RefObjA(@JsonProperty("id") final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }


}
