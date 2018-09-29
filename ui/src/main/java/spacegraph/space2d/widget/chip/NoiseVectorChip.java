package spacegraph.space2d.widget.chip;

import jcog.exe.Loop;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.Tensor;
import jcog.signal.tensor.TensorFunc;
import jcog.signal.tensor.TensorLERP;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.Random;

public class NoiseVectorChip extends Splitting {

    final IntRange size = new IntRange(1, 1, 64);
    private final TypedPort<Tensor> out;
    private BitmapMatrixView view;

    //TODO move to SmoothingChip
    @Deprecated final FloatRange momentum = new FloatRange(0.05f, 0, 1f);

    @Nullable Loop updater;
    @Nullable TensorLERP outputVector;

    final Random rng;

    public NoiseVectorChip() {
        this(new XoRoShiRo128PlusRandom(System.nanoTime()));
    }

    public NoiseVectorChip(Random rng) {
        super();/*0.25f, */

        this.rng = rng;

        updater = Loop.of(this::next);
        R( new Gridding(
            new LabeledPane("fps", new FloatSlider(1f, 0, 120).on((f)->{
                updater.setFPS(f);
            })),

            new LabeledPane("size", new IntSlider(size)),

            new LabeledPane("momentum", new FloatSlider(0.5f, 0, 1).on(momentum::set)),

            new LabeledPane("out", out = new TypedPort<>(Tensor.class))
        ));

        next();

    }

    protected void next() {
        Tensor o = this.outputVector;
        if (o == null || o.volume()!=size.intValue()) {

            //TODO abstract;
            TensorFunc oNext = Tensor.randomVectorGauss(size.intValue(), 0, 1, rng);


            L(view = new BitmapMatrixView((this.outputVector = new TensorLERP(oNext, momentum)).data));
        }

        out.out(outputVector.update());
        view.update();
    }
}
