package jcog.event;

/**
 * essentially holds a list of registrations but forms an activity context
 * from the dynamics of its event reactivity
 */
public class Offs extends jcog.data.list.FastCoWList<Off> implements Off {

    Offs(int capacity) {
        super(capacity, Off[]::new);
    }

    Offs() {
        this(1);
    }

    public Offs(Off... r) {
        this(r.length);
        for (Off o : r)
            add(o);
    }


    public void off() {
        for (int i = 0; i < size(); i++) {
            get(i).off();
        }
        clear();
    }







}
