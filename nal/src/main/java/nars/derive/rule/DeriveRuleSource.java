package nars.derive.rule;

import nars.$;
import nars.Narsese;
import nars.derive.PostCondition;
import nars.index.term.PatternIndex;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.transform.TermTransform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static nars.Op.VAR_PATTERN;
import static nars.derive.rule.DeriveRuleSet.parseRuleComponents;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class DeriveRuleSource extends ProxyTerm {

    public final String source;

    public DeriveRuleSource(String ruleSrc) throws Narsese.NarseseException {
        super(
            $.pFast(parseRuleComponents(ruleSrc))
                .transform(UppercaseAtomsToPatternVariables)
        );
        this.source = ruleSrc;
    }

    protected DeriveRuleSource(DeriveRuleSource raw, PatternIndex index)  {
        super(index.pattern(raw.term()));
        this.source = raw.source;
    }

    static final TermTransform UppercaseAtomsToPatternVariables = new TermTransform() {
        @Override
        public Termed transformAtomic(Term atomic) {
            if (atomic instanceof Atom) {
                if (!PostCondition.reservedMetaInfoCategories.contains(atomic)) { //do not alter keywords
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return $.v(VAR_PATTERN, atomic.toString());
                    }
                }
            }
            return atomic;
        }


    };

    public static DeriveRuleSource parse(String ruleSrc) throws Narsese.NarseseException {
        return new DeriveRuleSource(ruleSrc);
    }

    public static Stream<DeriveRuleSource> parse(String... rawRules) {
        return parse(Stream.of(rawRules));
    }

    public static Stream<DeriveRuleSource> parse(Stream<String> rawRules) {

        return rawRules.map(src -> lines.computeIfAbsent(src, s -> {
            try {
                return parse(s);
            } catch (Narsese.NarseseException e) {
                throw new RuntimeException("rule parse: " + e + "\n\t" + src);
            }
        }));
        //.filter(Objects::nonNull);

    }

    /** line parse cache */
    private final static Map<String, DeriveRuleSource> lines = new ConcurrentHashMap<>(1024);

}





