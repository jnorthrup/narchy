package nars.derive.hypothesis;

import nars.NAR;
import nars.derive.Derivation;
import nars.derive.premise.Premise;
import nars.link.TaskLinks;
import nars.term.Compound;
import nars.term.Term;
import nars.time.When;

import java.util.function.Predicate;

import static nars.Op.ATOM;

/** generator of hypotheses */
public interface Hypothesizer {

	void premises(Predicate<Premise> p, When<NAR> when, TaskLinks links, Derivation d);

}
