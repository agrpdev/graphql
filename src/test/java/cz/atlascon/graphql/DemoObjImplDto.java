package cz.atlascon.graphql;

import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLParam;
import java.util.List;

@GraphQLDto
public class DemoObjImplDto implements DemoObj {

    private final int id;
    private final String val;

    DemoObjImplDto(final int id, final String val) {
        this.id = id;
        this.val = val;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getVal() {
        return val;
    }

    @Override
    public List<String> getList() {
        return null;
    }

    public List<String> getItemzParam(@GraphQLParam("arg0") int arg0,
                                      @GraphQLParam("arg1") int arg1,
                                      @GraphQLParam("arg2") int arg2) {
        return List.of("1", "2", "3");
    }

    public void isVoidMethod() {
        // not gql
    }

    public String notAMethod() {
        return "";
    }

}
