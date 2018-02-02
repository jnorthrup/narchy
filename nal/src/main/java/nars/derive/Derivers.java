package nars.derive;

import nars.NAR;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternIndex;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * utility class for working witih Deriver's
 */
public class Derivers {

    /** range is inclusive */
    public static Set<String> standard(int minLevel, int maxLevel, String... otherFiles) {
        Set<String> files = new TreeSet();
        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {
                case 8:
                    //files.add("motivation.nal");
                    //files.add("list.nal");  //experimental
                case 7:
                    //TODO move temporal induction to a separate file
                    //fallthru
                case 6:
                    files.add("nal6.nal");
                    files.add("nal6.guess.nal");
                    files.add("nal6.layer2.nal");

                    files.add("induction.nal");  //TODO nal6 only needs general induction, not the temporal parts

                    files.add("misc.nal"); //TODO split this up
                    break;
                case 5:
                case 4:
                case 3:
                case 2:
                    files.add("nal3.nal");
                    files.add("nal3.guess.nal");
                    files.add("nal2.nal");
                    files.add("nal2.guess.nal");
                    files.add("analogy.nal");
                    break;
                case 1:
                    files.add("nal1.nal");
                    files.add("nal1.guess.nal");
                    break;
            }
        }

        Collections.addAll(files, otherFiles);

        return files;
    }

    /**
     * loads default deriver rules, specified by a range (inclusive) of levels. this allows creation
     * of multiple deriver layers each targetting a specific range of the NAL spectrum
     */
    public static Function<NAR, Deriver> deriver(int minLevel, int maxLevel, String... extraFiles) {
        assert ((minLevel <= maxLevel && maxLevel > 0) || extraFiles.length > 0);

        return Deriver.deriver(nar ->
                PremiseRuleSet.rules(nar, new PatternIndex(),
                        standard(minLevel, maxLevel, extraFiles)
                ));
    }
}
