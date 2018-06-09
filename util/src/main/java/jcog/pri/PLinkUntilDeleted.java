package jcog.pri;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted but saves the current value in a field first.
 */
public class PLinkUntilDeleted<X extends Deleteable> extends PLink<X> {

    public float priBeforeDeletion = Float.NaN;

    public PLinkUntilDeleted(X id, float p) {
        super(id, p);
    }

    @Override
    public final float priUpdate() {
        float p = this.pri;
        if (p == p) {
            if (id.isDeleted()) {
                this.priBeforeDeletion = p;
                return (this.pri = Float.NaN);
            } else {
                return p;
            }
        } else {
            return Float.NaN;
        }
    }


}
