package jcog.math.tensor;

import jcog.Util;
import jcog.learn.deep.dA;
import jcog.math.random.XorShift128PlusRandom;

/**
 * applies stacked autoencoder as a filter function of an input tensor
 */
public class AutoTensor extends ArrayTensor {

    private final Tensor in;
    private final dA a;
    private final double learnRate = 0.01f;
    float noise = 0.0005f;

    public AutoTensor(Tensor input, int outputs) {
        super(outputs);
        this.in = input;
        this.a = new dA(
                input.volume(),
                outputs, new XorShift128PlusRandom(1));
        a.randomize();
    }

    @Override
    public float[] get() {
        //learn next
        float[] ii = in.get();
        //System.out.println(n4(ii));

        double[] dii = Util.toDouble(ii);

        set(a.train(dii, learnRate, 0));

        if (noise > 0) {
            float max = Float.NEGATIVE_INFINITY;
            float min = Float.POSITIVE_INFINITY;
            for (int i = 0; i < data.length; i++) {
                double v = (data[i] += (((a.rng.nextFloat()) - 0.5f) * 2f) * noise);
                max = Math.max((float) v, max);
                min = Math.min((float) v, min);
            }
            //normalize
            if (max!=min) {
                for (int i = 0; i < data.length; i++) {
                    data[i] = (data[i] - min) / (max - min);
                }
            }
        }
        return super.get();
    }
}
