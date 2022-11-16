package cz.atlascon.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.ng.AllowNullElements;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLResource;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.cachecontrol.CacheControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NullableCollectionsElementsTest {

    private GraphQL grapql;

    @GraphQLDto("TestInput")
    public static class TestInput {

        private final List<Integer> ids;

        @JsonCreator
        public TestInput(@Nonnull @AllowNullElements @JsonProperty("ids") List<Integer> ids) {
            this.ids = ids;
        }

        public List<Integer> getIds() {
            return ids;
        }
    }

    @GraphQLResource
    public static class TestResource {

        @GraphQlQuery("test")
        public String get(@GraphQLParam("arg") TestInput testInput) throws Exception {
            return testInput.getIds().toString();
        }

        @GraphQlQuery("testPrimitive")
        public String get(@AllowNullElements @GraphQLParam("arg") List<Integer> testInput) throws Exception {
            return testInput.toString();
        }

    }

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                // instead of classpath scan
                .addGqlResource(new TestResource())
                .build();
    }

    @Test
    public void shouldAcceptNulls() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ test(arg: {ids: [1,2,null,3]})  }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"test\":\"[1, 2, null, 3]\"}}",
                response);
    }

    @Test
    public void shouldAcceptNullsPrimitive() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ testPrimitive(arg: [1,2,null,3])  }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"testPrimitive\":\"[1, 2, null, 3]\"}}",
                response);
    }


}