package jcog.pri;

/** pri limited to 0..1.0 range */
public class UnitPri extends Pri implements UnitPrioritizable {

    public UnitPri() {
        super();
    }

    public UnitPri(Prioritized x) {
        super(x);
    }

    public UnitPri(float x) {
        super(x);
    }

   @Override
    protected final boolean unit() {
        return true;
    }



}
