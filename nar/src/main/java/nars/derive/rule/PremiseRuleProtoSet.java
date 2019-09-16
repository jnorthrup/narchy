package nars.derive.rule;

import com.google.common.base.Splitter;
import jcog.memoize.CaffeineMemoize;
import jcog.util.ArrayUtil;
import nars.NAR;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
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
public class PremiseRuleProtoSet extends TreeSet<PremiseRuleProto> {

    @Deprecated public final NAR nar;

    public PremiseRuleProtoSet(NAR nar, String... rules) {
        this(nar, PremiseRule.parse(rules));
    }

    public PremiseRuleProtoSet(NAR nar, Stream<PremiseRule> r) {
        this.nar = nar;
        r.distinct().map(this::compile).collect(Collectors.toCollection(()->this));
    }

    private PremiseRuleProto compile(PremiseRule x) {
        return new PremiseRuleProto(x);
    }

    private final static Function<String, Collection<PremiseRule>> ruleFileCache = CaffeineMemoize.build((String n) -> {

        byte[] bb;
        try (InputStream nn = NAR.class.getClassLoader().getResourceAsStream(n)) {
            bb = nn.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            bb = ArrayUtil.EMPTY_BYTE_ARRAY;
        }
        return PremiseRule.parse(load(bb)).collect(Collectors.toSet());

    }, 32, false);



    public static Supplier<Collection<PremiseRule>> file(String n) {
        return ()-> PremiseRuleProtoSet.ruleFileCache.apply(n);
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


}

