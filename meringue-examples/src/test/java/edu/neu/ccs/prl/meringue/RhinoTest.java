package edu.neu.ccs.prl.meringue;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;

@RunWith(JQF.class)
public class RhinoTest {
    public void test(byte[] input) {
        Context context = Context.enter();
        try {
            context.compileString(new String(input), "", 0, null);
        } catch (EvaluatorException e) {
            //
        } finally {
            Context.exit();
        }
    }

    @Fuzz
    public void testWithGenerator(@From(JavaScriptCodeGenerator.class) String source) {
        test(source.getBytes());
    }
}
