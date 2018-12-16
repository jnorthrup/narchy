package spacegraph.space2d.widget.port;

import spacegraph.space2d.widget.console.TextEdit0;

abstract public class EditablePort<X> extends TypedPort<X> {

    public final TextEdit0 edit;

    public EditablePort(X initialValue, Class<? super X> type) {
        super(type);
        process(initialValue);

        edit = new TextEdit0(8, 1);
        //TODO txt = new TextEdit(8, 1);
        edit.on(z -> out(parse(z)));
        set(edit);
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
    protected boolean out(Port sender, X _next) {
        X next = process(_next);

        if (next!=null && change(next)) {
            if (super.out(sender, next)) {
                edit.text(toString(next));
                return true;
            }
        }
        return false;
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
