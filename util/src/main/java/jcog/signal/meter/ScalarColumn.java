/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.signal.meter;

/**
 * Instance of a column header in a Metrics table; indicates what appears
 * in the column index of rows.
 * May cache re-usable metadata specific to the signal shared by several SignalData views (ex: min, max)
 */
public class ScalarColumn implements Comparable<ScalarColumn> {
    public final String id;
    public String unit;

    private double min, max;


    public ScalarColumn(String id) {
        this(id, null);
    }

    public ScalarColumn(String id, String unit) {
        this.id = id;
        this.unit = unit;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return id.equals(((ScalarColumn) obj).id);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int compareTo(ScalarColumn o) {
        return id.compareTo(o.id);
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    void setMin(double newMin) {
        min = newMin;
    }

    void setMax(double newMax) {
        max = newMax;
    }

    void resetBounds() {
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
    }










}
