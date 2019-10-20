













package jcog.grammar.synthesize;


import jcog.grammar.synthesize.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static jcog.grammar.synthesize.util.GrammarUtils.*;

public class MergesSynthesis {
    public static NodeMerges getMergesMultiple(Iterable<Node> roots, Predicate<String> oracle) {
        var merges = new NodeMerges();
        var processed = new NodeMerges();
        for (var first : roots) {
            for (var second : roots) {
                if (processed.contains(first, second)) {
                    continue;
                }
                processed.add(first, second);
                merges.addAll(getMergesSingle(first, second, oracle));
            }
        }
        return merges;
    }

    public static NodeMerges getMergesSingle(Node firstRoot, Node secondRoot, Predicate<String> oracle) {
        var merges = new NodeMerges();
        var processedMerges = new NodeMerges();
        var pairFirst = getAllExamples(firstRoot);
        var pairSecond = getAllExamples(secondRoot);
        for (var first : getAllNodes(firstRoot)) {
            for (var second : getAllNodes(secondRoot)) {
                if (processedMerges.contains(first, second)) {
                    continue;
                }
                processedMerges.add(first, second);
                getMergesHelper(first, second, pairFirst, pairSecond, oracle, merges);
            }
        }
        return merges;
    }

    private static void getMergesHelper(Node first, Node second, MultivalueMap<Node, String> firstExampleMap, MultivalueMap<Node, String> secondExampleMap, Predicate<String> oracle, NodeMerges merges) {
        if (first.equals(second)) {
            return;
        }
        if (!(first instanceof RepetitionNode) || !(second instanceof RepetitionNode)) {
            return;
        }
        var firstRep = ((RepetitionNode) first).rep;
        var secondRep = ((RepetitionNode) second).rep;
        if (firstRep instanceof ConstantNode || firstRep instanceof MultiConstantNode || secondRep instanceof ConstantNode || secondRep instanceof MultiConstantNode) {
            return;
        }
        if (isMultiAlternationRepetitionConstant(firstRep, true) || isMultiAlternationRepetitionConstant(secondRep, true)) {
            return;
        }
        Collection<String> firstExamplesSimple = new ArrayList<>();
        firstExamplesSimple.add(secondRep.getData().example + secondRep.getData().example);
        Collection<String> secondExamplesSimple = new ArrayList<>();
        secondExamplesSimple.add(firstRep.getData().example + firstRep.getData().example);
        if (!GrammarSynthesis.getCheck(oracle, firstRep.getData().context, firstExamplesSimple) || !GrammarSynthesis.getCheck(oracle, secondRep.getData().context, secondExamplesSimple)) {
            return;
        }
        Collection<String> firstExamples = secondExampleMap.get(secondRep).stream().map(example1 -> example1 + example1).collect(Collectors.toList());
        Collection<String> secondExamples = firstExampleMap.get(firstRep).stream().map(example -> example + example).collect(Collectors.toList());
        if ((isStructuredExample(firstRep) && isStructuredExample(secondRep))
                || (GrammarSynthesis.getCheck(oracle, firstRep.getData().context, firstExamples) && GrammarSynthesis.getCheck(oracle, secondRep.getData().context, secondExamples))) {
            Log.info("MERGE NODE FIRST:\n" + firstRep.getData().context.pre + " ## " + firstRep.getData().example + " ## " + firstRep.getData().context.post);
            Log.info("MERGE NODE SECOND:\n" + secondRep.getData().context.pre + " ## " + secondRep.getData().example + " ## " + secondRep.getData().context.post);
            merges.add(firstRep, secondRep);
        }
    }

    private static void getAllExamplesHelper(Node node, MultivalueMap<Node, String> examples) {
        for (var child : node.getChildren()) {
            getAllExamplesHelper(child, examples);
        }
        if (node instanceof RepetitionNode) {
            var repNode = (RepetitionNode) node;
            for (var example : examples.get(repNode.start)) {
                examples.add(repNode, example + repNode.rep.getData().example + repNode.end.getData().example);
            }
            for (var example : examples.get(repNode.rep)) {
                examples.add(repNode, repNode.start.getData().example + example + repNode.end.getData().example);
            }
            for (var example : examples.get(repNode.end)) {
                examples.add(repNode, repNode.start.getData().example + repNode.rep.getData().example + example);
            }
        } else if (node instanceof MultiConstantNode) {
            var mconstNode = (MultiConstantNode) node;
            var example = mconstNode.getData().example;
            for (var i = 0; i < mconstNode.characterChecks.size(); i++) {
                var pre = example.substring(0, i);
                var post = example.substring(i + 1);
                for (char c : mconstNode.characterChecks.get(i)) {
                    examples.add(mconstNode, pre + c + post);
                }
            }
        } else if (node instanceof AlternationNode) {
            var altNode = (AlternationNode) node;
            for (var example : examples.get(altNode.first)) {
                examples.add(altNode, example);
            }
            for (var example : examples.get(altNode.second)) {
                examples.add(altNode, example);
            }
        } else if (node instanceof ConstantNode) {
            examples.add(node, node.getData().example);
        } else if (node instanceof MultiAlternationNode) {
            for (var child : node.getChildren()) {
                for (var example : examples.get(child)) {
                    examples.add(node, example);
                }
            }
        } else {
            throw new RuntimeException("Invalid node type: " + node.getClass().getName());
        }
    }

    private static MultivalueMap<Node, String> getAllExamples(Node root) {
        var allExamples = new MultivalueMap<Node, String>();
        getAllExamplesHelper(root, allExamples);
        return allExamples;
    }


    private static boolean isMultiAlternationRepetitionConstant(Node node, boolean isParentRep) {
        return GrammarSynthesis.getMultiAlternationRepetitionConstantChildren(node, isParentRep).hasT();
    }

    private static boolean isStructuredExample(Node node) {
        for (var descendant : getDescendants(node)) {
            if (!(descendant instanceof MultiConstantNode)) {
                continue;
            }
            var mconstNode = (MultiConstantNode) descendant;
            for (var checks : mconstNode.characterChecks) {
                if (checks.size() == 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
