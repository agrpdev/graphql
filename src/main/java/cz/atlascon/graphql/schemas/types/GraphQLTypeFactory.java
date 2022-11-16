package cz.atlascon.graphql.schemas.types;

import graphql.schema.GraphQLType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface GraphQLTypeFactory {

    Optional<GraphQLType> createType(Type type);

}
