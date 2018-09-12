package nars.agent;

import jcog.TODO;
import jcog.event.Off;
import jcog.exe.Loop;
import nars.control.DurService;
import nars.time.clock.RealTime;

abstract public class FrameTrigger {

    private volatile Off on = null;

    abstract protected Off install(NAgent a);

    public void start(NAgent a) {
        synchronized (this) {
            assert(on == null);
            on = install(a);
        }
    }

    public void stop() {
        synchronized (this) {
            if (on != null) {
                on.off();
                on = null;
            }
        }
    }

    /** estimate the time of the next cycle */
    abstract public long next(long now);

    /** measured in realtime */
    public static class FPS extends FrameTrigger {

        private transient final float initialFPS;

        public final Loop loop;
        private NAgent agent = null;

        public FPS(float fps) {
            this.initialFPS = fps;
            loop = new Loop(-1) {
                @Override public boolean next() {
                    agent.next();
                    return true;
                }
            };
        }

        @Override
        public long next(long now) {
            RealTime t = (RealTime) agent.nar().time;
            return now + Math.round(t.secondsToUnits(loop.periodMS.getOpaque()*0.001));
        }

        @Override protected Off install(NAgent a) {
            if (!(a.nar().time instanceof RealTime))
                throw new UnsupportedOperationException("realtime clock required");

            agent = a;

            loop.setFPS(initialFPS);

            return loop::stop;
        }
    }

    /** measured in # of perceptual durations */
    public static class Durs extends FrameTrigger {

        private transient final float durPeriod;

        public DurService loop = null;

        public Durs(float durPeriod) {
            this.durPeriod = durPeriod;
        }
        @Override protected Off install(NAgent a) {
            loop = DurService.on(a.nar(), a::next);
            loop.durs(durPeriod);
            return loop;
        }

        @Override
        public long next(long now) {
            DurService l = this.loop;
            return l !=null ?  now + l.durCycles() : now;
        }
    }
    /** measured in # of cycles */
    public static class Cycs extends FrameTrigger {

        private transient float cyclePeriod;

        public Off loop = null;

        public Cycs (float cyclePeriod) {
            if (cyclePeriod !=1)
                throw new TODO();
            this.cyclePeriod = cyclePeriod;
        }
        @Override protected Off install(NAgent a) {
            //loop = DurService.on(a.nar(), a::next);
            //loop.durs(initialCycs);
            loop = a.nar().eventCycle.on(a::next);
            return loop;
        }

        @Override
        public long next(long now) {
            return Math.round(now + ((double)cyclePeriod));
        }
    }
    public static FPS fps(float fps) { return new FPS(fps); }
    public static Durs durs(float durs) { return new Durs(durs); }
    public static Cycs cycles(float cycles) { return new Cycs(cycles); }
}
