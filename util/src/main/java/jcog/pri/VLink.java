package jcog.pri;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * prioritized vector link
 * a) reference to an item
 * b) priority
 * c) vector coordinates
 * d) current centroid id(s)
 */
public final class VLink<X> extends PLink<X> {

    /**
     * feature vector representing the item as learned by clusterer
     */
    @NotNull
    public final double[] coord;

    /**
     * current centroid
     * TODO if allowing multiple memberships this will be a RoaringBitmap or something
     */
    public int centroid = -1;


    public VLink(X t, float pri, double[] coord) {
        super(t, pri);
        this.coord = coord;
    }

    public VLink(X t, float pri, int dims) {
        super(t, pri);
        Arrays.fill(this.coord = new double[dims], Double.NaN);
    }

    @Override
    public String toString() {
        return toBudgetString() + " " + id + "<" + Arrays.toString(coord) + '@' + centroid+ ">";
    }



}
