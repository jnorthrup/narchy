package jcog.pri.mix.control;

import jcog.Util;
import jcog.learn.ql.HaiQae;
import jcog.math.tensor.ArrayTensor;
import jcog.math.tensor.Tensor;

public class HaiQMixAgent implements MixAgent {

    HaiQae agent;
    float delta = 0.2f;

    @Override
    public void act(Tensor in, float score, ArrayTensor out) {
        if (agent == null) {
            agent = new HaiQae(in.volume(), in.volume() / 2, out.volume() * 2);
        }

        int action = agent.act(score, in.get());
        if (action == -1)
            return; //error

        int which = action / 2;
        out.set(Util.clamp(out.get(which) + ((action % 2 == 0) ? (+1) : (-1)) * delta, -1, +1), which);

    }
}
