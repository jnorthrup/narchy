package jcog.learn.ntm.memory.address.content;

import jcog.learn.ntm.control.UVector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ContentAddressing   
{
    public final BetaSimilarity[] BetaSimilarities;
    public final UVector content;

    
    public ContentAddressing(BetaSimilarity[] betaSimilarities) {
        BetaSimilarities = betaSimilarities;
        content = new UVector(betaSimilarities.length);

        double max = BetaSimilarities[0].value;
        for(BetaSimilarity iterationBetaSimilarity : betaSimilarities ) {
            max = Math.max(max, iterationBetaSimilarity.value);
        }

        double sum = 0.0;
        for (int i = 0; i < BetaSimilarities.length; i++) {
            BetaSimilarity unit = BetaSimilarities[i];
            double weight = Math.exp(unit.value - max);
            content.value(i, weight);
            sum += weight;
        }
        content.valueMultiplySelf(1.0/sum);
    }

    public void backwardErrorPropagation() {


        double gradient = content.sumGradientValueProducts();

        for (int i = 0; i < content.size(); i++)        {
            BetaSimilarities[i].grad += (content.grad(i) - gradient) * content.value(i);
        }
    }

    public static ContentAddressing[] getVector(Integer x, Function<Integer, BetaSimilarity[]> paramGetter) {
        List<ContentAddressing> list = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            ContentAddressing contentAddressing = new ContentAddressing(paramGetter.apply(i));
            list.add(contentAddressing);
        }
        ContentAddressing[] vector = list.toArray(new ContentAddressing[0]);
        return vector;
    }

}


