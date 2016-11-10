package org.yinwang.pysonar;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.List;

public class TestRefs
{
    @Test
    public void testRefs()
    {
        List<String> failed = TestInference.testAll("tests", false);
        String msg = "";
        for (String fail : failed)
        {
            msg += "[failed] " + fail;
        }
        assertTrue("Some tests failed :\n" + msg, failed == null);
    }
}
