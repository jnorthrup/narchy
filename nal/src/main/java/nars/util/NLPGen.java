package nars.util;

import nars.*;
import nars.index.term.PatternIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static nars.Op.VAR_PATTERN;

/**
 * TODO not finished
 */
public class NLPGen {

    final NAR terminal = NARS.shell();
    final PatternIndex index = new PatternIndex();

    public interface Rule {
        @NotNull String get(Term t, float freq, float conf, Tense tense);
    }

    final List<Rule> rules = new ArrayList();

    public NLPGen() {
        train("A a B", "(A --> B).");
        train("A instancing B", "({A} --> B).");
        train("A propertied B", "(A --> [B]).");

        train("A same B", "(A <-> B).");
        train("A imply B", "(A ==> B).");
        //train("A not imply B", "(--,(A ==> B)).");

        train("A not a B", "(--,(A --> B)).");
        train("A not same B", "(--,(A <-> B)).");

        train("A or B", "(--,((--,A) && (--,B))).");
        train("A and B", "(A && B).");
        train("A, B, and C", "(&&,A,B,C).");
        train("A, B, C, and D", "(&&,A,B,C,D).");

        train("A and not B", "(A && (--,B)).");
        train("not A and not B", "((--,A) && (--,B)).");

    }


    private void train(String natural, String narsese) {
        final int maxVars = 6;
        for (int i = 0; i < maxVars; i++) {
            String v = String.valueOf((char) ('A' + i));
            narsese = narsese.replaceAll(v, '%' + v);
        }

        try {
            train(natural, Narsese.parse().task(narsese, terminal));
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

    }

    private void train(String natural, Task t) {
        Compound pattern = (Compound)(index.get(t.term(), true).term());

        rules.add((tt, freq, conf, tense) -> {
            if (timeMatch(t, tense)) {
                if (Math.abs(t.freq() - freq) < 0.33f) {
                    if (Math.abs(t.conf() - conf) < 0.33f) {

                        final String[] result = {null};

                        Unify u = new Unify(VAR_PATTERN, terminal.random(), Param.UnificationStackMax, terminal.matchTTLmax.intValue()) {

                            @Override
                            public void tryMatch() {


                                final String[] r = {natural};
                                xy.forEach((x, y) -> {
                                    String var = x.toString();
                                    if (!var.startsWith("%"))
                                        return;
                                    var = String.valueOf(((char) (var.charAt(1) - '1' + 'A'))); //HACK conversion between normalized pattern vars and the vars used in the input pattern
                                    r[0] = r[0].replace(var, y.toString());
                                });

                                result[0] = r[0];
                            }
                        };

                        u.unify(pattern, tt, true);

                        if (result[0]!=null)
                            return result[0];

                    }
                }
            }
            return null;
        });
    }

    private boolean timeMatch(@NotNull Task t, Tense tense) {
        return t.isEternal() && tense == Tense.Eternal;
        //TODO non-eternal case
    }

    /*public String toString(Term x, boolean tru) {
        return x.toString();
    }*/

    public String toString(@NotNull Term x, float freq, float conf, Tense tense) {
        for (Rule r : rules) {
            String y = r.get(x, freq, conf, tense);
            if (y != null)
                return y;
        }
        //DEFAULT
        return x.toString();
    }

    public String toString(@NotNull Task x) {
        return toString(x.term(), x.freq(), x.conf(), x.isEternal() ? Tense.Eternal : Tense.Present /* TODO */);
    }


}
