package jcog.learn.ntm.memory.address.content;

import jcog.learn.ntm.control.UVector;

import java.util.function.Function;
import java.util.stream.IntStream;

public class ContentAddressing   
{
    public final BetaSimilarity[] BetaSimilarities;
    public final UVector content;

    
    public ContentAddressing(BetaSimilarity[] betaSimilarities) {
        BetaSimilarities = betaSimilarities;
        content = new UVector(betaSimilarities.length);

        var max = BetaSimilarities[0].value;
        for(var iterationBetaSimilarity : betaSimilarities ) {
            max = Math.max(max, iterationBetaSimilarity.value);
        }

        var sum = 0.0;
        for (var i = 0; i < BetaSimilarities.length; i++) {
            var unit = BetaSimilarities[i];
            var weight = Math.exp(unit.value - max);
            content.value(i, weight);
            sum += weight;
        }
        content.valueMultiplySelf(1.0/sum);
    }

    public void backwardErrorPropagation() {


        var gradient = content.sumGradientValueProducts();

        for (var i = 0; i < content.size(); i++)        {
            BetaSimilarities[i].grad += (content.grad(i) - gradient) * content.value(i);
        }
    }

    public static ContentAddressing[] getVector(Integer x, Function<Integer, BetaSimilarity[]> paramGetter) {
        var vector = IntStream.range(0, x).mapToObj(i -> new ContentAddressing(paramGetter.apply(i))).toArray(ContentAddressing[]::new);
        return vector;
    }

}


