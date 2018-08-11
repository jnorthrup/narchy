package nars.concept.util;

import jcog.event.Ons;
import nars.NAR;
import nars.concept.Concept;
import nars.util.TimeAware;
import org.jetbrains.annotations.NotNull;

/**
 * TODO this is not updated to latest API
 */
public abstract class ConceptMap {

	@NotNull
	public final TimeAware timeAware;

	Ons regs;
	int frame = -1;
	protected int cycleInFrame = -1;

	public int frame() {
		return frame;
	}

	public void reset() {
	}

	protected ConceptMap(@NotNull NAR nar) {

        regs = new Ons(
        nar.eventClear.on(n -> {
            frame = 0;
            reset();
        }),
        nar.eventCycle.on(n -> {
            frame++;
            onFrame();
            cycleInFrame = 0;
        }));
        




        this.timeAware = nar;

    }
	public void off() {

	}

	protected void onFrame() {
	}



	public abstract boolean contains(Concept c);

	/**
	 * returns true if the concept was successfully removed (ie. it was already
	 * present and not permanently included)
	 */
	protected abstract boolean onConceptForget(Concept c);

	/**
	 * returns true if the concept was successfully added (ie. it was not
	 * already present)
	 */
	protected abstract boolean onConceptActive(Concept c);

}
