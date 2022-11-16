package cz.atlascon.graphql.union;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.*;

import javax.annotation.Nonnull;

@GraphQLResource
public class UnionResource {

    @GraphQLDto("ConcreteUnionType")
    public static class ConcreteUnionType {
        private final String id;
        private final String info;

        public ConcreteUnionType(String id, String info) {
            this.id = id;
            this.info = info;
        }

        public String getId() {
            return id;
        }

        public String getInfo() {
            return info;
        }
    }

    @GraphQLDto("Union")
    @GraphQLReturnTypes(values = ConcreteUnionType.class)
    public static class Union extends AbstractGraphQLUnion {

        public Union(Object value) {
            super(value);
        }

    }

    @GraphQLDto("UnionReference")
    public static class UnionReference implements GraphQLReference<Union> {

        private final String id;

        // TODO allow non-json-creator constructors for internal graphql references
        @JsonCreator
        public UnionReference(@JsonProperty("id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "UnionReference{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }

    @GraphQLDto("Item")
    public static class Item {
        private final String id;

        public Item(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public UnionReference getVal() {
            return new UnionReference("val-" + id);
        }
    }

    @GraphQlQuery(value = "union", isPublic = false)
    public Union resolve(@GraphQLParam("ref") final UnionReference ref) {
        return new Union(new ConcreteUnionType(ref.getId(), ref.toString()));
    }

    @GraphQlQuery("item")
    public Item get(@Nonnull @GraphQLParam("id") final String id) {
        return new Item(id);
    }
}
