package spacegraph.space2d.widget.port;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

public class BoolPort extends TypedPort<Boolean> {

    public BoolPort() {
        super(Boolean.class);
    }

    public BoolPort(BooleanConsumer b) {
        this();
        on(B->{
            b.accept(B.booleanValue());
        });
    }
}
