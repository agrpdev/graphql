package cz.atlascon.graphql.schemas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLReference;
import cz.atlascon.graphql.schemas.PojoOutputSchemaGeneratorTest.ExtendedItem;

public class ExtendedItemRef implements GraphQLReference<ExtendedItem> {

    private final int docId;

    @JsonCreator
    public ExtendedItemRef(@JsonProperty("docId") final int docId) {
        this.docId = docId;
    }

    @GraphQLField
    public int getDocId() {
        return docId;
    }
}
