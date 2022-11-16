package cz.atlascon.graphql;

import cz.atlascon.graphql.ng.GraphQLField;

public class DemoObjExtended extends DemoObjImpl {

    private final String extra;

    DemoObjExtended(final int id,
                    final String val,
                    final String extra) {
        super(id, val);
        this.extra = extra;
    }

    @GraphQLField
    public String getExtra() {
        return extra;
    }
}
