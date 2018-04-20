package nars.derive.premise;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import jcog.memoize.QuickMemoize;
import nars.NAR;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * intermediate representation of a set of compileable Premise Rules
 * TODO remove this class, just use Set<PremiseDeriverProto>'s
 */
public class PremiseDeriverRuleSet extends HashSet<PremiseDeriverProto> {


    public final static QuickMemoize<String, List<PremiseDeriverSource>> ruleCache = new QuickMemoize<>(32, (String n) -> {
        InputStream nn = null;
        try {
            nn = ClassLoader.getSystemResource(n).openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
//                    InputStream nn = NAR.class.getResourceAsStream(
//                            //"nal/" + n
//                            n
//                    );
        byte[] bb;
        try {
            bb = nn.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            bb = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        return PremiseDeriverSource.parse(load(bb)).distinct().collect(Collectors.toList());


    });

    public final NAR nar;


    public PremiseDeriverRuleSet(NAR nar, String... rules) {
        this(new PremisePatternIndex(nar), rules);
    }

    PremiseDeriverRuleSet(PremisePatternIndex index, String... rules) {
        this(index, PremiseDeriverSource.parse(rules));
    }

    private PremiseDeriverRuleSet(PremisePatternIndex patterns, Stream<PremiseDeriverSource> parsed) {
        assert(patterns.nar!=null);
        this.nar = patterns.nar;
        parsed.forEach(rule -> super.add(new PremiseDeriverProto(rule, patterns)));
    }


    public static PremiseDeriverRuleSet files(NAR nar, Collection<String> filename) {
        return new PremiseDeriverRuleSet(
                new PremisePatternIndex(nar),
                filename.stream().flatMap(n -> PremiseDeriverRuleSet.ruleCache.apply(n).stream()));
    }

    @Override
    public boolean add(PremiseDeriverProto rule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends PremiseDeriverProto> c) {
        throw new UnsupportedOperationException();
    }

    static Stream<String> load(@NotNull byte[] data) {
        return preprocess(Streams.stream(Splitter.on('\n').split(new String(data))));
    }

    static Stream<String> preprocess(@NotNull Stream<String> lines) {

        return lines.map(s -> {

            s = s.trim(); //HACK write a better file loader

            if (s.isEmpty() || s.startsWith("//")) {
                return null;
            }

            if (s.contains("..")) {
                s = s.replace("A..", "%A.."); //add var pattern manually to ellipsis
                s = s.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
                s = s.replace("B..", "%B.."); //add var pattern manually to ellipsis
                s = s.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis
            }

//            if (s.startsWith("try:")) {
//                filtering = true;
//            }

            return s;
            //lines2.add(s);

        }).filter(Objects::nonNull);


//        if (filtering) {
//            StringBuilder current_rule = new StringBuilder(256);
//            List<String> unparsed_rules = $.newArrayList(1024);
//            for (String s : lines) {
//
//                s = s.trim(); //HACK write a better file loader
//
//                boolean currentRuleEmpty = current_rule.length() == 0;
//                if (s.startsWith("//") || spacePattern.matcher(s).replaceAll(Matcher.quoteReplacement("")).isEmpty()) {
//
//                    if (!currentRuleEmpty) {
//
//                        if (!filtering || filtering && current_rule.toString().contains("try:")) {
//                            unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
//                        }
//                        current_rule.setLength(0); //start identifying a new rule
//                    }
//
//                } else {
//                    //note, it can also be that the current_rule is not empty and this line contains |- which means
//                    //its already a new rule, in which case the old rule has to be added before we go on
//                    if (!currentRuleEmpty && s.contains("|-")) {
//
//                        if (!filtering || filtering && current_rule.toString().contains("try:")) {
//                            unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
//                        }
//                        current_rule.setLength(0); //start identifying a new rule
//
//                    }
//                    current_rule.append(s).append('\n');
//                }
//            }
//
//            if (current_rule.length() > 0) {
//                if (!filtering || filtering && current_rule.toString().contains("try:")) {
//                    unparsed_rules.add(current_rule.toString());
//                }
//            }
//
//            return unparsed_rules;
//            //.parallelStream();
//            //.stream();
//        } else {
//            return lines;//.stream();
//        }

    }




//    static {
//        Map<String, Compound> m;
//        try {
//            m = new FileHashMap<>();
//        } catch (IOException e) {
//            e.printStackTrace();
//            m = new ConcurrentHashMap();
//        }
//        lines = m;
//    }


//    final static com.github.benmanes.caffeine.cache.Cache<String, Pair<Compound, String>> lines = Caffeine.newBuilder()
//            .maximumSize(4 * 1024)
//            .builder();




//    public static PremiseRule[] parse(@NotNull TermIndex index, @NotNull String... src) {
//        return Util.map((s -> {
//            try {
//                return parse(s, index);
//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//                return null;
//            }
//        }), new PremiseRule[src.length], src);
//    }


    //    public void permute(@NotNull PremiseRule preNormRule, String src, @NotNull PatternIndex index, @NotNull Collection<PremiseRule> ur) {
//        add(preNormRule, src, ur, index,
//                (PremiseRule r) -> permuteSwap(r, src, index, ur,
//                        (PremiseRule s) -> permuteBackward(src, index, ur, r)));
//    }

//    void permuteBackward(String src, @NotNull PatternIndex index, @NotNull Collection<PremiseRule> ur, @NotNull PremiseRule r) {
//        if (permuteBackwards && r.permuteBackward) {
//
//            r.backwardPermutation(index, (q, reason) -> {
//                PremiseRule b = add(q, src + ':' + reason, ur, index);
//                //System.out.println("BACKWARD: " + b);
//
//                //                    //2nd-order backward
//                //                    if (forwardPermutes(b)) {
//                //                        permuteSwap(b, src, index, ur);
//                //                    }
//            });
//        }
//    }



//    public void permuteSwap(@NotNull PremiseRule r, String src, @NotNull PatternIndex index, @NotNull Collection<PremiseRule> ur, @NotNull Consumer<PremiseRule> then) {
//
//        then.accept(r);
//
//        if (permuteForwards && r.permuteForward) {
//
//            PremiseRule bSwap = r.swapPermutation(index);
//            if (bSwap != null)
//                then.accept(add(bSwap, src + ":forward", ur, index));
//        }
//
//    }


}

