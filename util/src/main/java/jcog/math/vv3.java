package jcog.math;

import jcog.Util;

/** double float (64-bit) 3D vector */
public final class vv3  {

    /** TODO read-only impl */
    public static final vv3 X_AXIS = new vv3(1.0, (double) 0, (double) 0);
    public static final vv3 Y_AXIS = new vv3((double) 0, 1.0, (double) 0);
    public static final vv3 Z_AXIS = new vv3((double) 0, (double) 0, 1.0);

    public double x;
    public double y;
    public double z;

    public vv3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public vv3 clone() {
        return new vv3(x, y, z);
    }

    public boolean equals(vv3 v) {
        return equals(v, Double.MIN_NORMAL);
    }
    public boolean equals(vv3 v, double epsilon) {
        return this == v ||
                (
                    Util.equals(x, v.x, epsilon) &&
                    Util.equals(y, v.y, epsilon) &&
                    Util.equals(z, v.z, epsilon)
                );
    }

    public vv3 addThis(vv3 v) {
        this.x += v.x; this.y += v.y; this.z += v.z;
        return this;
    }
    public vv3 add(vv3 v) {
        return new vv3(x + v.x, y + v.y, z + v.z);
    }

    public vv3 minus(vv3 v) {
        return new vv3(x - v.x, y - v.y, z - v.z);
    }

    public vv3 scale(double s) {
        return new vv3(s * x, s * y, s * z);
    }
    public vv3 scaleThis(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        return this;
    }

    public double dot(vv3 v) {
        return x*v.x + y*v.y + z*v.z;
    }

    public vv3 cross(vv3 v) {
        return new vv3(
            y*v.z - z*v.y,
            z*v.x - x*v.z,
            x*v.y - y*v.x
        );
    }

    public double lengthSquared() {
        return x*x + y*y + z*z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public vv3 normalize() {
        if (hasZero()) {
            return this;
        }
        return scale(1.0 / length());
    }

    public vv3 normalizeThis(double scale) {
        normalizeThis();
        return scaleThis(scale);
    }

    public vv3 normalizeThis() {
        if (hasZero())
            return this;
        return scaleThis(1.0 / length());
    }

    private boolean hasZero() {
        return Math.abs(x) <= Double.MIN_NORMAL ||
               Math.abs(y) <= Double.MIN_NORMAL ||
               Math.abs(z) <= Double.MIN_NORMAL;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ')';
    }

    public void invertThis() {
        x = -x;
        y = -y;
        z = -z;
    }

    public double distanceSquared(vv3 v) {
        if (this == v) return (double) 0;
        return Util.sqr(x - v.x) + Util.sqr(y - v.y) + Util.sqr(z - v.z);
    }
}
