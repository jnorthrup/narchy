//package nars.derive;
//
//import jcog.math.IntRange;
//import jcog.pri.PLink;
//import jcog.pri.bag.impl.PLinkArrayBag;
//import jcog.pri.op.PriMerge;
//import nars.NAR;
//import nars.attention.TaskLinkWhat;
//import nars.derive.model.Derivation;
//import nars.derive.premise.Premise;
//import nars.derive.premise.PremiseSource;
//import nars.derive.rule.PremiseRuleSet;
//import nars.link.TaskLinks;
//import nars.time.When;
//import nars.time.event.WhenTimeIs;
//
//import java.util.function.BooleanSupplier;
//import java.util.function.Consumer;
//import java.util.function.Function;
//
///** balances exploration vs exploitation */
//public class MixDeriver extends Deriver {
//
//    final ThreadLocal<PLinkArrayBag<Function<Derivation,?>>> q = ThreadLocal.withInitial(()->
//            new PLinkArrayBag<>(PriMerge.max, 16));
//
//
//    public final IntRange premisesPerIteration = new IntRange(3, 1, 32);
//
//    public final IntRange termLinksPerTaskLink = new IntRange(1, 1, 8);
//
//
//    protected MixDeriver(PremiseRuleSet rules) {
//        super(rules);
//    }
//
//    @Override
//    protected void derive(Derivation d, BooleanSupplier kontinue) {
//        PremiseSource premises = d.premises;
//        premises.commit();
//
//        When<NAR> now = WhenTimeIs.now(d);
//        int matchTTL = matchTTL();
//        int deriveTTL = d.nar().deriveBranchTTL.intValue();
//        int premisesPerIteration = this.premisesPerIteration.intValue();
//        int termLinksPerTaskLink = this.termLinksPerTaskLink.intValue();
//        TaskLinks links = ((TaskLinkWhat) d.what).links;
//
//        PLinkArrayBag<Function<Derivation,?>> q = this.q.get();
//
//        do {
//
//
//            if (q.isEmpty()) {
//                //expand hypotheses
//                premises.premises(
//                    now,
//                    premisesPerIteration,
//                    termLinksPerTaskLink,
//                    links, d, (Premise p)->{
//
//                        PLink<Function<Derivation, ?>> explore = new PLink<>((Derivation D) -> {
//                            return p.match(D, matchTTL);
//                        }, p.task.pri() /* estimate */);
//
//                        q.put(explore);
//
//                    });
//            } else {
//                q.pop(d.random, 1, (Consumer)((exploit)->{
//
//                    Object exploitation = ((Function)exploit).apply(d);
//                    System.out.println(exploitation);
//                }));
//            }
//        } while (kontinue.getAsBoolean());
//    }
//}
