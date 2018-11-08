package jcog.lab;

import jcog.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Optilive - continuously runnable live optimizer / analyzer
 *   --model, same as Optimize
 *   --parameters, same as Optimize
 *   --goals, same as Optimize
 *
 * initially starts idle with 0 active parameters, 0 active goals
 *
 * when at least 1 parameter and 1 goal are activated, it begins
 * results are collected into a streaming ARFF
 * results are analyzed by zero or more analysis engines, which can include:
 *     a) decision tree
 *     b) nars
 *     c) etc..
 * these derive analyses that are collected in separate logs for each
 *
 *
 * remote control interface thru JMX RPC or something
 */
public class Optilive<S,E>  {

    transient Optimization<S,E> running = null;

    final Supplier<S> subj;
    final Function<Supplier<S>, E> experiment;

    final List<Goal<E>> goals;
    final List<Var<S, ?>> vars;
    final List<Sensor<E, ?>> sensors;
    static final private Logger logger = LoggerFactory.getLogger(Optilive.class);
    private Thread thread;

    private static final long SLEEP_TIME_MS = 500;

    public Optilive(Supplier<S> subj, Function<Supplier<S>, E> experiment, List<Goal<E>> goals, List<Var<S, ?>> vars, List<Sensor<E, ?>> sensors) {
        this.subj = subj;

        this.experiment = experiment;
        this.goals = goals;
        this.vars = vars;
        this.sensors = sensors;
    }

    private Random random() {
        return ThreadLocalRandom.current();
    }

    /** build the next optimization to run */
    private Optimization<S, E> next() {

        Goal<E> goal = goals.get(random().nextInt(goals.size())); //TODO choose subset of this list by model

        List<Var<S, ?>> vars = this.vars; //TODO choose subset of this list by model

        List<Sensor<E, ?>> sensors = this.sensors; //TODO choose subset of this list by model

        return new Optimization<>(subj,experiment, goal, vars, sensors);
    }

    private int experimentIterations() {
        return 16; //TODO model decided
    }

    private enum State {
        Decide {
            @Override
            public <S,E> void run(Optilive<S, E> o) {
                o.running = o.next();
                o.logger.info("next experiment {}", o.running);
                o.nextState = Run;
            }
        },
        Run {
            @Override
            public <S,E> void run(Optilive<S, E> o) {
                o.running.runSync(o.experimentIterations());
                o.nextState = Analyze;
            }
        },
        Analyze {
            @Override
            public <S,E> void run(Optilive<S, E> o) {
                o.analyze();
                o.nextState = Decide;
            }
        },
        Sleep {
            @Override
            public <S,E> void run(Optilive<S, E> o) {
                Util.sleepMS(SLEEP_TIME_MS);
            }
        };

        abstract public <S,E> void run(Optilive<S,E> o);
    }

    /** persist to disk or network, cleanup */
    private void shutdown() {

    }

    /** run analyses and store results */
    private void analyze() {
        running.print();
        running.tree(3, 6).print();
    }


    private volatile State state = State.Decide, nextState = null;

    private void run() {
        logger.info("start");
        do {
            try {
                state.run(this);
                state = nextState;
            } catch (ThreadDeath e) {
                break;
            } catch (Throwable e) {
                logger.info("{}", e);
            }
        } while (true);

        shutdown();
    }

    public void pause() {
        synchronized(thread) {
            nextState = State.Sleep;
            thread.interrupt();
        }
    }

    public void resume() {
        synchronized(thread) {
            nextState = State.Run;
            thread.interrupt();
        }

    }

    public void stop() {
        synchronized(thread) {
            Thread thread = this.thread;
            this.thread = null;
            thread.stop();
            try {
                thread.join();
            } catch (Throwable e) {
                logger.error("{}", e);
            }
        }
    }

    public void start() {
        (this.thread = new Thread(this::run)).start();
    }

}

