//package nars.test.analyze;
//
//import jcog.event.Topic;
//import jcog.meter.event.HitMeter;
//import nars.NAR;
//import nars.control.NARService;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
//* Created by me on 2/10/15.
//*/
//public class EventCount extends NARService {
//
//    @NotNull
//    public final Map<Object, HitMeter> eventMeters = new ConcurrentHashMap<>();
//
//    {
//        Topic.each(NAR.class, (field) -> {
//            String nn = field.getName();
//            eventMeters.put(nn, new HitMeter(nn));
//        });
//    }
//
//    public EventCount(@NotNull NAR nar) {
//        super(nar);
//    }
//
//    @Override
//    protected void start(NAR nar)  {
//        super.start(nar);
//        ons.addAll(Topic.all(nar, (event, value) -> eventMeters.get(event).hit()));
//    }
//
//    public long numTaskProcesses() { return eventMeters.get("eventTask").count(); }
////    public long numOutputs() { return eventMeters.get("eventDerived").count(); }
////    public long numInputs() { return eventMeters.get("eventInput").count(); }
////    public long numExecutions() { return eventMeters.get("eventExecute").count(); }
////    public long numErrors() { return eventMeters.get("eventError").count(); }
////    public long numAnswers() { return eventMeters.get("eventAnswer").count(); }
//
//
//    public void reset() {
//        eventMeters.values().forEach(HitMeter::reset);
//    }
//}
