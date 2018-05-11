package jcog.exe.valve;

class ValveTest {

    final Sharing<String,String> v = new Sharing<>();

//    @Test
//    public void testTimeSlicing1() {
//
//        TimeSlicing<String, String> exe = new TimeSlicing<>("time", 2);
//        v.can(
//                exe, //in fraction of the system's time cycle (whatever it currently is)
//                new Mix<>("memory"), //in bytes
//                new Mix<>("profiling"), //essentially boolean, on or off
//                new Mix<>("wan_bandwidth"), //in bits per second
//                new Mix<>("energy")  //in watts
//        );
//
//        Demand<String,String> a = v.start("A");
//        a.need("memory", 0.25f);
//        a.need("profiling", 1);
//        AbstractWork<String, String> aw = new AbstractWork<>(a, "time", 0.25f) {
//            @Override
//            public boolean next() {
//                //System.out.println("A \t" + this);
//                Util.sleep(10); //+ Math.round(Math.random() * 25)
//                return true;
//            }
//        };
//
//
//        Demand b = v.start("B");
//        b.need("memory", 0.1f);
//        AbstractWork bw = new AbstractWork<>(b, "time", 0.5f) {
//            @Override
//            public boolean next() {
//                //System.out.println("B \t" + this);
//                Util.sleep(10); //+ Math.round(Math.random() * 25)
//                return true;
//            }
//        };
//
//
//        Demand<String, String> c = v.start("idle");
//        //c.need("time", 0.01f);
//
//        v.commit();
//
//        System.out.println(v.summary());
//
//        Util.sleep(500);
//
//        exe.stop();
//        //await total shutdown...
//
//        Util.sleep(50);
//
//        InstrumentedWork next;
//        while ((next = exe.pending.poll())!=null) {
//            System.out.println(next.summary());
//        }
//    }

}