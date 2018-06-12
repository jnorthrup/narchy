package nars.derive.premise;

import jcog.exe.action.StochAction;

/** takes a parameter between 0. and 1.0 characterizing the intensity
 *  to apply.  the executor model interprets the intensity value
 *  as it wishes but ideally it results in a fair result
 *  relative to higher and lower values within the spectrum.
 */
public interface ThrottledAction<X> extends StochAction<X> {


//    /** default power=0.5 */
//    @Override default boolean test(X context) {
//        return test(context, 0.5f);
//    }

    boolean test(X context, float power);
}
