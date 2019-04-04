package nars.util;

import jcog.data.set.ArrayHashSet;
import jcog.data.set.MetalLongSet;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.table.ARFF;
import jcog.table.DataTable;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.truth.Stamp;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Row;

import java.io.IOException;
import java.util.Random;

import static nars.Op.*;

public class NALDataTableTest {

    final NAR n = NARS.tmp();

    @Test
    public void validatePredictionXOR() throws Exception {
        n.beliefPriDefault.set(0.02f);
        n.questionPriDefault.set(0.5f);
        n.termVolumeMax.set(16);
        n.log();
        validatePrediction(n, xorARFF
                //,"--(0<->1)."
        ,"XOR(?1,0)?" ,"XOR(?1,1)?"
        );
    }

    @Test
    public void validatePredictionShuttle() throws Exception {
        n.log();

        validatePrediction(n, shuttleARFF);
    }

    @Test
    public void validatePredictionIris() throws Exception {
        validatePrediction(n, irisARFF);
    }


    static NAR validatePrediction(NAR n, String arffData, String... hints) throws IOException, ARFF.ARFFParseError {
        ArrayHashSet<Row> data = new ArrayHashSet<>();

        DataTable dataset = new ARFF(arffData) {
            @Override
            public boolean add(Object... point) {
                
                if (arffData == irisARFF) {
                    
                    for (int i = 0, pointLength = point.length; i < pointLength; i++) {
                        Object x = point[i];
                        if (x instanceof Number) {
                            point[i] = Math.round(((Number) x).floatValue() * 10);
                        }
                    }
                }
                return super.add(point);
            }
        };


        int originalDataSetSize = data.size();

        Random rng = new XoRoShiRo128PlusRandom(1);


//        int validationSetSize = 1;
//        Collection<Row> validationPointsActual = new FasterList(validationSetSize);
//        Collection<Row> validationPoints = new FasterList(validationSetSize);
//        for (int i= 0; i < validationSetSize; i++) {
//            Row randomPoint = data.remove(rng);
//            validationPointsActual.addAt(randomPoint);
//
//            MutableList randomPointErased = randomPoint.toList();
//            randomPointErased.setAt(randomPointErased.size()-1, "?class");
//            validationPoints.addAt(randomPointErased.toImmutable());
//        }
//
//        ARFF validation = dataset.clone(validationPoints);
//        validation.print();

//        assertEquals(originalDataSetSize, validationPoints.size() + data.size());






        MetalLongSet questions = new MetalLongSet(4);
        n.onTask(t->{
            if (t.isInput())
                questions.add(t.stamp()[0]);
        }, QUESTION, QUEST);

        n.onTask(t->{
            if (Stamp.overlapsAny(questions, t.stamp())) {
                //if (t.isInput())
                    System.out.println("ANSWER: " + t);
            }
        }, BELIEF, GOAL);



        NALData.believe(n, dataset, NALData.predictsLast);



//        Task[] questions1 = NALSchema.data(n, validation, QUESTION, NALSchema.predictsLast).toArray(Task[]::new);
//        new DialogTask(n, questions1) {
//            @Override
//            protected boolean onTask(Task x, Term unifiedWith) {
//                System.out.println(unifiedWith + ": " + x);
//                return super.onTask(x, unifiedWith);
//            }
//            //            @Override
////            protected boolean onTask(Task x) {
////                if (!x.isInput())
////                    System.out.println(x);
////                return true;
////            }
//        };

        try {
            n.input(hints);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

        n.run(5000);
        return n;
    }

    /** https://github.com/renatopp/arff-datasets/blob/master/boolean/xor.arff */
    static final String xorARFF = "%\n" +
            "% XOR\n" +
            "%\n" +
            '\n' +
            "@RELATION XOR\n" +
            '\n' +
            "@ATTRIBUTE input1 REAL\n" +
            "@ATTRIBUTE input2 REAL\n" +
            "@ATTRIBUTE y REAL\n" +
            '\n' +
            "@DATA\n" +
            "0.0,0.0,0.0\n" +
            "0.0,1.0,1.0\n" +
            "1.0,0.0,1.0\n" +
            "1.0,1.0,0.0\n";

    static final String shuttleARFF = "% 1. Title: Space Shuttle Autolanding Domain\n" +
            "% \n" +
            "% 2. Sources:\n" +
            "%     (a) Original source: unknown\n" +
            "%         -- NASA: Mr. Roger Burke's autolander design team\n" +
            "%     (b) Donor: Bojan Cestnik\n" +
            "%                 Jozef Stefan Institute\n" +
            "%                 Jamova 39\n" +
            "%                 61000 Ljubljana\n" +
            "%                 Yugoslavia (tel.: (38)(+61) 214-399 ext.287) \n" +
            "%     (c) Date: November 1988\n" +
            "% \n" +
            "% 3. Past Usage: (several, it appears)\n" +
            "%      Example: Michie,D. (1988).  The Fifth Generation's Unbridged Gap.\n" +
            "%               In Rolf Herken (Ed.) The Universal Turing Machine: A\n" +
            "%               Half-Century Survey, 466-489, Oxford University Press.\n" +
            "% \n" +
            "% 4. Relevant Information:\n" +
            "%      This is a tiny database.  Michie reports that Burke's group used\n" +
            "%      RULEMASTER to generate comprehendable rules for determining\n" +
            "%      the conditions under which an autolanding would be preferable to\n" +
            "%      manual control of the spacecraft.\n" +
            "% \n" +
            "% 5. Number of Instances: 15\n" +
            "% \n" +
            "% 6. Number of Attributes: 7 (including the class attribute)\n" +
            "% \n" +
            "% 7. Attribute Information:\n" +
            "%     1. STABILITY: stab, xstab\n" +
            "%     2. ERROR: XL, LX, MM, SS\n" +
            "%     3. SIGN: pp, nn\n" +
            "%     4. WIND: head, tail\n" +
            "%     5. MAGNITUDE: Low, Medium, Strong, OutOfRange\n" +
            "%     6. VISIBILITY: yes, no\n" +
            "%     7. Class: noauto, auto\n" +
            "%        -- that is, advise using manual/automatic control\n" +
            "% \n" +
            "% 8. Missing Attribute Values:\n" +
            "%    -- none\n" +
            "%    -- but several \"don't care\" values: (denoted by \"_\")\n" +
            "%          Attribute Number:   Number of Don't Care Values:\n" +
            "%                         2:   2\n" +
            "%                         3:   3\n" +
            "%                         4:   8\n" +
            "%                         5:   8\n" +
            "%                         6:   5\n" +
            "%                         7:   0\n" +
            "% \n" +
            "% 9. Class Distribution:\n" +
            "%     1. Use noauto control: 6\n" +
            "%     2. Use automatic control: 9%\n" +
            "% Information about the dataset\n" +
            "% CLASSTYPE: nominal\n" +
            "% CLASSINDEX: first\n" +
            "%\n" +
            "@relation shuttle-landing-control\n" +
            "@attribute STABILITY {1,2}\n" +
            "@attribute ERROR {1,2,3,4}\n" +
            "@attribute SIGN {1,2}\n" +
            "@attribute WIND {1,2}\n" +
            "@attribute MAGNITUDE {1,2,3,4}\n" +
            "@attribute VISIBILITY {vis,invis}\n" +
            "@attribute Class {manual,auto}\n" +
            "@data\n" +
            "_,_,_,_,_,invis,auto\n" +
            "2,_,_,_,_,vis,manual\n" +
            "1,2,_,_,_,vis,manual\n" +
            "1,1,_,_,_,vis,manual\n" +
            "1,3,2,2,_,vis,manual\n" +
            "_,_,_,_,4,vis,manual\n" +
            "1,4,_,_,1,vis,auto\n" +
            "1,4,_,_,2,vis,auto\n" +
            "1,4,_,_,3,vis,auto\n" +
            "1,3,1,1,1,vis,auto\n" +
            "1,3,1,1,2,vis,auto\n" +
            "1,3,1,2,1,vis,auto\n" +
            "1,3,1,2,2,vis,auto\n" +
            "1,3,1,1,3,vis,manual\n" +
            "1,3,1,2,3,vis,auto\n";

    static final String irisARFF = "% 1. Title: Iris Plants Database\n" +
            "% \n" +
            "% 2. Sources:\n" +
            "%      (a) Creator: R.A. Fisher\n" +
            "%      (b) Donor: Michael Marshall (MARSHALL%PLU@io.arc.nasa.gov)\n" +
            "%      (c) Date: July, 1988\n" +
            "% \n" +
            "% 3. Past Usage:\n" +
            "%    - Publications: too many to mention!!!  Here are a few.\n" +
            "%    1. Fisher,R.A. \"The use of multiple measurements in taxonomic problems\"\n" +
            "%       Annual Eugenics, 7, Part II, 179-188 (1936); also in \"Contributions\n" +
            "%       to Mathematical Statistics\" (John Wiley, NY, 1950).\n" +
            "%    2. Duda,R.O., & Hart,P.E. (1973) Pattern Classification and Scene Analysis.\n" +
            "%       (Q327.D83) John Wiley & Sons.  ISBN 0-471-22361-1.  See page 218.\n" +
            "%    3. Dasarathy, B.V. (1980) \"Nosing Around the Neighborhood: A New System\n" +
            "%       Structure and Classification Rule for Recognition in Partially Exposed\n" +
            "%       Environments\".  IEEE Transactions on Pattern Analysis and Machine\n" +
            "%       Intelligence, Vol. PAMI-2, No. 1, 67-71.\n" +
            "%       -- Results:\n" +
            "%          -- very low misclassification rates (0% for the setosa class)\n" +
            "%    4. Gates, G.W. (1972) \"The Reduced Nearest Neighbor Rule\".  IEEE \n" +
            "%       Transactions on Information Theory, May 1972, 431-433.\n" +
            "%       -- Results:\n" +
            "%          -- very low misclassification rates again\n" +
            "%    5. See also: 1988 MLC Proceedings, 54-64.  Cheeseman et al's AUTOCLASS II\n" +
            "%       conceptual clustering system finds 3 classes in the data.\n" +
            "% \n" +
            "% 4. Relevant Information:\n" +
            "%    --- This is perhaps the best known database to be found in the pattern\n" +
            "%        recognition literature.  Fisher's paper is a classic in the field\n" +
            "%        and is referenced frequently to this day.  (See Duda & Hart, for\n" +
            "%        example.)  The data setAt contains 3 classes of 50 instances each,\n" +
            "%        where each class refers to a type of iris plant.  One class is\n" +
            "%        linearly separable from the other 2; the latter are NOT linearly\n" +
            "%        separable from each other.\n" +
            "%    --- Predicted attribute: class of iris plant.\n" +
            "%    --- This is an exceedingly simple domain.\n" +
            "% \n" +
            "% 5. Number of Instances: 150 (50 in each of three classes)\n" +
            "% \n" +
            "% 6. Number of Attributes: 4 numeric, predictive attributes and the class\n" +
            "% \n" +
            "% 7. Attribute Information:\n" +
            "%    1. sepal length in cm\n" +
            "%    2. sepal width in cm\n" +
            "%    3. petal length in cm\n" +
            "%    4. petal width in cm\n" +
            "%    5. class: \n" +
            "%       -- Iris Setosa\n" +
            "%       -- Iris Versicolour\n" +
            "%       -- Iris Virginica\n" +
            "% \n" +
            "% 8. Missing Attribute Values: None\n" +
            "% \n" +
            "% Summary Statistics:\n" +
            "%  \t           Min  Max   Mean    SD   Class Correlation\n" +
            "%    sepal length: 4.3  7.9   5.84  0.83    0.7826   \n" +
            "%     sepal width: 2.0  4.4   3.05  0.43   -0.4194\n" +
            "%    petal length: 1.0  6.9   3.76  1.76    0.9490  (high!)\n" +
            "%     petal width: 0.1  2.5   1.20  0.76    0.9565  (high!)\n" +
            "% \n" +
            "% 9. Class Distribution: 33.3% for each of 3 classes.\n" +
            '\n' +
            "@RELATION iris\n" +
            '\n' +
            "@ATTRIBUTE \"(sepal,length)\"\tREAL\n" +
            "@ATTRIBUTE \"(sepal,width)\"\tREAL\n" +
            "@ATTRIBUTE \"(petal,length)\"\tREAL\n" +
            "@ATTRIBUTE \"(petal,width)\"\tREAL\n" +
            "@ATTRIBUTE class \t{Iris_setosa,Iris_versicolor,Iris_virginica}\n" +
            '\n' +
            "@DATA\n" +
            "5.1,3.5,1.4,0.2,Iris_setosa\n" +
            "4.9,3.0,1.4,0.2,Iris_setosa\n" +
            "4.7,3.2,1.3,0.2,Iris_setosa\n" +
            "4.6,3.1,1.5,0.2,Iris_setosa\n" +
            "5.0,3.6,1.4,0.2,Iris_setosa\n" +
            "5.4,3.9,1.7,0.4,Iris_setosa\n" +
            "4.6,3.4,1.4,0.3,Iris_setosa\n" +
            "5.0,3.4,1.5,0.2,Iris_setosa\n" +
            "4.4,2.9,1.4,0.2,Iris_setosa\n" +
            "4.9,3.1,1.5,0.1,Iris_setosa\n" +
            "5.4,3.7,1.5,0.2,Iris_setosa\n" +
            "4.8,3.4,1.6,0.2,Iris_setosa\n" +
            "4.8,3.0,1.4,0.1,Iris_setosa\n" +
            "4.3,3.0,1.1,0.1,Iris_setosa\n" +
            "5.8,4.0,1.2,0.2,Iris_setosa\n" +
            "5.7,4.4,1.5,0.4,Iris_setosa\n" +
            "5.4,3.9,1.3,0.4,Iris_setosa\n" +
            "5.1,3.5,1.4,0.3,Iris_setosa\n" +
            "5.7,3.8,1.7,0.3,Iris_setosa\n" +
            "5.1,3.8,1.5,0.3,Iris_setosa\n" +
            "5.4,3.4,1.7,0.2,Iris_setosa\n" +
            "5.1,3.7,1.5,0.4,Iris_setosa\n" +
            "4.6,3.6,1.0,0.2,Iris_setosa\n" +
            "5.1,3.3,1.7,0.5,Iris_setosa\n" +
            "4.8,3.4,1.9,0.2,Iris_setosa\n" +
            "5.0,3.0,1.6,0.2,Iris_setosa\n" +
            "5.0,3.4,1.6,0.4,Iris_setosa\n" +
            "5.2,3.5,1.5,0.2,Iris_setosa\n" +
            "5.2,3.4,1.4,0.2,Iris_setosa\n" +
            "4.7,3.2,1.6,0.2,Iris_setosa\n" +
            "4.8,3.1,1.6,0.2,Iris_setosa\n" +
            "5.4,3.4,1.5,0.4,Iris_setosa\n" +
            "5.2,4.1,1.5,0.1,Iris_setosa\n" +
            "5.5,4.2,1.4,0.2,Iris_setosa\n" +
            "4.9,3.1,1.5,0.1,Iris_setosa\n" +
            "5.0,3.2,1.2,0.2,Iris_setosa\n" +
            "5.5,3.5,1.3,0.2,Iris_setosa\n" +
            "4.9,3.1,1.5,0.1,Iris_setosa\n" +
            "4.4,3.0,1.3,0.2,Iris_setosa\n" +
            "5.1,3.4,1.5,0.2,Iris_setosa\n" +
            "5.0,3.5,1.3,0.3,Iris_setosa\n" +
            "4.5,2.3,1.3,0.3,Iris_setosa\n" +
            "4.4,3.2,1.3,0.2,Iris_setosa\n" +
            "5.0,3.5,1.6,0.6,Iris_setosa\n" +
            "5.1,3.8,1.9,0.4,Iris_setosa\n" +
            "4.8,3.0,1.4,0.3,Iris_setosa\n" +
            "5.1,3.8,1.6,0.2,Iris_setosa\n" +
            "4.6,3.2,1.4,0.2,Iris_setosa\n" +
            "5.3,3.7,1.5,0.2,Iris_setosa\n" +
            "5.0,3.3,1.4,0.2,Iris_setosa\n" +
            "7.0,3.2,4.7,1.4,Iris_versicolor\n" +
            "6.4,3.2,4.5,1.5,Iris_versicolor\n" +
            "6.9,3.1,4.9,1.5,Iris_versicolor\n" +
            "5.5,2.3,4.0,1.3,Iris_versicolor\n" +
            "6.5,2.8,4.6,1.5,Iris_versicolor\n" +
            "5.7,2.8,4.5,1.3,Iris_versicolor\n" +
            "6.3,3.3,4.7,1.6,Iris_versicolor\n" +
            "4.9,2.4,3.3,1.0,Iris_versicolor\n" +
            "6.6,2.9,4.6,1.3,Iris_versicolor\n" +
            "5.2,2.7,3.9,1.4,Iris_versicolor\n" +
            "5.0,2.0,3.5,1.0,Iris_versicolor\n" +
            "5.9,3.0,4.2,1.5,Iris_versicolor\n" +
            "6.0,2.2,4.0,1.0,Iris_versicolor\n" +
            "6.1,2.9,4.7,1.4,Iris_versicolor\n" +
            "5.6,2.9,3.6,1.3,Iris_versicolor\n" +
            "6.7,3.1,4.4,1.4,Iris_versicolor\n" +
            "5.6,3.0,4.5,1.5,Iris_versicolor\n" +
            "5.8,2.7,4.1,1.0,Iris_versicolor\n" +
            "6.2,2.2,4.5,1.5,Iris_versicolor\n" +
            "5.6,2.5,3.9,1.1,Iris_versicolor\n" +
            "5.9,3.2,4.8,1.8,Iris_versicolor\n" +
            "6.1,2.8,4.0,1.3,Iris_versicolor\n" +
            "6.3,2.5,4.9,1.5,Iris_versicolor\n" +
            "6.1,2.8,4.7,1.2,Iris_versicolor\n" +
            "6.4,2.9,4.3,1.3,Iris_versicolor\n" +
            "6.6,3.0,4.4,1.4,Iris_versicolor\n" +
            "6.8,2.8,4.8,1.4,Iris_versicolor\n" +
            "6.7,3.0,5.0,1.7,Iris_versicolor\n" +
            "6.0,2.9,4.5,1.5,Iris_versicolor\n" +
            "5.7,2.6,3.5,1.0,Iris_versicolor\n" +
            "5.5,2.4,3.8,1.1,Iris_versicolor\n" +
            "5.5,2.4,3.7,1.0,Iris_versicolor\n" +
            "5.8,2.7,3.9,1.2,Iris_versicolor\n" +
            "6.0,2.7,5.1,1.6,Iris_versicolor\n" +
            "5.4,3.0,4.5,1.5,Iris_versicolor\n" +
            "6.0,3.4,4.5,1.6,Iris_versicolor\n" +
            "6.7,3.1,4.7,1.5,Iris_versicolor\n" +
            "6.3,2.3,4.4,1.3,Iris_versicolor\n" +
            "5.6,3.0,4.1,1.3,Iris_versicolor\n" +
            "5.5,2.5,4.0,1.3,Iris_versicolor\n" +
            "5.5,2.6,4.4,1.2,Iris_versicolor\n" +
            "6.1,3.0,4.6,1.4,Iris_versicolor\n" +
            "5.8,2.6,4.0,1.2,Iris_versicolor\n" +
            "5.0,2.3,3.3,1.0,Iris_versicolor\n" +
            "5.6,2.7,4.2,1.3,Iris_versicolor\n" +
            "5.7,3.0,4.2,1.2,Iris_versicolor\n" +
            "5.7,2.9,4.2,1.3,Iris_versicolor\n" +
            "6.2,2.9,4.3,1.3,Iris_versicolor\n" +
            "5.1,2.5,3.0,1.1,Iris_versicolor\n" +
            "5.7,2.8,4.1,1.3,Iris_versicolor\n" +
            "6.3,3.3,6.0,2.5,Iris_virginica\n" +
            "5.8,2.7,5.1,1.9,Iris_virginica\n" +
            "7.1,3.0,5.9,2.1,Iris_virginica\n" +
            "6.3,2.9,5.6,1.8,Iris_virginica\n" +
            "6.5,3.0,5.8,2.2,Iris_virginica\n" +
            "7.6,3.0,6.6,2.1,Iris_virginica\n" +
            "4.9,2.5,4.5,1.7,Iris_virginica\n" +
            "7.3,2.9,6.3,1.8,Iris_virginica\n" +
            "6.7,2.5,5.8,1.8,Iris_virginica\n" +
            "7.2,3.6,6.1,2.5,Iris_virginica\n" +
            "6.5,3.2,5.1,2.0,Iris_virginica\n" +
            "6.4,2.7,5.3,1.9,Iris_virginica\n" +
            "6.8,3.0,5.5,2.1,Iris_virginica\n" +
            "5.7,2.5,5.0,2.0,Iris_virginica\n" +
            "5.8,2.8,5.1,2.4,Iris_virginica\n" +
            "6.4,3.2,5.3,2.3,Iris_virginica\n" +
            "6.5,3.0,5.5,1.8,Iris_virginica\n" +
            "7.7,3.8,6.7,2.2,Iris_virginica\n" +
            "7.7,2.6,6.9,2.3,Iris_virginica\n" +
            "6.0,2.2,5.0,1.5,Iris_virginica\n" +
            "6.9,3.2,5.7,2.3,Iris_virginica\n" +
            "5.6,2.8,4.9,2.0,Iris_virginica\n" +
            "7.7,2.8,6.7,2.0,Iris_virginica\n" +
            "6.3,2.7,4.9,1.8,Iris_virginica\n" +
            "6.7,3.3,5.7,2.1,Iris_virginica\n" +
            "7.2,3.2,6.0,1.8,Iris_virginica\n" +
            "6.2,2.8,4.8,1.8,Iris_virginica\n" +
            "6.1,3.0,4.9,1.8,Iris_virginica\n" +
            "6.4,2.8,5.6,2.1,Iris_virginica\n" +
            "7.2,3.0,5.8,1.6,Iris_virginica\n" +
            "7.4,2.8,6.1,1.9,Iris_virginica\n" +
            "7.9,3.8,6.4,2.0,Iris_virginica\n" +
            "6.4,2.8,5.6,2.2,Iris_virginica\n" +
            "6.3,2.8,5.1,1.5,Iris_virginica\n" +
            "6.1,2.6,5.6,1.4,Iris_virginica\n" +
            "7.7,3.0,6.1,2.3,Iris_virginica\n" +
            "6.3,3.4,5.6,2.4,Iris_virginica\n" +
            "6.4,3.1,5.5,1.8,Iris_virginica\n" +
            "6.0,3.0,4.8,1.8,Iris_virginica\n" +
            "6.9,3.1,5.4,2.1,Iris_virginica\n" +
            "6.7,3.1,5.6,2.4,Iris_virginica\n" +
            "6.9,3.1,5.1,2.3,Iris_virginica\n" +
            "5.8,2.7,5.1,1.9,Iris_virginica\n" +
            "6.8,3.2,5.9,2.3,Iris_virginica\n" +
            "6.7,3.3,5.7,2.5,Iris_virginica\n" +
            "6.7,3.0,5.2,2.3,Iris_virginica\n" +
            "6.3,2.5,5.0,1.9,Iris_virginica\n" +
            "6.5,3.0,5.2,2.0,Iris_virginica\n" +
            "6.2,3.4,5.4,2.3,Iris_virginica\n" +
            "5.9,3.0,5.1,1.8,Iris_virginica\n";
}
