package jcog.event;

import jcog.list.FasterList;

import java.util.Collections;

/**
 * essentially holds a list of registrations but forms an activity context
 * from the dynamics of its event reactivity
 */
public class Ons extends FasterList<On> {

    Ons(int length) {
        super(length);
    }

    Ons() {
        this(1);
    }

    public Ons(On<?>... r) {
        super(r.length);
        Collections.addAll(this, r);
    }


    public void off() {
        for (int i = 0; i < size(); i++) {
            get(i).off();
        }
        clear();
    }

//    public Ons add(On<?>... elements) {
//        Collections.addAll(this, elements);
//        return this;
//    }


}
