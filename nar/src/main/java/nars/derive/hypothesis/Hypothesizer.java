package nars.derive.hypothesis;

import nars.NAR;
import nars.derive.Derivation;
import nars.derive.premise.Premise;
import nars.link.TaskLinks;
import nars.time.When;

import java.util.function.Predicate;

/** generator of hypotheses */
public interface Hypothesizer {

	void premises(Predicate<Premise> p, When<NAR> when, TaskLinks links, Derivation d);

}
