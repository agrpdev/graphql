package cz.atlascon.graphql.schemas.types;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import graphql.schema.GraphQLType;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CompositeGraphQLTypeFactory implements GraphQLTypeFactory {

    private final Map<Type, GraphQLType> cache = Maps.newHashMap();
    private final List<GraphQLTypeFactory> factoryList;

    public static CompositeGraphQLTypeFactory createDefault() {
        return new CompositeGraphQLTypeFactory(List.of(
                new DatesTypeFactory(),
                new EnumTypeFactory(),
                new PrimitiveTypeFactory()
        ));
    }

    public CompositeGraphQLTypeFactory(final List<GraphQLTypeFactory> factories) {
        Preconditions.checkNotNull(factories);
        factoryList = Lists.newArrayList(factories);
    }

    public void add(GraphQLTypeFactory typeFactory) {
        Preconditions.checkNotNull(typeFactory);
        factoryList.add(typeFactory);
    }

    @Override
    public Optional<GraphQLType> createType(final Type type) {
        if (cache.containsKey(type)) {
            return Optional.of(cache.get(type));
        }
        for (GraphQLTypeFactory f : factoryList) {
            final Optional<GraphQLType> t = f.createType(type);
            if (t.isPresent()) {
                cache.putIfAbsent(type, t.get());
                return t;
            }
        }
        return Optional.empty();
    }
}
