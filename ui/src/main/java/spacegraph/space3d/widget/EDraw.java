package spacegraph.space3d.widget;


import jcog.Util;
import jcog.pri.UnitPri;
import spacegraph.space3d.SimpleSpatial;

/**
 * Drawn edge, lightweight
 */
public class EDraw<Y extends SimpleSpatial> extends UnitPri {

    private final int hash;
    
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    /** proportional to radius */
    public float attractionDist = 1f;

    final Y src, tgt;
    public EDraw(Y src, Y target, float pri) {
        //super(Tuples.twin(src,target), pri);
        super(pri);
        this.src = src;
        this.tgt = target;
        this.hash = Util.hashCombine(src.id, target.id);
    }

    public Y src() {
        return src; //id.getOne();
    }
    public Y tgt() {
        return tgt; //id.getTwo();
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    public final boolean connected() {
        return src.active() && tgt.active();
    }
}
