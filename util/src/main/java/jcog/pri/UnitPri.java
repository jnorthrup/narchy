package jcog.pri;

import jcog.Util;

/** pri limited to 0..1.0 range */
public class UnitPri extends Pri implements UnitPrioritizable {

    public UnitPri() {
        super();
    }

    public UnitPri(Prioritizable x) {
        super(x);
    }

    public UnitPri(float x) {
        super(x);
    }

    @Override public float v(float x) {
        return Util.unitize(x);
    }


}
