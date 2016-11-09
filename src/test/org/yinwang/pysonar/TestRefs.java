package org.yinwang.pysonar;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestRefs
{
    @Test
    public void testRefs()
    {
        String result = TestInference.testAll("tests", false);
        assertEquals(result, null, result);
    }
}
