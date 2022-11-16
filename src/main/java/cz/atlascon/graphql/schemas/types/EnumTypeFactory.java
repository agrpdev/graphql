package cz.atlascon.graphql.schemas.types;

import cz.atlascon.graphql.common.Common;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLEnumValueDefinition.Builder;
import graphql.schema.GraphQLType;

import java.lang.reflect.Type;
import java.util.Optional;

public class EnumTypeFactory implements GraphQLTypeFactory {

    @Override
    public Optional<GraphQLType> createType(final Type type) {
        if (type instanceof Class && ((Class) type).isEnum()) {
            return Optional.of(createEnumType((Class) type));
        }
        return Optional.empty();
    }


    private GraphQLEnumType createEnumType(Class enumClz) {
        final String eName = Common.getName(enumClz);
        try {
            GraphQLEnumType.Builder enumQL = GraphQLEnumType.newEnum().name(eName);
            for (Object constant : enumClz.getEnumConstants()) {
                final String constantName = (String) enumClz.getMethod("name").invoke(constant);
                final boolean deprecated = enumClz.getField(constantName)
                        .isAnnotationPresent(Deprecated.class);
                final Builder valueBuilder = GraphQLEnumValueDefinition.newEnumValueDefinition()
                        .name(constantName)
                        .value(constant);
                if (deprecated) {
                    valueBuilder.deprecationReason("Deprecated in class " + enumClz.getName());
                }
                enumQL.value(valueBuilder.build());
            }
            return enumQL.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
