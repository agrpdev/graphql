package cz.atlascon.graphql.iface;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.GraphQLCreator;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.GraphqlRequest;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLReference;
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

public class InterfaceReferenceTest {

    private GraphQL grapql;

    @GraphQLDto("ObjectIface")
    public interface ObjectIface {
        int getId();

        String getName();
    }

    @GraphQLDto("ObjectDtoId")
    public static class ObjectDtoId implements GraphQLReference<ObjectIface> {

        private final int id;

        @JsonCreator
        public ObjectDtoId(@Nonnull @JsonProperty("id") int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

    }

    @GraphQLDto("ObjectImpl")
    public static class ObjectIml implements ObjectIface {

        private final String name;
        private final int id;

        public ObjectIml(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getId() {
            return id;
        }
    }

    @GraphQLResource
    public static class TestResource {

        @GraphQlQuery("getItems")
        public List<ObjectDtoId> get() throws Exception {
            return List.of(new ObjectDtoId(1), new ObjectDtoId(2), new ObjectDtoId(3));
        }

        @GraphQlQuery("resolveItem")
        public ObjectIml resolve(@GraphQLParam("ref") ObjectDtoId ref) {
            return new ObjectIml("obj-" + ref.id, ref.id);
        }

    }

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                // instead of classpath scan
                .addOutputTypes(List.of(ObjectIml.class, ObjectIface.class))
                .addGqlResource(new TestResource())
                .build();
    }

    @Test
    public void shouldReturnNoImplRef() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ getItems { id } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"getItems\":[{\"id\":1},{\"id\":2},{\"id\":3}]}}",
                response);
    }


}