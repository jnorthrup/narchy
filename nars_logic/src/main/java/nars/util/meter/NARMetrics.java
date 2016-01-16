package nars.util.meter;

import nars.NAR;
import nars.util.event.FrameReaction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NARMetrics extends FrameReaction {

    @NotNull
    public final TemporalMetrics<Object> metrics;
    @NotNull
    public final NAR nar;

    public NARMetrics(@NotNull NAR n, int historySize) {
        super(n);

        nar = n;

        metrics = new TemporalMetrics(historySize);

        metrics.addViaReflection(n.memory.emotion);
//
//        if (n.memory.resource!=null)
//            metrics.addViaReflection(n.memory.resource);


        //metrics.addMeter(new BasicStatistics(metrics, n.memory.resource.FRAME_DURATION.id(), 16));
//        if (n.memory.resource!=null)
//            metrics.add(new FirstOrderDifference(metrics, n.memory.resource.CYCLE_RAM_USED.id()));

        metrics.addViaReflection(n.memory.logic);
    }

    @Override
    public void onFrame() {
        metrics.update((double)nar.time());
    }
//
//    @Override
//    public void setActive(boolean b) {
//        super.setActive(b);
//        if (nar!=null)
//            nar.memory.logic.setActive(b);
//    }
//
//    @Override
//    public void event(Class event, Object[] args) {
//        if (event == Events.FrameEnd.class) {
//            if (metrics!=null)
//                metrics.update((double)nar.time());
//        }
//
//    }

    public <S extends Signals> S addMeter(S m) {
        metrics.add(m);
        return m;
    }

    @NotNull
    public TemporalMetrics<Object> getMetrics() {
        return metrics;
    }

    @NotNull
    public SignalData[] getCharts(@NotNull String... names) {
        List<SignalData> l = new ArrayList(names.length);
        for (String n : names) {
            SignalData t = metrics.newSignalData(n);
            if (t!=null)
                l.add(t);
        }
        return l.toArray(new SignalData[l.size()]);
    }

    public List<SignalData> getCharts() {
        return metrics.getSignalDatas();
    }
}
