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
import java.util.List;
import java.util.Map;

public class UnionTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addGqlResource(new UnionResolver())
                .addGqlResource(new UnionResolverRefs())
                .addGqlResource(new UnionResolverRefsIface())
                .addGqlResource(new DemoObjResolver())
                .build();
    }

    @Test
    public void shouldGetUnionRef() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "{ union2(arg : \"testinput\") { ... on cz_atlascon_graphql_DemoObjImpl { id, val } } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertTrue(!((Map) result.getData()).isEmpty());


    }

    @Test
    public void shouldGetUnionRefIface() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "{ union3(arg : \"testinput\") { ... on cz_atlascon_graphql_DemoObjImpl { id, val } } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertTrue(!((Map) result.getData()).isEmpty());


    }

    @Test
    public void shouldGetUnion() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "{ union(arg : \"testinput\") { ... on cz_atlascon_graphql_pojo_PojoObject { id, type, name(prependBs: false) } } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertTrue(!((Map) result.getData()).isEmpty());


    }

}