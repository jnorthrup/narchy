package jcog.service;

import jcog.Paper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * component of a system.
 *
 * The generic name of 'Part' is to imply generality.
 * this class was originally derived from Guava's "Service" class and related API.
 * but Service implies some server/served activity while in reality this releationship
 * is necessarily relevant or clearly defined.  instead the simpler container/contained
 * relationship does certainly exist.  the components may 'serve' the collection,
 * and at the same time the collection may 'serve' the components.  but from the
 * system's perspective, this becomes subjective ontologically.
 *
 * @param C to whatever may be (whether temporarily or permanently):
 *          dynamically attached
 *          interfaced with
 *          integrated to
 *
 *
 *
 * they are dynamically registerable and de-registerable.
 * multiple instances of the same type of Part may be present unless explicitly restricted by singleton flag.
 *
 * this is meant to be flexible and dynamic, to support runtime metaprogramming, telemetry, performance metrics
 *   accessible to both external control and internal reasoner decision processes.
 *
 * TODO methods of accessing parts by different features:
 *      --function
 *      --class
 *      --name
 *      --etc
 *
 */
@Paper
public abstract class Part<C>  {

    final AtomicReference<Parts.ServiceState> state = new AtomicReference<>(Parts.ServiceState.Off);

    public final boolean isOn() {
        return state.getOpaque() == Parts.ServiceState.On;
    }

    public final boolean isOnOrStarting() {
        return state.getOpaque().onOrStarting;
    }

    public final boolean isOff() {
        return state.getOpaque() == Parts.ServiceState.Off;
    }


    @Override
    public String toString() {
        String nameString = getClass().getName();

        if (nameString.startsWith("jcog.") || nameString.startsWith("nars.")) //HACK
            nameString = getClass().getSimpleName();

        return nameString + ':' + super.toString();
    }

    abstract protected void start(C x);

    abstract protected void stop(C x);

    public Parts.ServiceState state() {
        return state.getOpaque();
    }

    protected void _state(Parts.ServiceState forced) {
        this.state.set(forced);
    }
}
