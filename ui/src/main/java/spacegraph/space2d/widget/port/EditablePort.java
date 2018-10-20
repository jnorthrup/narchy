package spacegraph.space2d.widget.port;

import spacegraph.space2d.widget.console.TextEdit0;

abstract public class EditablePort<X> extends TypedPort<X> {

    final TextEdit0 txt;

    public EditablePort(X initialValue, Class<? super X> type) {
        super(type);
        process(initialValue);

        txt = new TextEdit0(8, 1);
        txt.on(z -> out(parse(z)));
        set(txt);
    }


//
//    public final void out(X x) {
//        if (x == null)
//            return;
//
////        try {
//                super.out(next);
//            //}
////
////        } catch (Throwable t) {
////
////        }
//    }

    @Override
    protected void out(Port sender, X _next) {
        X next = process(_next);
        if (next==null)
            return;

        if (change(next)) {

            txt.text(toString(next));
            super.out(sender, next);

            return /* true */;
        }
        return /*false*/;
    }

    protected String toString(X next) {
        return next.toString();
    }

    abstract protected boolean change(X x);


    /** returns true if the value is valid and can set the port, override in subclasses to filter input */
    public X process(X x) {
        return x;
    }

    abstract protected X parse(String x);

}
