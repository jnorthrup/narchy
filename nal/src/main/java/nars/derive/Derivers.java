package nars.derive;

import nars.NAR;
import nars.derive.premise.PremiseDeriverRuleSet;

import java.util.*;

/**
 * utility class for working witih Deriver's
 */
public class Derivers {

    /** HACK range is inclusive */
    private static Set<String> standard(int minLevel, int maxLevel, String... otherFiles) {
        Set<String> files = new TreeSet();
        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {
                case 8:
                    
                    
                case 7:
                    
                    
                case 6:
                    files.add("nal6.nal");
                    files.add("nal6.guess.nal");
                    files.add("nal6.layer2.nal");
                    files.add("nal6.to.nal3.nal");

                    files.add("induction.nal");
                    files.add("hol.nal");

                    //files.add("misc.nal");
                    break;
                case 5:
                case 4:
                    files.add("nal4.nal");
                    break;
                case 3:
                case 2:
                    files.add("nal3.nal");
                    files.add("nal3.guess.nal");
                    files.add("nal2.nal");
                    files.add("nal2.guess.nal");
                    break;
                case 1:
                    files.add("analogy.nal");
                    files.add("nal1.nal");
                    files.add("nal1.guess.nal");
                    break;
            }
        }

        Collections.addAll(files, otherFiles);

        return files;
    }


















    /** standard ruleset */
    public static PremiseDeriverRuleSet nal(NAR nar, int minLevel, int maxLevel, String... extraFiles) {
        return files(nar, standard(minLevel, maxLevel, extraFiles)        );
    }

    public static PremiseDeriverRuleSet files(NAR nar, String... filename) {
        return files(nar, List.of(filename));
    }

    private static PremiseDeriverRuleSet files(NAR nar, Collection<String> filename) {
        return PremiseDeriverRuleSet.files(nar, filename);
    }
    public static PremiseDeriverRuleSet parse(NAR nar, String... rules) {
        return new PremiseDeriverRuleSet(nar, rules);
    }
}
