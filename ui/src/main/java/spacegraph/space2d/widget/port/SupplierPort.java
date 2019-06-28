package spacegraph.space2d.widget.port;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.CheckBox;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SupplierPort<T> extends ConstantPort<T> {

    private final AtomicReference<T> built = new AtomicReference(null);
    private final Supplier<T> builder;

    public SupplierPort(String label, Class<? super T> type, Supplier<T> builder) {
        super(type);
        this.builder = builder;
        CheckBox toggle;
        set(toggle = new CheckBox(label));
        toggle.set(false);
        toggle.on((boolean tb)->{
            if (tb) {
                built.updateAndGet(x -> {
                    if (x == null)
                        return builder.get();
                    else
                        return x;
                });
                T b = built.getOpaque();
                set(b);
                if (b instanceof Surface) {
                    toggle.stop();
                    set(new Splitting(toggle, 0.9f, true, (Surface)b));
                }
            } else {
                if (built.getAndSet(null)!=null) {
//                    toggle.stop();
                    set((T) null);
//                    set(new Scale(toggle, 1));
                    set(toggle);
                }
            }
        });
    }

}
