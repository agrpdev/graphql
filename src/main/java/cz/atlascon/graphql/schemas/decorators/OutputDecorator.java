package cz.atlascon.graphql.schemas.decorators;

import graphql.schema.GraphQLFieldDefinition;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface OutputDecorator {

    /**
     * Decorate graphql object field with extra information from java field
     */
    void decorate(GraphQLFieldDefinition.Builder fieldBuilder, Method metehod);

}
