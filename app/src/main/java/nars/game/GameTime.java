package nars.game;

import jcog.data.list.FastCoWList;
import jcog.event.Off;
import jcog.exe.Loop;
import nars.NAR;
import nars.control.NARPart;
import nars.time.Tense;
import nars.time.clock.RealTime;
import nars.time.part.DurLoop;

/** model for timing a game */
public abstract class GameTime {

    private final Off on = null;

    protected abstract NARPart clock(Game a);


    /** estimated cycles per frame. not necessarily equal to the NAR's dur() */
    public abstract float dur();

    /** estimate the time of the next cycle */
    public abstract long next(long now);

    public final long prev(long now) {
        return (now - Math.max(1, next(now))); //HACK
    }

    /** like clone, but creates a new Time instance that 'follows' another */
    public abstract GameTime chain(float periodMultiplier);

    public final GameTime chain() { return chain(1); }

    abstract public void pause(boolean pause);


    /** measured in realtime
     * TODO async loop for extended sleep periods
     * */
    public static class FPS extends GameTime {

        private final transient float initialFPS;

        public final Loop loop;
        private Game g = null;
        private float dur = 1;

        public FPS(float fps) {
            this.initialFPS = fps;
            loop = new Loop(-1) {
                @Override public boolean next() {
                    g.next();
                    return true;
                }
            };
        }

        final FastCoWList<FPS> children = new FastCoWList<>(FPS.class);

        @Override
        public GameTime chain(float periodMultiplier) {
            FPS parent = this;
            return new FPS(initialFPS /* HACK */ / periodMultiplier) {

                {
                    children.add(this);
                }

                @Override
                public float dur() {
                    return parent.dur * periodMultiplier;
                }
            };
        }

        @Override
        public float dur() {
            return dur;
        }

        @Override
        public long next(long now) {
            RealTime t = (RealTime) g.nar.time;

            double unitsPerSec = 1/t.secondsPerUnit();
            double secondsPerFrame = 1/loop.getFPS();
            double unitsPerFrame = unitsPerSec * secondsPerFrame;
            this.dur = (float) Math.max(1, unitsPerFrame);
            return now + Math.round(t.secondsToUnits(loop.periodS()));
        }

        private transient int loopMS;
        private transient boolean wasPaused = false;

        @Override
        public synchronized void pause(boolean pause) {
            if (!wasPaused && pause) {
                loopMS = loop.periodMS();
                loop.stop();
                wasPaused = true;
                children.forEach(c -> c.pause(true));
            } else if (wasPaused && !pause) {
                loop.setPeriodMS(loopMS);
                wasPaused = false;
                children.forEach(c -> c.pause(false));
            }
        }

        @Override protected NARPart clock(Game g) {
//            if (!(g.nar().time instanceof RealTime))
//                throw new UnsupportedOperationException("realtime clock required");

            this.g = g;

            return new NARSubLoop();
        }

        private final class NARSubLoop extends NARPart {
            @Override
            protected void starting(NAR nar) {
                loop.fps(initialFPS);
            }

            @Override
            protected void stopping(NAR nar) {
                loop.stop();
            }
        }
    }

    /** measured in # of perceptual durations */
    public static class Durs extends GameTime {

        private final transient float durPeriod;

        public DurLoop loop = null;
        private float dur = 1;

        Durs(float durPeriod) {
            this.durPeriod = durPeriod;
        }

        @Override
        public GameTime chain(float periodMultiplier) {
            Durs parent = this;
            return new Durs(durPeriod * periodMultiplier) {
                @Override
                public float dur() {
                    return parent.dur * periodMultiplier;
                }
            };
        }

        @Override protected DurLoop clock(Game a) {
            loop = new DurLoop.DurRunnable(a::next);
            loop.durs(durPeriod);
            return loop;
        }

        @Override
        public void pause(boolean pause) {
            //nothing necessary to do
        }

        @Override
        public float dur() {
            return dur;
        }

        @Override
        public long next(long now) {
            DurLoop l = this.loop;
            this.dur = Tense.occToDT(l.durCycles());
            return now + l.durCycles();
        }
    }
//    /** measured in # of cycles */
//    public static class Cycs extends GameTime {
//
//        private final transient float cyclePeriod;
//
//        public Off loop = null;
//
//        public Cycs (float cyclePeriod) {
//            if (cyclePeriod !=1)
//                throw new TODO();
//            this.cyclePeriod = cyclePeriod;
//        }
//        @Override protected Off install(Game a) {
//            //loop = DurService.on(a.nar(), a::next);
//            //loop.durs(initialCycs);
//            loop = a.nar().onCycle(a::next);
//            return loop;
//        }
//
//        @Override
//        public float dur() {
//            throw new TODO();
//        }
//
//        @Override
//        public long next(long now) {
//            return Math.round(now + ((double)cyclePeriod));
//        }
//    }
    public static FPS fps(float fps) { return new FPS(fps); }
    public static Durs durs(float durs) { return new Durs(durs); }
//    public static Cycs cycles(float cycles) { return new Cycs(cycles); }
}
