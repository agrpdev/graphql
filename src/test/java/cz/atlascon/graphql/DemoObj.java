package cz.atlascon.graphql;

import cz.atlascon.graphql.ng.GraphQLField;
import java.util.List;

public interface DemoObj {

    @GraphQLField
    int getId();

    @GraphQLField
    String getVal();

    @GraphQLField
    List<String> getList();

}
