package jcog.pri;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * prioritized vector link
 * a) reference to an item
 * b) priority
 * c) vector coordinates
 * d) current centroid id(s)
 */
public final class VLink<X> extends PLinkHashCached<X> {

    /**
     * feature vector representing the item as learned by clusterer
     */
    public final double[] coord;

    /**
     * current centroid
     * TODO if allowing multiple memberships this will be a RoaringBitmap or something
     */
    public int centroid = -1;


//    public VLink(X t, float pri, double[] coord) {
//        super(t, pri);
//        this.coord = coord;
//    }

    public VLink(X t, float pri, int dims) {
        super(t, pri);
        Arrays.fill(this.coord = new double[dims], Double.NaN);
    }

    @Override
    public String toString() {
        return toBudgetString() + ' ' + id + '<' + Arrays.toString(coord) + '@' + centroid+ '>';
    }


    public void update(@Nullable Consumer<VLink<X>> f) {
        X tt = get();
        if ((tt instanceof Prioritized) && ((Prioritized) tt).isDeleted())
            delete();
        else if (f!=null)
            f.accept(this);
    }
}
