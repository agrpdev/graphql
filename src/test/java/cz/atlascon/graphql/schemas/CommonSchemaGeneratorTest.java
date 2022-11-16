package cz.atlascon.graphql.schemas;

import cz.atlascon.graphql.DemoObjImplDto;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


public class CommonSchemaGeneratorTest {

    @Ignore
    @Test
    public void shouldParseMethod() throws NoSuchMethodException {
        Assert.assertFalse(CommonSchemaGenerator.isDtoMethod(DemoObjImplDto.class.getMethod("isVoidMethod")));
        Assert.assertFalse(CommonSchemaGenerator.isDtoMethod(DemoObjImplDto.class.getMethod("notAMethod")));

        Assert.assertTrue(CommonSchemaGenerator
            .isDtoMethod(DemoObjImplDto.class.getMethod("getItemzParam", int.class, int.class, int.class)));
        Assert.assertTrue(CommonSchemaGenerator.isDtoMethod(DemoObjImplDto.class.getMethod("getId")));
        Assert.assertTrue(CommonSchemaGenerator.isDtoMethod(DemoObjImplDto.class.getMethod("getVal")));
        Assert.assertTrue(CommonSchemaGenerator.isDtoMethod(DemoObjImplDto.class.getMethod("getList")));
    }

    @Ignore
    @Test
    public void getMethods() {
//        Assert.assertEquals(Set.of("getId", "getVal", "getList", "getItemzParam"),
//            CommonSchemaGenerator.getGraphQLMethods(DemoObjImplDto.class).stream().map(
//                Method::getName).collect(Collectors.toSet()));

        // TODO fix, this is bug
//        Assert.assertEquals(Set.of("getId", "getVal", "getList"),
//            CommonSchemaGenerator.getGraphQLMethods(DemoObjImpl.class).stream().map(
//                Method::getName).collect(Collectors.toSet()));
    }


}