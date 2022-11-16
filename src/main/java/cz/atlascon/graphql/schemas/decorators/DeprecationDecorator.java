package cz.atlascon.graphql.schemas.decorators;

import graphql.schema.GraphQLFieldDefinition.Builder;
import java.lang.reflect.Method;

public class DeprecationDecorator implements OutputDecorator {

    @Override
    public void decorate(final Builder fieldBuilder, final Method method) {
        if (method.isAnnotationPresent(Deprecated.class)) {
            fieldBuilder.deprecate("deprecated");
        }
    }
}
