package org.yinwang.pysonar;

import org.junit.Test;

import static org.junit.Assert.fail;

import java.util.List;

public class TestRefs
{
    @Test
    public void testRefs()
    {
        List<String> failed = TestInference.testAll("tests", false);
        if (failed != null)
        {
            String msg = "Some tests failed. " +
                         "\nLook at 'missing_refs.json' or 'wrong_types.json' in corresponding directories for details.";
            msg += "\n----------------------------- FAILED TESTS ---------------------------";
            for (String fail : failed)
            {
                msg += "\n - " + fail;
            }
            msg += "\n----------------------------------------------------------------------";
            fail(msg);
        }
    }
}
