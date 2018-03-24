package nars.derive.rule;

import nars.$;
import nars.subterm.Subterms;
import nars.term.ProxyTerm;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class DeriveRuleSource extends ProxyTerm {

    public final String source;

    public DeriveRuleSource(Subterms premiseAndResult, String src) {
        super($.pFast(premiseAndResult));
        this.source = src;
    }


}





