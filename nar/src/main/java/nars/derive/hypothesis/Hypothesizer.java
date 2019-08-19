package nars.derive.hypothesis;

import nars.NAR;
import nars.derive.Derivation;
import nars.link.TaskLinks;
import nars.time.When;

/** generator of hypotheses */
public interface Hypothesizer {

	void premises(When<NAR> when, TaskLinks links, Derivation d);

}
