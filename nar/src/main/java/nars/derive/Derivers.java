package nars.derive;

import nars.NAR;
import nars.derive.rule.PremiseRuleSet;

import java.util.*;

/**
 * utility class for working witih Deriver's
 */
public enum Derivers { ;

    /** HACK range is inclusive */
    private static Set<String> standard(int minLevel, int maxLevel, String... otherFiles) {
        Set<String> f = new TreeSet();
        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {

                case 6:
                    f.add("induction.nal");

                    f.add("nal6.nal");
                    f.add("nal6.decompose.nal");

                    f.add("equivalence.nal");

                    //f.add("nal6.guess.nal");
                    //f.add("nal6.layer2.nal");
                    //f.addAt("nal6.to.nal3.nal");
                    //f.addAt("hol.nal");

                        //files.addAt("nal6.misc.nal"); //<- suspect
                        //files.addAt("nal6.pedantic.nal"); //<- spam


                    break;
                case 4:
                    f.add("nal4.nal");
                    break;
                case 3:
                    f.add("nal3.nal");
                    f.add("nal3.decompose.nal");
                    //f.add("nal3.guess.nal");
                    //f.add("nal3.decompose.extra.nal");
                    //f.add("nal3.induction.nal");
                    break;
                case 2:
                    f.add("nal2.nal");
                    f.add("nal2.guess.nal");
                    break;
                case 1:
                    f.add("analogy.nal");
                    f.add("nal1.nal");
                    f.add("nal1.guess.nal");
                    break;
            }
        }

        Collections.addAll(f, otherFiles);

        return f;
    }


    /** standard ruleset */
    public static PremiseRuleSet nal(NAR nar, int minLevel, int maxLevel, String... extraFiles) {
        return files(nar, standard(minLevel, maxLevel, extraFiles)        );
    }

    public static PremiseRuleSet files(NAR nar, String... filename) {
        return files(nar, List.of(filename));
    }

    private static PremiseRuleSet files(NAR nar, Collection<String> filename) {
        return PremiseRuleSet.files(nar, filename);
    }
    public static PremiseRuleSet parse(NAR nar, String... rules) {
        return new PremiseRuleSet(nar, rules);
    }
}
