package nars.derive.rule;

import com.google.common.base.Splitter;
import jcog.data.set.ArrayHashSet;
import jcog.memoize.CaffeineMemoize;
import jcog.util.ArrayUtil;
import nars.NAR;
import nars.derive.action.How;
import nars.derive.action.PatternHow;
import nars.truth.func.NALTruth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * a set of related rules, forming a module that can be combined with other rules and modules
 * to form customized derivers, compiled together.
 *
 * intermediate representation of a set of compileable Premise Rules
 * TODO remove this class, just use Set<PremiseDeriverProto>'s
 */
public class PremiseRuleSet {

    @Deprecated public final NAR nar;
    public final ArrayHashSet<PremiseRule> rules;

    public PremiseRuleSet(NAR nar, String... rules) {
        this(nar, PatternHow.parse(rules));
    }

    public PremiseRuleSet(NAR nar, Stream<PremiseRule> r) {
        this(nar);
        r.collect(Collectors.toCollection(new Supplier<ArrayHashSet<PremiseRule>>() {
            @Override
            public ArrayHashSet<PremiseRule> get() {
                return rules;
            }
        }));
    }

    public PremiseRuleSet(NAR nar, ArrayHashSet<PremiseRule> r) {
        this.nar = nar;
        this.rules = r;
    }

    public PremiseRuleSet(NAR nar) {
        this(nar, new ArrayHashSet<>(1024));
    }


    private static final Function<String, Collection<PremiseRule>> ruleFileCache = CaffeineMemoize.build(new Function<String, Collection<PremiseRule>>() {
        @Override
        public Collection<PremiseRule> apply(String n) {

            byte[] bb;
            try (InputStream nn = NAR.class.getClassLoader().getResourceAsStream(n)) {
                bb = nn.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
                bb = ArrayUtil.EMPTY_BYTE_ARRAY;
            }
            return PatternHow.parse(load(bb), NALTruth.the).collect(Collectors.toList());

        }
    }, 64, false);



    public static Collection<PremiseRule> file(String n) {
        return PremiseRuleSet.ruleFileCache.apply(n);
    }

    private static Stream<String> load(byte[] data) {
        return preprocess(StreamSupport.stream(Splitter.on('\n').split(new String(data)).spliterator(), false));
    }

    private static Stream<String> preprocess(Stream<String> lines) {

        return lines.map(String::trim).filter(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return !s.isEmpty() && !s.startsWith("//");
            }
        }).map(new Function<String, String>() {
            @Override
            public String apply(String s) {

                String s1 = s;
                if (s1.contains("..")) {
                    s1 = s1.replace("A..", "%A.."); //add var pattern manually to ellipsis
                    //s = s.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
                    s1 = s1.replace("B..", "%B.."); //add var pattern manually to ellipsis
                    //s = s.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis
                }


                return s1;

            }
        });

    }


    public int size() {
        return rules.size();
    }

    public final DeriverProgram compile() {
        return compile(new UnaryOperator<How>() {
            @Override
            public How apply(How a) {
                return a;
            }
        });
    }

    public final DeriverProgram compile(UnaryOperator<How> actionTransform) {
        return PremiseRuleCompiler.the(rules, nar, actionTransform);
    }


    public final PremiseRuleSet add(String... metalNALRules) {
        return add(PatternHow.parse(metalNALRules));
    }

    public final PremiseRuleSet add(HowBuilder r) {
        return add(r.get());
    }

    private PremiseRuleSet add(PremiseRule r) {
        this.rules.add(r);
        return this;
    }

    public final PremiseRuleSet add(Stream<PremiseRule> r) {
        r.collect(Collectors.toCollection(new Supplier<ArrayHashSet<PremiseRule>>() {
            @Override
            public ArrayHashSet<PremiseRule> get() {
                return PremiseRuleSet.this.rules;
            }
        }));
        return this;
    }

}

