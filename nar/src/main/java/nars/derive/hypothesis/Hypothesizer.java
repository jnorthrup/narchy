package nars.derive.hypothesis;

import nars.derive.Derivation;
import nars.link.TaskLinks;

/** generator of hypotheses */
public interface Hypothesizer {

	void hypothesize(TaskLinks links, Derivation d);

}
