package cz.atlascon.graphql.selfref;

import com.fasterxml.jackson.databind.ObjectMapper;
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

public class SelfRefTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addOutputType(ObjA.class)
                .addOutputType(RefObjA.class)
                .addGqlResource(new ObjAResolver())
                .build();
    }

    @Test
    public void shouldParseInputListOfObjects() throws Exception {
        String response = query("{ resolveA(arg: {id: 123}) { id } } ");
        Assert.assertEquals("{\"data\":{\"resolveA\":{\"id\":123}}}", response);
    }


    private String query(final String query) throws Exception {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", query);
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        return response;
    }

}