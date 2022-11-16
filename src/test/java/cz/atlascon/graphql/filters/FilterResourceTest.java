package cz.atlascon.graphql.filters;

import cz.atlascon.graphql.GraphQLCreator;
import cz.atlascon.graphql.GraphqlRequest;
import cz.atlascon.graphql.invoke.GraphQLFilter;
import cz.atlascon.graphql.ng.resources.ResourceMethod;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.cachecontrol.CacheControl;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FilterResourceTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addFilter(new GraphQLFilter() {
                    @Override
                    public void onBeforeInvoke(DataFetchingEnvironment environment, Object source, ResourceMethod method, Object[] params) {

                    }
                })
                .addGqlResource(new FilterResource())
                .build();
    }

    @Test
    public void shouldGetConcreteItemFromUnionReference() throws Exception {
        Map map = new HashMap();
        map.put("query", "{ test(id: \"item0\") { " +
                "id " +
                "info " +
                "} }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(
                "{data={test={id=item0, info=blablabla}}}",
                result.toSpecification().toString());
    }

}
