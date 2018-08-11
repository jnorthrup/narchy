package nars.term.util;

import com.google.common.base.Joiner;
import jcog.tree.perfect.Trie;
import jcog.tree.perfect.TrieNode;
import jcog.tree.perfect.TrieSequencer;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;


/**
 * indexes sequences of (a perfectly-hashable fixed number
 * of unique) terms in a magnos trie
 */
public class TermTrie<K extends Term, V> extends Trie<List<K>, V> implements TrieSequencer<List<K>> {

    final ObjectIntHashMap<Term> conds = new ObjectIntHashMap<>();

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        printSummary(this.root, out);
    }


    public TermTrie() {
        super(null);
    }



    public static <A, B> void printSummary(TrieNode<List<A>, B> node, PrintStream out) {

        node.forEach(n -> {
            List<A> seq = n.seq();

            int from = n.start();

            out.print(n.childCount() + "|" + n.getSize() + "  ");

            indent(from * 4);

            out.println(Joiner.on(" , ").join(seq.subList(from, n.end())
                    
                    
            ));

            printSummary(n, out);
        });


    }


    
    @NotNull
    @Deprecated
    public SummaryStatistics costAnalyze(FloatFunction<K> costFn, @Nullable PrintStream o) {

        SummaryStatistics termCost = new SummaryStatistics();
        SummaryStatistics sequenceLength = new SummaryStatistics();
        SummaryStatistics branchFanOut = new SummaryStatistics();
        SummaryStatistics endDepth = new SummaryStatistics();
        int[] currentDepth = new int[1];

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

    public static <K, V> void costAnalyze(FloatFunction<K> costFn, SummaryStatistics termCost, SummaryStatistics sequenceLength, SummaryStatistics branchFanOut, SummaryStatistics endDepth, int[] currentDepth, TrieNode<List<K>, V> root) {

        int nc = root.childCount();
        if (nc > 0)
            branchFanOut.addValue(nc);

        root.forEach(n -> {
            List<K> seq = n.seq();

            int from = n.start();

            

            int to = n.end();
            sequenceLength.addValue(to-from);

            for (int i = from; i < to; i++) {
                termCost.addValue(costFn.floatValueOf(seq.get(i)));
            }

            
            currentDepth[0]++;

            costAnalyze(costFn, termCost, sequenceLength, branchFanOut, endDepth, currentDepth, n);

            currentDepth[0]--;

            endDepth.addValue(currentDepth[0]);
        });
    }

    public static void indent(int amount) {
        for (int i = 0; i < amount; i++) {
            System.out.print(' ');
        }
    }



    /**
     * override this to implement custom merging behavior; there can be only one
     */
    protected void onMatch(K existing, K incoming) {

    }


    @Override
    public int matches(List<K> sequenceA, int indexA, List<K> sequenceB, int indexB, int count) {
        for (int i = 0; i < count; i++) {
            K a = sequenceA.get(i + indexA);
            K b = sequenceB.get(i + indexB);
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
