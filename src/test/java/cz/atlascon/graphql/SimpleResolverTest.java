package cz.atlascon.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLField;
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

public class SimpleResolverTest {

    public static class InputElement {

        private final InputElement parent;
        private final String name;

        @JsonCreator
        public InputElement(final @JsonProperty("parent") InputElement parent,
                            final @JsonProperty("name") String name) {
            this.parent = parent;
            this.name = name;
        }

        @GraphQLField
        public InputElement getParent() {
            return parent;
        }

        @GraphQLField
        public String getName() {
            return name;
        }
    }

    public static class Input {

        private final String desc;
        private final InputElement itm;
        private final List<List<InputElement>> itemz;

        @JsonCreator
        public Input(final @JsonProperty("desc") String desc,
                     final @JsonProperty("itm") InputElement itm,
                     final @JsonProperty("itemz") List<List<InputElement>> itemz) {
            this.desc = desc;
            this.itm = itm;
            this.itemz = itemz;
        }

        @GraphQLField
        public String getDesc() {
            return desc;
        }

        @GraphQLField
        public InputElement getItm() {
            return itm;
        }

        @GraphQLField
        public List<List<InputElement>> getItemz() {
            return itemz;
        }
    }

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addGqlResource(
                        new Object() {
                            @GraphQlQuery("simple")
                            public InputElement get(final @GraphQLParam("inputStr") String in) {
                                return new InputElement(new InputElement(null, "blabla" + in), "dziecko" + in);
                            }
                        }
                ).build();
    }

    @Test
    public void shouldParseInputListOfObjects() throws Exception {
        String response = query("{ simple(inputStr: \"inArg\") { parent { parent {name}, name}, name }} ");
        Assert
                .assertEquals(
                        "{\"data\":{\"simple\":{\"parent\":{\"parent\":null,\"name\":\"blablainArg\"},\"name\":\"dzieckoinArg\"}}}",
                        response);
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