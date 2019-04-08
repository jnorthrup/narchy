package nars.agent;

import jcog.TODO;
import jcog.event.Off;
import jcog.exe.Loop;
import nars.time.Tense;
import nars.time.clock.RealTime;
import nars.time.part.DurLoop;

/** model for timing a game */
abstract public class GameTime {

    private volatile Off on = null;

    abstract protected Off install(Game a);

    public void start(Game a) {
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

    /** estimated cycles per frame. not necessarily equal to the NAR's dur() */
    public abstract int dur();

    /** estimate the time of the next cycle */
    abstract public long next(long now);

    public final long prev(long now) {
        return (now - Math.max(1, next(now))); //HACK
    }

    /** measured in realtime
     * TODO async loop for extended sleep periods
     * */
    public static class FPS extends GameTime {

        private transient final float initialFPS;

        public final Loop loop;
        private Game g = null;

        public FPS(float fps) {
            this.initialFPS = fps;
            loop = new Loop(-1) {
                @Override public boolean next() {
                    g.next();
                    return true;
                }
            };
        }

        @Override
        public int dur() {
            RealTime t = (RealTime) g.nar().time;
            double unitsPerSec = 1/t.secondsPerUnit();
            double secondsPerFrame = 1/loop.getFPS();
            double unitsPerFrame = unitsPerSec * secondsPerFrame;
            return Math.max(1, (int)Math.round(unitsPerFrame));
        }

        @Override
        public long next(long now) {
            RealTime t = (RealTime) g.nar().time;
            return now + Math.round(t.secondsToUnits(loop.periodMS.getOpaque()*0.001));
        }

        @Override protected Off install(Game a) {
            if (!(a.nar().time instanceof RealTime))
                throw new UnsupportedOperationException("realtime clock required");

            g = a;

            loop.setFPS(initialFPS);

            return loop::stop;
        }
    }

    /** measured in # of perceptual durations */
    public static class Durs extends GameTime {

        private transient final float durPeriod;

        public DurLoop loop = null;

        public Durs(float durPeriod) {
            this.durPeriod = durPeriod;
        }
        @Override protected Off install(Game a) {
            loop = a.nar().onDur(a::next);
            loop.durs(durPeriod);
            return loop;
        }

        @Override
        public int dur() {
            return Tense.occToDT(loop.durCycles());
        }

        @Override
        public long next(long now) {
            DurLoop l = this.loop;
            return l !=null ?  now + l.durCycles() : now;
        }
    }
    /** measured in # of cycles */
    public static class Cycs extends GameTime {

        private final transient float cyclePeriod;

        public Off loop = null;

        public Cycs (float cyclePeriod) {
            if (cyclePeriod !=1)
                throw new TODO();
            this.cyclePeriod = cyclePeriod;
        }
        @Override protected Off install(Game a) {
            //loop = DurService.on(a.nar(), a::next);
            //loop.durs(initialCycs);
            loop = a.nar().eventCycle.on(a::next);
            return loop;
        }

        @Override
        public int dur() {
            throw new TODO();
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
