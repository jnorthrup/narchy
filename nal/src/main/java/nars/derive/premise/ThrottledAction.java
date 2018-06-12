package nars.derive.premise;

import jcog.exe.action.StochAction;
import nars.control.Cause;

/** takes a parameter between 0. and 1.0 characterizing the intensity
 *  to apply.  the executor model interprets the intensity value
 *  as it wishes but ideally it results in a fair result
 *  relative to higher and lower values within the spectrum.
 */
public abstract class ThrottledAction<X> extends StochAction<X> {

    public final Cause[] causes;

    protected ThrottledAction(Cause... causes) {
        this.causes = causes;
    }

    /** default power=0.5 */
    @Override public boolean test(X context) {
        return test(context, 0.5f);
    }

    abstract public boolean test(X context, float power);
}
