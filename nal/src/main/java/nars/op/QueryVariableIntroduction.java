package nars.op;

public class QueryVariableIntroduction {

    /**
     *
     * @param nar
     * @param capacity
     * @param selectionRate amount percent of capacity processed per frame, 0 <= x <= 1
     * @param priority pri multiplier scale
     * @return
     */
//    public static MutaTaskBag queryVariableIntroduction(NAR nar, int capacity, float selectionRate, float priority) {

//        return new MutaTaskBag(
//                new VarIntroduction(2 /* max iterations */) {
//
//                    @Override
//                    protected Term[] next(Compound input, Term selection, int iteration) {
//                        return new Term[] {  $.varQuery("q" + iteration) };
//                    }
//
//                    @Nullable
//                    @Override
//                    protected Term[] nextSelection(Compound input) {
//                        return Terms.substMaximal(input, this::canIntroduce, 2, 3);
//                    }
//
//                    protected boolean canIntroduce(Term subterm) {
//                        return !subterm.op().var;
//                    }
//
//                    @Override
//                    protected Task clone(@NotNull Task original, Compound c) {
//                        Task t = super.clone(original, c);
//                        t.budget().mul(priority, 1f, 1f); //decrease in proportion to the input term's volume
//                        return t;
//                    }
//                },
//                (t) -> {
//                    return !(t.cyclic());  //!(instanceof VarIntroTask..
//                },
//                selectionRate,
//                new CurveBag<>(
//                        Math.max(1, capacity),
//                        new CurveBag.NormalizedSampler(CurveBag.power2BagCurve, nar.random),
//                        BudgetMerge.plusBlend,
//                        new ConcurrentHashMap<>(capacity)),
//                nar);
//  }

}
