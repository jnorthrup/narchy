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
public class PremiseRuleSet extends TreeSet<PremiseRule> {

    @Deprecated public final NAR nar;

    public PremiseRuleSet(NAR nar, String... rules) {
        this(nar, PremiseRuleBuilder.parse(rules));
    }

    public PremiseRuleSet(NAR nar, Stream<PremiseRuleBuilder> r) {
        this.nar = nar;
        r.distinct().map(PremiseRuleBuilder::get).collect(Collectors.toCollection(()->this));
    }

    private final static Function<String, Collection<PremiseRuleBuilder>> ruleFileCache = CaffeineMemoize.build((String n) -> {

        byte[] bb;
        try (InputStream nn = NAR.class.getClassLoader().getResourceAsStream(n)) {
            bb = nn.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            bb = ArrayUtil.EMPTY_BYTE_ARRAY;
        }
        return PremiseRuleBuilder.parse(load(bb)).collect(Collectors.toSet());

    }, 32, false);



    public static Supplier<Collection<PremiseRuleBuilder>> file(String n) {
        return ()-> PremiseRuleSet.ruleFileCache.apply(n);
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

