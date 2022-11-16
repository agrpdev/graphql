package cz.atlascon.graphql.schemas.decorators;

import cz.atlascon.graphql.ng.GraphQLDesc;
import graphql.schema.GraphQLFieldDefinition.Builder;
import java.lang.reflect.Method;

public class DescriptionDecorator implements OutputDecorator {

    @Override
    public void decorate(final Builder fieldBuilder, final Method method) {
        if (method.isAnnotationPresent(GraphQLDesc.class)) {
            final String val = method.getAnnotation(GraphQLDesc.class).value();
            if (val != null) {
                fieldBuilder.description(val);
            }
        }
    }
}
