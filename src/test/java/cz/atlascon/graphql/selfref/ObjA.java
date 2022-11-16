package cz.atlascon.graphql.selfref;

import com.google.common.collect.Maps;
import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLTypeName;

import java.util.Map;

@GraphQLTypeName("TypeA")
@GraphQLDto
public class ObjA {

    private final int id;
    private final Map<String, ObjA> kidz = Maps.newConcurrentMap();
    private final String name;

    public ObjA(final int id, final String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public Map<String, ObjA> getKidz() {
        return kidz;
    }

    public String getName() {
        return name;
    }
}
