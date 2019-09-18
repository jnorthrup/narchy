package nars.derive.hypothesis;

import nars.derive.Derivation;
import nars.derive.premise.Premise;
import nars.link.TaskLinks;

/** generator of hypotheses */
public interface Hypothesizer {

	Premise hypothesize(TaskLinks links, Derivation d);

}
