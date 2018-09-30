package spacegraph.space2d.widget.port;

import java.util.concurrent.atomic.AtomicReference;

public class TextPort extends EditablePort<String> {

    final AtomicReference<String> val = new AtomicReference();

    public TextPort() {
        super("", String.class);
    }

    @Override
    protected boolean change(String s) {
        final boolean[] changed = {false};
        val.accumulateAndGet(s, (p,n)->{
           if (p!=null && n.equals(p)) {
               changed[0] = true;
               return p;
           } else
               return s;
        });
        return changed[0];
    }

    @Override
    protected String parse(String x) {
        return x;
    }
}
