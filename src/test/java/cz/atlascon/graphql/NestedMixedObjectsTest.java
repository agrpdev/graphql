package cz.atlascon.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLField;
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

public class NestedMixedObjectsTest {

    public static class Nested {

        private final Map<String, Inner> mapf;

        public Nested(final Map<String, Inner> mapf) {
            this.mapf = mapf;
        }

        @GraphQLField
        public Map<String, Inner> getMapf() {
            return mapf;
        }

    }

    public static class Inner {

        private final String name;
        private final int cnt;
        private final List<Inner> kidz;

        public Inner(final String name,
                     final int cnt,
                     final List<Inner> kidz) {
            this.name = name;
            this.cnt = cnt;
            this.kidz = kidz;
        }

        @GraphQLField
        public List<Inner> getKidz() {
            return kidz;
        }

        @GraphQLField
        public String getName() {
            return name;
        }

        @GraphQLField
        public int getCnt() {
            return cnt;
        }
    }

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder().addGqlResource(
                new Object() {
                    @GraphQlQuery("nested")
                    public Nested get(final @GraphQLParam("arg") String ref) {
                        Map<String, Inner> m = ImmutableMap.of(
                                "a", new Inner("inner-a", 12, List.of(new Inner("a-a", 123, List.of()))),
                                "b", new Inner("inner-b", 22, List.of(new Inner("b-a", 223, List.of())))
                        );
                        return new Nested(m);
                    }
                }
        ).build();
    }

    @Test
    public void shouldGetNestedMixedItems() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ nested(arg: \"blablaa\") { mapf{key} } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"nested\":{\"mapf\":[{\"key\":\"a\"},{\"key\":\"b\"}]}}}",
                response);
    }
}