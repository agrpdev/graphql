package cz.atlascon.graphql.ng.resources;

import cz.atlascon.graphql.methods.TestGQLResource;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResourceProcessorTest {

    @Test
    public void resTest() {
        final ResourceProcessor processor = new ResourceProcessor(new TestGQLResource());

    }

}