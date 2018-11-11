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
        Set<String> f = new TreeSet();
        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {

                case 6:
                    f.add("induction.nal");

                    f.add("nal6.nal");
                    f.add("nal6.decompose.nal");
                    f.add("nal6.layer2.nal");
                    f.add("nal6.guess.nal");

                    f.add("nal6.to.nal3.nal");

                    //f.add("hol.nal");

                        //files.add("nal6.misc.nal"); //<- suspect
                        //files.add("nal6.pedantic.nal"); //<- spam


                    break;
                case 4:
                    f.add("nal4.nal");
                    f.add("nal4.guess.nal");
                    break;
                case 3:
                    f.add("nal3.nal");
                    f.add("nal3.guess.nal");
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

















    public static PremiseDeriverRuleSet rules(NAR nar, String... files) {
        return nal(nar, 0, 0, files);
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
