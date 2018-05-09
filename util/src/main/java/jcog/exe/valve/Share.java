package jcog.exe.valve;

import jcog.Util;
import jcog.pri.Pri;

/** an allocated share of some resource */
public class Share<Who,What> extends Pri {
    public final What what;
    public final Who who;

    /** ownership amount supplied to the client, in absolute fraction of 100%.
     *  determined by the Focus not client. this is stored in the super class's 'pri' field. */
    //float supply = 0;

    /** mutable demand, adjustable by client in [0..1.0].  its meaning is subjective and may be
     * relative to its previous values. */
    float need = 0f;

    public Share(Who who, What what) {
        this.who = who;
        this.what = what;
    }

    public void need(float newDemand) {
        this.need = Util.unitize(newDemand);
    }
    public float need() {
        return this.need;
    }

    @Override public String toString() {
        return "(" + what + "+" + pri() + "|" + need + "~" + who + ")";
    }
}
