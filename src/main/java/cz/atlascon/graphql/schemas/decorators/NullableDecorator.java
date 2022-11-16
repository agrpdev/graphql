package cz.atlascon.graphql.schemas.decorators;

import cz.atlascon.graphql.schemas.CommonSchemaGenerator;
import graphql.schema.GraphQLFieldDefinition.Builder;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;

public class NullableDecorator implements OutputDecorator {

    private final GraphQLOutputType fieldType;

    public NullableDecorator(final GraphQLOutputType fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public void decorate(final Builder fieldBuilder, final Method method) {
        if (method.isAnnotationPresent(Nonnull.class) && !(fieldType instanceof GraphQLNonNull)) {
            fieldBuilder.type(CommonSchemaGenerator.wrapNonNull(fieldType));
        } else {
            fieldBuilder.type(fieldType);
        }

    }
}
