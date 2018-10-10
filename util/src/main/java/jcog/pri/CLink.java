package jcog.pri;

import org.roaringbitmap.RoaringBitmap;

/** Priority implementatio nwhich proxies to another and attaches a bitmap feature vector */
public class CLink<X extends Prioritizable> extends RoaringBitmap implements Prioritizable {

    public final X ref;

    public CLink(X ref, int... initialBits) {
        super();
        this.ref = ref;
        for (int i : initialBits)
            add(i);
    }

    @Override
    public String toString() {
        return ref + super.toString();
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (this.ref == o) return true;
        CLink c = (CLink)o; 
        return ref.equals(c.ref);
    }

    @Override
    public float pri(float p) {
        return ref.pri(p);
    }


    @Override
    public float pri() {
        return ref.pri();
    }

    @Override
    public boolean delete() {
        return ref.delete();
    }

    @Override
    public boolean isDeleted() {
        return ref.isDeleted();
    }
}
