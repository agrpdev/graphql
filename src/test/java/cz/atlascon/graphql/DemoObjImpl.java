package cz.atlascon.graphql;

import cz.atlascon.graphql.ng.GraphQLField;
import java.util.List;

public class DemoObjImpl implements DemoObj {

    private final int id;
    private final String val;

    public DemoObjImpl(final int id, final String val) {
        this.id = id;
        this.val = val;
    }

    @GraphQLField
    @Override
    public int getId() {
        return id;
    }

    @GraphQLField
    @Override
    public String getVal() {
        return val;
    }

    @Override
    public List<String> getList() {
        return null;
    }
}
