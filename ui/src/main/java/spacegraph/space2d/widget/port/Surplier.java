package spacegraph.space2d.widget.port;

import jcog.exe.Exe;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.CheckBox;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class Surplier<T> extends ConstantPort<T> {

    private final AtomicReference<T> built = new AtomicReference(null);

    public Surplier(String label, Class<? super T> type, Supplier<T> builder) {
        super(type);
        CheckBox toggle;
        set(toggle = new CheckBox(label));
        toggle.set(false);
        toggle.on(new BooleanProcedure() {
            @Override
            public void value(boolean tb) {
                if (tb) {
                    toggle.enabled(false);
                    Exe.run(new Runnable() {
                        @Override
                        public void run() {
                            if (!toggle.on())
                                return; //toggled off while waiting to execute

                            built.updateAndGet(new UnaryOperator<T>() {
                                @Override
                                public T apply(T x) {
                                    if (x == null)
                                        return builder.get();
                                    else
                                        return x;
                                }
                            });
//                    if (!toggle.on()) {
//                        built.set(null);
//                        return; //toggled off while building
//                    }

                            T b = built.getOpaque();
                            Surplier.this.set(b);
                            if (b instanceof Surface) {
                                toggle.stop();
                                Surplier.this.set(new Splitting(toggle, 0.95f, true, (Surface) b));
                            }
                            toggle.set(true);
                            toggle.enabled(true);
                        }
                    });
                } else {
                    if (built.getAndSet(null) != null) {
                        toggle.enabled(false);

//                    toggle.stop();
                        Surplier.this.set((T) null);
//                    set(new Scale(toggle, 1));
                        Surplier.this.set(toggle);
                        toggle.enabled(true);
                    }
                }
            }
        });
    }

    public static Surplier button(String label, Supplier<Surface> s) {
        return new Surplier<>(label, Surface.class, s);
    }
}
