package nars.derive.rule;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import nars.NAR;
import nars.Narsese;
import nars.derive.DeriveRules;
import nars.derive.TrieDeriver;
import nars.index.term.PatternIndex;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * intermediate representation of a set of compileable Premise Rules
 */
public class DeriveRuleSet extends HashSet<DeriveRuleProto> {

    private static final Pattern ruleImpl = Pattern.compile("\\|\\-");

//    private static final Logger logger = LoggerFactory.getLogger(DeriveRuleSet.class);

    public static DeriveRuleSet load(NAR nar, Collection<String> filename) {
        PatternIndex p = new PatternIndex();
        p.nar = nar;
        return new DeriveRuleSet(filename.stream().flatMap(n -> ruleCache.apply(n).stream()), p, nar);
    }

    private final static Memoize<String, List<DeriveRuleSource>> ruleCache = new SoftMemoize<>((String n) -> {
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
        return DeriveRuleSource.parse(load(bb)).distinct().collect(Collectors.toList());


    }, 32, true);


    public DeriveRuleSet(NAR nar, String... rules) {
        this(new PatternIndex(), nar, rules);
    }

    DeriveRuleSet(PatternIndex index, NAR nar, String... rules) {
        this(DeriveRuleSource.parse(rules), index, nar);
    }

    private DeriveRuleSet(Stream<DeriveRuleSource> parsed, PatternIndex patterns, NAR nar) {
        //HACK
        if (patterns.nar == null)
            patterns.nar = nar;
        else
            if (patterns.nar!=nar)
                throw new RuntimeException("wrong NAR ref");

        parsed.forEach(rule -> super.add(new DeriveRuleProto(rule, patterns)));
    }

    @Override
    public boolean add(DeriveRuleProto rule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends DeriveRuleProto> c) {
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



    @NotNull
    static Subterms parseRuleComponents(@NotNull String src) throws Narsese.NarseseException {

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

    public DeriveRules compile() {
        return TrieDeriver.the(this, null);
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

