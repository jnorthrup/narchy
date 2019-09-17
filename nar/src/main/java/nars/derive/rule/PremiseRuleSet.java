package nars.derive.rule;

import com.google.common.base.Splitter;
import jcog.data.set.ArrayHashSet;
import jcog.memoize.CaffeineMemoize;
import jcog.util.ArrayUtil;
import nars.NAR;
import nars.derive.action.PatternPremiseAction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
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
        this(nar, PatternPremiseAction.parse(rules));
    }

    public PremiseRuleSet(NAR nar, Stream<PremiseRule> r) {
        this(nar);
        r.collect(Collectors.toCollection(()->rules));
    }

    public PremiseRuleSet(NAR nar, ArrayHashSet<PremiseRule> r) {
        this.nar = nar;
        this.rules = r;
    }

    public PremiseRuleSet(NAR nar) {
        this(nar, new ArrayHashSet<>(1024));
    }

    private final static Function<String, Collection<PremiseRule>> ruleFileCache = CaffeineMemoize.build((String n) -> {

        byte[] bb;
        try (InputStream nn = NAR.class.getClassLoader().getResourceAsStream(n)) {
            bb = nn.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            bb = ArrayUtil.EMPTY_BYTE_ARRAY;
        }
        return PatternPremiseAction.parse(load(bb)).collect(Collectors.toList());

    }, 64, false);



    public static Collection<PremiseRule> file(String n) {
        return PremiseRuleSet.ruleFileCache.apply(n);
    }

    private static Stream<String> load(byte[] data) {
        return preprocess(StreamSupport.stream(Splitter.on('\n').split(new String(data)).spliterator(), false));
    }

    private static Stream<String> preprocess(Stream<String> lines) {

        return lines.map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("//")).map(s -> {

            if (s.contains("..")) {
                s = s.replace("A..", "%A.."); //add var pattern manually to ellipsis
                //s = s.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
                s = s.replace("B..", "%B.."); //add var pattern manually to ellipsis
                //s = s.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis
            }


            return s;

        });


    }


    public int size() {
        return rules.size();
    }

    public final DeriverProgram compile() {
        return PremiseRuleCompiler.the(rules, nar);
    }

    public final PremiseRuleSet add(String... metalNALRules) {
        return add(PatternPremiseAction.parse(metalNALRules));
    }

    public final PremiseRuleSet add(PremiseRuleBuilder r) {
        return add(r.get());
    }

    public final PremiseRuleSet add(PremiseRule r) {
        this.rules.add(r);
        return this;
    }

    public final PremiseRuleSet add(Collection<PremiseRule> r) {
        this.rules.addAll(r);
        return this;
    }

    public final PremiseRuleSet add(Stream<PremiseRule> r) {
        r.collect(Collectors.toCollection(()->this.rules));
        return this;
    }

}

