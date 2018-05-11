package jcog.exe.valve;

import jcog.Util;

class ValveTest {

    public static void main(String[] args) {
        Valve<String,String> v = new Valve<>();
        TimeSlicing<String, String> exe = new TimeSlicing<>("time", 2);
        v.can(
                exe, //in fraction of the system's time cycle (whatever it currently is)
                new Valve.Mix<>("memory"), //in bytes
                new Valve.Mix<>("profiling"), //essentially boolean, on or off
                new Valve.Mix<>("wan_bandwidth"), //in bits per second
                new Valve.Mix<>("energy")  //in watts
        );

        Valve.Customer<String,String> a = v.start("A");
        a.need("memory", 0.25f);
        a.need("profiling", 1);
        AbstractWork<String, String> aw = new AbstractWork<>(a, "time", 0.25f) {
            @Override
            public boolean next() {
                //System.out.println("A \t" + this);
                Util.sleep(10); //+ Math.round(Math.random() * 25)
                return true;
            }
        };


        Valve.Customer b = v.start("B");
        b.need("memory", 0.1f);
        AbstractWork bw = new AbstractWork<>(b, "time", 0.5f) {
            @Override
            public boolean next() {
                //System.out.println("B \t" + this);
                Util.sleep(10); //+ Math.round(Math.random() * 25)
                return true;
            }
        };


        Valve.Customer c = v.start("idle");
        //c.need("time", 0.01f);

        v.commit();

        System.out.println(v.summary());

        Util.sleep(1000);

        exe.stop();
        //await total shutdown...

        Util.sleep(100);

        InstrumentedWork next;
        while ((next = exe.pending.poll())!=null) {
            System.out.println(next.summary());
        }
    }

}