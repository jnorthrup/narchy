package nars.derive.premise;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import jcog.data.set.ArrayUnenforcedSet;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.Memoize;
import nars.NAR;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * intermediate representation of a set of compileable Premise Rules
 * TODO remove this class, just use Set<PremiseDeriverProto>'s
 */
public class PremiseDeriverRuleSet extends ArrayUnenforcedSet<PremiseRuleProto> {

    public NAR nar;

    public PremiseDeriverRuleSet(NAR nar, String... rules) {
        this(new PremisePatternIndex(nar), rules);
    }

    public PremiseDeriverRuleSet(PremisePatternIndex index, String... rules) {
        this(index, PremiseRuleSource.parse(rules));
    }

    private PremiseDeriverRuleSet(PremisePatternIndex patterns, Stream<PremiseRuleSource> parsed) {
        assert (patterns.nar != null);
        this.nar = patterns.nar;
        parsed.distinct().map(x -> x.apply(patterns)).forEach(super::add);
    }

    private final static Memoize<String, Collection<PremiseRuleSource>> ruleCache = CaffeineMemoize.build((String n) -> {

        byte[] bb;

        try (InputStream nn = NAR.class.getClassLoader().getResourceAsStream(n)) {

            bb = nn.readAllBytes();

        } catch (IOException e) {

            e.printStackTrace();
            bb = ArrayUtils.EMPTY_BYTE_ARRAY;

        }
        return (PremiseRuleSource.parse(load(bb)).collect(Collectors.toSet()));

    }, 32, false);

    public static PremiseDeriverRuleSet files(NAR nar, Collection<String> filename) {
        return new PremiseDeriverRuleSet(
                new PremisePatternIndex(nar),
                filename.stream().flatMap(n -> PremiseDeriverRuleSet.ruleCache.apply(n).stream()));
    }

    private static Stream<String> load(byte[] data) {
        return preprocess(Streams.stream(Splitter.on('\n').split(new String(data))));
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

