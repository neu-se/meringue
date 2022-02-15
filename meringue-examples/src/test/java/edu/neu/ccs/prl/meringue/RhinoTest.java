package edu.neu.ccs.prl.meringue;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;

public class RhinoTest {
    public void compile(byte[] input) {
        Context context = Context.enter();
        try {
            context.compileString(new String(input), "", 0, null);
        } catch (EvaluatorException e) {
            //
        } finally {
            Context.exit();
        }
    }
}
