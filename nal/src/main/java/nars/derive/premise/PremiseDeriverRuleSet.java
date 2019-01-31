package nars.derive.premise;

import com.google.common.base.Splitter;
import jcog.data.set.ArrayUnenforcedSet;
import jcog.memoize.Memoizers;
import jcog.util.ArrayUtils;
import nars.NAR;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * intermediate representation of a set of compileable Premise Rules
 * TODO remove this class, just use Set<PremiseDeriverProto>'s
 */
public class PremiseDeriverRuleSet extends ArrayUnenforcedSet<PremiseRuleProto> {

    public final NAR nar;

    public PremiseDeriverRuleSet(NAR nar, String... rules) {
        this(nar, PremiseRuleSource.parse(rules));
    }

    private PremiseDeriverRuleSet(NAR nar, Stream<PremiseRuleSource> parsed) {
        this.nar = nar;
        parsed.distinct().map(x -> new PremiseRuleProto(x, nar)).forEach(super::add);
    }

    private final static Function<String, Collection<PremiseRuleSource>> ruleCache = Memoizers.the.memoize(
            PremiseDeriverRuleSet.class.getSimpleName() + "_rule", 32,
            (String n) -> {

        byte[] bb;

        try (InputStream nn = NAR.class.getClassLoader().getResourceAsStream(n)) {

            bb = nn.readAllBytes();

        } catch (IOException e) {

            e.printStackTrace();
            bb = ArrayUtils.EMPTY_BYTE_ARRAY;

        }
        return (PremiseRuleSource.parse(load(bb)).collect(Collectors.toSet()));

    });


    public static PremiseDeriverRuleSet files(NAR nar, Collection<String> filename) {
        return new PremiseDeriverRuleSet(
                nar,
                filename.stream().flatMap(n -> PremiseDeriverRuleSet.ruleCache.apply(n).stream()));
    }

    private static Stream<String> load(byte[] data) {
        return preprocess(StreamSupport.stream(Splitter.on('\n').split(new String(data)).spliterator(), false));
    }

    private static Stream<String> preprocess(Stream<String> lines) {

        return lines.map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("//")).map(s -> {

            if (s.contains("..")) {
                s = s.replace("A..", "%A.."); //addAt var pattern manually to ellipsis
                //s = s.replace("%A..B=_", "%A..%B=_"); //addAt var pattern manually to ellipsis
                s = s.replace("B..", "%B.."); //addAt var pattern manually to ellipsis
                //s = s.replace("%A.._=B", "%A.._=%B"); //addAt var pattern manually to ellipsis
            }


            return s;

        });


    }


}

