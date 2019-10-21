package spacegraph.space2d.widget.port;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

public class TextPort extends EditablePort<String> {

    final AtomicReference<String> val = new AtomicReference();

    public TextPort() {

        super("", String.class);
        on(new Consumer<String>() {
            @Override
            public void accept(String s) {
                boolean[] changed = {false};
                val.accumulateAndGet(s, new BinaryOperator<String>() {
                    @Override
                    public String apply(String p, String n) {
                        if (p != null && n.equals(p)) {
                            changed[0] = true;
                            return p;
                        } else
                            return s;
                    }
                });
                if (changed[0]) {

                }
            }
        });
    }

    @Override
    protected String parse(String x) {
        return x;
    }
}
