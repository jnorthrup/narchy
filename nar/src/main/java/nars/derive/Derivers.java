package nars.derive;

import jcog.data.set.ArrayHashSet;
import nars.NAR;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleSet;

import static nars.derive.rule.PremiseRuleSet.file;

/**
 * utility class for working witih Deriver's
 */
public enum Derivers { ;

    /** HACK range is inclusive */
    private static ArrayHashSet<PremiseRule> standard(int minLevel, int maxLevel, String... otherFiles) {

        ArrayHashSet<PremiseRule> f = new ArrayHashSet<>(1024);

        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {
                case 1:
                    f.addAll(file("nal1.nal"));
                    f.addAll(file("nal1.guess.nal"));
                    //f.add(new NAL1_Guess()::get);
                    f.addAll(file("analogy.nal"));
                    break;

                case 2:
                    f.addAll(file("nal2.nal"));
                    f.addAll(file("nal2.guess.nal"));
                    //f.addAll(file("nal2.member.nal"));
                    break;

                case 3:
                    f.addAll(file("nal3.nal"));
                    f.addAll(file("nal3.decompose.nal"));
                    //f.addAll(file("nal3.guess.nal"));
                    //f.add("nal3.decompose.extra.nal");
                    //f.add("nal3.induction.nal");
                    break;

                case 4:
                    f.addAll(file("nal4.nal"));
                    break;

                case 6:
                    f.addAll(file("induction.nal"));

                    f.addAll(file("nal6.nal"));
                    f.addAll(file("nal6.strong.nal"));
                    f.addAll(file("nal6.decompose.nal"));
                    f.addAll(file("nal6.free.nal"));
                    f.addAll(file("nal6.guess.nal"));
                    f.addAll(file("nal6.guess2.nal"));
                    //f.addAll(file("nal6.to.nal3.nal"));
                    //f.addAll(file("hol.nal"));

                    //                    f.addAll(file("nal6.layer2.nal"));
                    //f.add("equivalence.nal");


                        //files.add("nal6.misc.nal"); //<- suspect
                        //files.add("nal6.pedantic.nal"); //<- spam


                    break;


            }
        }

        for (String o : otherFiles)
            f.addAll(file(o));

        return f;
    }


    /** standard ruleset */
    public static PremiseRuleSet nal(NAR nar, int minLevel, int maxLevel, String... extraFiles) {
        return new PremiseRuleSet(nar, standard(minLevel, maxLevel, extraFiles));
    }

    @Deprecated public static PremiseRuleSet files(NAR nar, String... filename) {
        return new PremiseRuleSet(nar, standard(0,0,filename));
    }

}
