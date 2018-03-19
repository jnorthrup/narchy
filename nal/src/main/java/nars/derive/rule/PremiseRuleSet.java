package nars.derive.rule;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import nars.NAR;
import nars.Narsese;
import nars.index.term.PatternIndex;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Holds an set of derivation rules and a pattern index of their components
 */
public class PremiseRuleSet extends HashSet<PremiseRule> {

    private static final Pattern ruleImpl = Pattern.compile("\\|\\-");

    private static final Logger logger = LoggerFactory.getLogger(PremiseRuleSet.class);

    public static PremiseRuleSet rules(NAR nar, Collection<String> filename) {
        return rules(new PatternIndex(), nar, filename);
    }

    public static PremiseRuleSet rules(PatternIndex p, NAR nar, Collection<String> filename) {
        p.nar = nar;
        return new PremiseRuleSet(parsedRules(filename), p, nar);
    }

    final static Memoize<String, List<PremiseRule>> ruleCache =
            new SoftMemoize<>((String n) -> {
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
        return parse(load(bb)).distinct().collect(Collectors.toList());


    }, 32, true);

//    public static Stream<Pair<PremiseRule, String>> parsedRules(Collection<String> name) {
//        return name.stream().flatMap(n -> ruleCache.apply(n).stream());
//    }
    static Stream<PremiseRule> parsedRules(Collection<String> filenames) {
        return filenames.stream().flatMap(n -> ruleCache.apply(n).stream());
    }

    public PremiseRuleSet(NAR nar, String... rules) {
        this(new PatternIndex(), nar, rules);
    }

    public PremiseRuleSet(PatternIndex index, NAR nar, String... rules) {
        this(parse(Stream.of(rules)), index, nar);
    }

    public PremiseRuleSet(Stream<PremiseRule> parsed, PatternIndex patterns, NAR nar) {
        //HACK
        if (patterns.nar == null)
            patterns.nar = nar;
        else
            if (patterns.nar!=nar)
                throw new RuntimeException("wrong NAR ref");

        parsed.forEach(rule -> super.add(new CompileablePremiseRule(rule, patterns)));
    }

    @Override
    public boolean add(PremiseRule rule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends PremiseRule> c) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    static Stream<String> load(@NotNull byte[] data) {
        return preprocess(Streams.stream(Splitter.on('\n').split(new String(data))));
    }

    @NotNull
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


    final static Map<String, PremiseRule> lines = new ConcurrentHashMap<>(1024);
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


    public static Stream<PremiseRule> parse(Stream<String> rawRules) {

        return rawRules.map(src -> lines.computeIfAbsent(src, s -> {
            try {
                return PremiseRuleSet.parse(s);
            } catch (Narsese.NarseseException e) {
                logger.error("rule parse: {}:\t{}", e, src);
                return null;
            }
        })).filter(Objects::nonNull);

    }


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

    @NotNull
    public static PremiseRule parse(String src) throws Narsese.NarseseException {
        return new PremiseRule(parseRuleComponents(src), src);
    }

    @NotNull
    public static Subterms parseRuleComponents(@NotNull String src) throws Narsese.NarseseException {

        //(Compound) index.parseRaw(src)
        String[] ab = ruleImpl.split(src);
        if (ab.length != 2)
            throw new Narsese.NarseseException("Rule component must have arity=2, separated by \"|-\": " + src);

        String A = '(' + ab[0].trim() + ')';
        Term a = Narsese.term(A, false);
        if (!(a instanceof Compound))
            throw new Narsese.NarseseException("Left rule component must be compound: " + src);

        String B = '(' + ab[1].trim() + ')';
        Term b = Narsese.term(B, false);
        if (!(b instanceof Compound))
            throw new Narsese.NarseseException("Right rule component must be compound: " + src);

        return Subterms.subtermsInterned(a, b);
    }

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

