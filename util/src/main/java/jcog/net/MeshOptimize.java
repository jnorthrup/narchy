package jcog.net;

import com.google.common.primitives.Floats;
import jcog.Util;
import jcog.net.attn.MeshMap;
import jcog.optimize.Optimize;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class MeshOptimize<X> extends Optimize<X> {

    /** experiment id's */
    private static final AtomicInteger serial = new AtomicInteger();

    /** should get serialized compactly though by msgpack */
    private final MeshMap<Integer, List<Float>> m;

    public MeshOptimize(String id, Supplier<X> subject) {
        super(subject);

        m = MeshMap.get(id, (k,v)->{
            System.out.println("optimize recv: " + v);
        });

    }

    @Override
    protected void onExperiment(double[] point, double score) {
        super.onExperiment(point, score);
        m.put(serial.incrementAndGet(), Floats.asList(ArrayUtils.add(Util.toFloat(point), (float)score)));
    }
}
