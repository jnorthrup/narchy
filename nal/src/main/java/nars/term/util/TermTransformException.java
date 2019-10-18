package nars.term.util;

import nars.term.Term;
import nars.util.SoftException;

public class TermTransformException extends SoftException {
    final Term x;
    final Term y;
    public TermTransformException(Term x, Term y, String reason) {
        super(reason);
        this.x = x; this.y = y;
    }

    @Override
    public String getMessage() {
        String m = super.getMessage();
        String xh = "\tx: ";
        String yh = "\n\ty: ";
        StringBuilder s = new StringBuilder(m.length() + xh.length()*2 + 512);
        s.append(getClass()).append('\n');
        s.append(xh);
        x.appendTo(s).append(yh);
        return y.appendTo(s).append('\n').append(m).toString();
    }
}
