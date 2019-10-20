package nars.term.util.map;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.tree.perfect.Trie;
import jcog.tree.perfect.TrieNode;
import jcog.tree.perfect.TrieSequencer;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;


/**
 * indexes sequences of (a perfectly-hashable fixed number
 * of unique) terms in a magnos trie
 */
public class TermPerfectTrie<K extends Term, V> extends Trie<List<K>, V> implements TrieSequencer<List<K>> {

    private final ObjectIntHashMap<Term> conds = new ObjectIntHashMap<>();

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        printSummary(this.root, out);
    }


    public TermPerfectTrie() {
        super(null);
    }



    private static <A, B> void printSummary(TrieNode<List<A>, B> node, PrintStream out) {

        node.forEach(n -> {
            var seq = n.seq();

            var from = n.start();

            out.print(n.childCount() + "|" + n.getSize() + "  ");

            Texts.indent(from * 4);

            out.println(Joiner.on(" , ").join(seq.subList(from, n.end())
                    
                    
            ));

            printSummary(n, out);
        });


    }


    
    @Deprecated
    public SummaryStatistics costAnalyze(@Nullable PrintStream o, FloatFunction<K> costFn) {

        var termCost = new SummaryStatistics();
        var sequenceLength = new SummaryStatistics();
        var branchFanOut = new SummaryStatistics();
        var endDepth = new SummaryStatistics();
        var currentDepth = new int[1];

        costAnalyze(costFn, termCost, sequenceLength, branchFanOut, endDepth, currentDepth, this.root);

        if (o != null) {
            o.println("termCost: " + s(termCost));
            o.println("sequenceLength: " + s(sequenceLength));
            o.println("branchFanOut: " + s(branchFanOut));
            o.println("endDepth: " + s(endDepth));
        }
        return termCost;
    }

    private static String s(SummaryStatistics s) {
        return s.getSummary().toString().replace('\n', ' ').replace("StatisticalSummaryValues: ", "");
    }

    private static <K, V> void costAnalyze(FloatFunction<K> costFn, SummaryStatistics termCost, SummaryStatistics sequenceLength, SummaryStatistics branchFanOut, SummaryStatistics endDepth, int[] currentDepth, TrieNode<List<K>, V> root) {

        var nc = root.childCount();
        if (nc > 0)
            branchFanOut.addValue(nc);

        root.forEach(n -> {
            var seq = n.seq();

            var from = n.start();


            var to = n.end();
            sequenceLength.addValue(to-from);

            for (var i = from; i < to; i++) {
                termCost.addValue(costFn.floatValueOf(seq.get(i)));
            }

            
            currentDepth[0]++;

            costAnalyze(costFn, termCost, sequenceLength, branchFanOut, endDepth, currentDepth, n);

            currentDepth[0]--;

            endDepth.addValue(currentDepth[0]);
        });
    }


    /**
     * override this to implement custom merging behavior; there can be only one
     */
    private void onMatch(K existing, K incoming) {

    }


    @Override
    public int matches(List<K> sequenceA, int indexA, List<K> sequenceB, int indexB, int count) {
        for (var i = 0; i < count; i++) {
            var a = sequenceA.get(i + indexA);
            var b = sequenceB.get(i + indexB);
            if (!a.equals(b))
                return i;
            else {
                onMatch(b, a);
            }
        }

        return count;
    }


    @Override
    public int lengthOf(List<K> sequence) {
        return sequence.size();
    }

    @Override
    public int hashOf(List<K> sequence, int index) {
        return conds.getIfAbsentPut(sequence.get(index), conds::size);
    }

}
