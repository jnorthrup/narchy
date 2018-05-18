package nars.derive.premise;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.util.term.transform.TermTransform;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseDeriverSource extends ProxyTerm {

    public final String source;

    public PremiseDeriverSource(String ruleSrc) throws Narsese.NarseseException {
        super(
            $.pFast(parseRuleComponents(ruleSrc))
                .transform(new UppercaseAtomsToPatternVariables())
        );

        this.source = ruleSrc;
    }

    protected PremiseDeriverSource(PremiseDeriverSource raw, PremisePatternIndex index)  {
        super((index.pattern(raw.ref)));
        this.source = raw.source;
    }

    static class UppercaseAtomsToPatternVariables extends UnifiedMap<String,Term> implements TermTransform {

        public UppercaseAtomsToPatternVariables() { super(8); }

        @Override
        public Termed transformAtomic(Term atomic) {
            if (atomic instanceof Atom) {
                if (!PostCondition.reservedMetaInfoCategories.contains(atomic)) { //do not alter keywords
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return this.computeIfAbsent(name, (n)->$.varPattern(1 + this.size()));
                        //return $.v(VAR_PATTERN, name);
                    }
                }
            }
            return atomic;
        }

    }

    public static PremiseDeriverSource parse(String ruleSrc) throws Narsese.NarseseException {
        return new PremiseDeriverSource(ruleSrc);
    }

    public static Stream<PremiseDeriverSource> parse(String... rawRules) {
        return parse(Stream.of(rawRules));
    }

    public static Stream<PremiseDeriverSource> parse(Stream<String> rawRules) {
        return rawRules.map(src -> { //src -> lines.computeIfAbsent(src, s -> {
            try {
                return parse(src);
            } catch (Narsese.NarseseException e) {
                throw new RuntimeException("rule parse: " + e + "\n\t" + src);
            }
        });
        //.filter(Objects::nonNull);
    }

//    /** line parse cache */
//    private final static Map<String, PremiseDeriverSource> lines = new ConcurrentHashMap<>(1024);

    static final Pattern ruleImpl = Pattern.compile("\\|\\-");

    static Subterms parseRuleComponents(String src) throws Narsese.NarseseException {

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

        return Op.terms.subtermsInstance(a, b); //direct
    }
}





