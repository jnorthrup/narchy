package jcog.optimize;

import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * a knob but cooler
 */
public class Tweak<X> {

    public final ObjectFloatProcedure<X> apply;
    public final String id;

    /** transduces a generic floating point value to a change in a property of the experiment subject */
    public Tweak(String id, ObjectFloatProcedure<X> apply) {
        this.id = id;
        this.apply = apply;
    }

    @Override
    public String toString() {
        return id;
    }

    /** returns any unknown meta-parameters necessary for this tweak to be used */
    public List<String> unknown(Map<String,Float> hints) {
        return Collections.emptyList();
    }

}
