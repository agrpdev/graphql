package cz.atlascon.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.ng.GraphQLParam;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.cachecontrol.CacheControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReferenceReturnTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder().addGqlResource(
                new Object() {
                    @GraphQlQuery("myObjects")
                    public List<DemoObjRef> get() {
                        return List.of(new DemoObjRef(666), new DemoObjRef(665));
                    }
                }).addGqlResource(
                new Object() {
                    @GraphQlQuery("myObj")
                    public DemoObjRef get() {
                        return new DemoObjRef(666);
                    }
                }).addGqlResource(
                new Object() {
                    @GraphQlQuery("demoObj")
                    public DemoObjImpl get(final @GraphQLParam("arg") DemoObjRef ref) {
                        return new DemoObjImpl(ref.getId(), ref.getId() + " - was referenced ID");
                    }
                }
        ).build();
    }

    @Test
    public void shouldTranslateReference() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ myObj { id, val } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"myObj\":{\"id\":666,\"val\":\"666 - was referenced ID\"}}}",
                response);
    }

    @Test
    public void shouldTranslateCollection() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ myObjects { id, val } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"myObjects\":[{\"id\":666,\"val\":\"666 - was referenced ID\"},{\"id\":665,\"val\":\"665 - was referenced ID\"}]}}",
                response);
    }

}