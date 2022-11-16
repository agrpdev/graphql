package cz.atlascon.graphql.ng;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface GraphQLParam {

    String DEFAULT_ARG_NAME = "arg";

    String value() default "";

    String description() default "";

}
