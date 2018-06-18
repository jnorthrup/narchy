package nars.nal.multistep;

/** see: https://github.com/aindilis/madla-planner/blob/master/mapddl-benchmarks/blocksworld/domain.pddl */
class BlocksWorldTest {
    /* ex:
    (
        (&|,pickUp($a,$x),clear($x),onTable($x),empty(hand($a)))
            =|>
        (&|,--onTable($x),--clear($x),--empty(hand($a)),holding($a,$x))
    )
     */
}
