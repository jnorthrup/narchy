package jcog.optimize;

import jcog.util.ObjectFloatToFloatFunction;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * a knob but cooler
 */
public class Tweak<X> {

    public final ObjectFloatToFloatFunction<X> apply;
    public final String id;

    /** transduces a generic floating point value to a change in a property of the experiment subject */
    public Tweak(String id, ObjectFloatToFloatFunction<X> apply) {
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
