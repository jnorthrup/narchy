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
        var m = super.getMessage();
        var xh = "\tx: ";
        var s = new StringBuilder(m.length() + xh.length()*2 + 512);
        s.append(getClass()).append('\n');
        s.append(xh);
        var yh = "\n\ty: ";
        x.appendTo(s).append(yh);
        return y.appendTo(s).append('\n').append(m).toString();
    }
}
