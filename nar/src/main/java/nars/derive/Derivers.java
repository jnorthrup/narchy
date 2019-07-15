package nars.derive;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleSet;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static nars.derive.rule.PremiseRuleSet.file;

/**
 * utility class for working witih Deriver's
 */
public enum Derivers { ;

    /** HACK range is inclusive */
    private static Stream<Supplier<Collection<PremiseRule>>> standard(int minLevel, int maxLevel, String... otherFiles) {

        List<Supplier<Collection<PremiseRule>>> f = new FasterList<>(16);

        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {
                case 1:
                    f.add(file("nal1.nal"));
                    f.add(file("nal1.guess.nal"));
                    //f.add(new NAL1_Guess()::get);
                    f.add(file("analogy.nal"));
                    break;

                case 2:
                    f.add(file("nal2.nal"));
                    f.add(file("nal2.guess.nal"));
                    f.add(file("nal2.member.nal"));
                    break;

                case 3:
                    f.add(file("nal3.nal"));
                    f.add(file("nal3.decompose.nal"));
                    //f.add(file("nal3.guess.nal"));
                    //f.add("nal3.decompose.extra.nal");
                    //f.add("nal3.induction.nal");
                    break;

                case 4:
                    f.add(file("nal4.nal"));
                    break;

                case 6:
                    f.add(file("induction.nal"));

                    f.add(file("nal6.nal"));
                    f.add(file("nal6.decompose.nal"));
                    f.add(file("nal6.guess.nal"));
                    //f.add(file("nal6.guess2.nal"));
//                    f.add(file("nal6.layer2.nal"));

                    //f.add("equivalence.nal");



                    //f.addAt("nal6.to.nal3.nal");
                    //f.addAt("hol.nal");

                        //files.addAt("nal6.misc.nal"); //<- suspect
                        //files.addAt("nal6.pedantic.nal"); //<- spam


                    break;


            }
        }

        for (String o : otherFiles)
            f.add(file(o));

        return f.stream().distinct();
    }


    /** standard ruleset */
    public static PremiseRuleSet nal(NAR nar, int minLevel, int maxLevel, String... extraFiles) {
        return rules(nar, standard(minLevel, maxLevel, extraFiles)        );
    }

    public static PremiseRuleSet files(NAR nar, String... filename) {
        return rules(nar, Stream.of(filename).map(PremiseRuleSet::file));
    }

    private static PremiseRuleSet rules(NAR nar, Stream<Supplier<Collection<PremiseRule>>> src) {
        return new PremiseRuleSet(nar, src.flatMap(x->x.get().stream()));
    }

    public static PremiseRuleSet parse(NAR nar, String... rules) {
        return new PremiseRuleSet(nar, rules);
    }
}
