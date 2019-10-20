package nars.op.language;

import nars.*;
import nars.derive.premise.PatternTermBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.time.Tense;
import nars.unify.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nars.Op.VAR_PATTERN;

/**
 * TODO not finished
 * https://en.wikipedia.org/wiki/Receptive_aphasia
 */
public class NLPGen {

    final NAR terminal = NARS.shell();

    @FunctionalInterface
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
        final var maxVars = 6;
        for (var i = 0; i < maxVars; i++) {
            var v = String.valueOf((char) ('A' + i));
            narsese = narsese.replaceAll(v, '%' + v);
        }

        try {
            train(natural, Narsese.task(narsese, terminal));
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

    }

    private void train(String natural, Task t) {
        var pattern = (Compound)(PatternTermBuilder.patternify(t.term()).term());

        rules.add((tt, freq, conf, tense) -> {
            if (timeMatch(t, tense)) {
                if (Math.abs(t.freq() - freq) < 0.33f) {
                    if (Math.abs(t.conf() - conf) < 0.33f) {

                        String[] result = {null};

                        var u = new Unify(VAR_PATTERN, terminal.random(), NAL.unify.UNIFICATION_STACK_CAPACITY, terminal.deriveBranchTTL.intValue()) {

                            @Override
                            public boolean match() {


                                String[] r = {natural};
                                for (var entry : xy.entrySet()) {
                                    var x = entry.getKey();
                                    var y = entry.getValue();
                                    var var = x.toString();
                                    if (!var.startsWith("%"))
                                        continue;
                                    var = String.valueOf(((char) (var.charAt(1) - '1' + 'A')));
                                    r[0] = r[0].replace(var, y.toString());
                                }

                                result[0] = r[0];
                                return false;
                            }
                        };

                        u.unify(pattern, tt);

                        if (result[0]!=null)
                            return result[0];

                    }
                }
            }
            return null;
        });
    }

    private static boolean timeMatch(@NotNull Task t, Tense tense) {
        return t.isEternal() && tense == Tense.Eternal;
        
    }

    /*public String toString(Term x, boolean tru) {
        return x.toString();
    }*/

    public String toString(@NotNull Term x, float freq, float conf, Tense tense) {
        for (var r : rules) {
            var y = r.get(x, freq, conf, tense);
            if (y != null)
                return y;
        }
        
        return x.toString();
    }

    public String toString(@NotNull Task x) {
        return toString(x.term(), x.freq(), x.conf(), x.isEternal() ? Tense.Eternal : Tense.Present /* TODO */);
    }


}
