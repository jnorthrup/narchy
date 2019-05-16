package nars.term.util;

import nars.term.Term;
import nars.util.SoftException;

public class TermTransformException extends SoftException {
    final Term x, y;
    public TermTransformException(String reason, Term x, Term y) {
        super(reason);
        this.x = x; this.y = y;
    }

    @Override
    public String getMessage() {
        String m = super.getMessage();
        String xh = "\n\tx: ";
        String yh = "\n\ty: ";
        StringBuilder s = new StringBuilder(m.length() + xh.length()*2 + 512);
        s.append(m).append(xh);
        x.appendTo(s).append(yh);
        return y.appendTo(s).toString();
    }
}
