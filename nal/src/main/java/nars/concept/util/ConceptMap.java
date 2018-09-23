package nars.concept.util;

import jcog.event.Offs;
import nars.NAR;
import nars.concept.Concept;
import nars.util.TimeAware;
import org.jetbrains.annotations.NotNull;

/**
 * TODO this is not updated to latest API
 */
public abstract class ConceptMap {

	@NotNull
	private final TimeAware timeAware;

	private final Offs regs;
	private int frame = -1;
	private int cycleInFrame = -1;

	public int frame() {
		return frame;
	}

	private void reset() {
	}

	ConceptMap(NAR nar) {

        regs = new Offs(
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

	private void onFrame() {
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
