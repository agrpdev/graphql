package cz.atlascon.graphql.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLParam;

import javax.annotation.Nonnull;
import java.util.List;

public class PojoObject {

    public enum Type {
        NORMAL, SPECIAL
    }

    private final int id;
    @Nonnull
    private final Type type;
    @Nonnull
    private final String name;

    @JsonCreator
    public PojoObject(@JsonProperty("id") final int id,
                      @JsonProperty("type") @Nonnull final Type type,
                      @JsonProperty("name") @Nonnull final String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    @GraphQLField
    public List<String> getInfoList() {
        return List.of();
    }

    @GraphQLField
    public List<String> getInfoList2() {
        return List.of();
    }

    @GraphQLField
    public List<String> getInfoList3() {
        return List.of();
    }

    @GraphQLField
    public int getId() {
        return id;
    }

    @GraphQLField
    @Nonnull
    public Type getType() {
        return type;
    }

    @GraphQLField
    @Nonnull
    public String getName(@GraphQLParam("prependBs") boolean prependBs) {
        return prependBs ? "BS_" + name : name;
    }
}
