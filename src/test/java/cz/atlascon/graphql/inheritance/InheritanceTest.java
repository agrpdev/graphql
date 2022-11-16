package cz.atlascon.graphql.inheritance;

import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLIgnore;
import cz.atlascon.graphql.ng.GraphQLTypeName;
import cz.atlascon.graphql.schemas.CommonSchemaGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

public class InheritanceTest {

    @GraphQLTypeName("BaseInterface")
    public interface BaseInterface {

        @GraphQLIgnore
        String getTitle();

        @GraphQLField
        default String getDefaultItem() {
            return "default";
        }

    }

    @GraphQLDto("ExtendedInterface")
    public interface ExtendedInterface extends BaseInterface {

        default String getExtendedItem() {
            return "extended";
        }
    }

    public static abstract class AbstractBase implements ExtendedInterface {

        @GraphQLIgnore
        @Override
        public String getTitle() {
            return "title";
        }

    }

    @GraphQLDto("ConcreteClass")
    public static class ConcreteClass extends AbstractBase {


    }

    @Test
    public void shouldContainAllMethods() {
        final List<Method> methods = CommonSchemaGenerator.getGraphQLMethods(ConcreteClass.class);
        Assert.assertEquals(2, methods.size());
    }


}
