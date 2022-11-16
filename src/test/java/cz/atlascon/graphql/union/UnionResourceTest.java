package cz.atlascon.graphql.union;

import cz.atlascon.graphql.GraphQLCreator;
import cz.atlascon.graphql.GraphqlRequest;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.cachecontrol.CacheControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class UnionResourceTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addGqlResource(new UnionResource())
                .build();
    }

    @Test
    public void shouldGetConcreteItemFromUnionReference() throws Exception {
        Map map = new HashMap();
        map.put("query", "{ item(id: \"item0\") { " +
                "id " +
                "val {" +
                "   ... on ConcreteUnionType {" +
                "       id " +
                "       info" +
                "   }" +
                "}" +
                "} }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(
                "{data={item={id=item0, val={id=val-item0, info=UnionReference{id='val-item0'}}}}}",
                result.toSpecification().toString());
    }

}
