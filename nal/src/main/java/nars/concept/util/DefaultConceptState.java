package nars.concept.util;

import jcog.Util;
import jcog.math.MutableInteger;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.concept.action.ActionConcept;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;

import static jcog.Util.clamp;

/**
 * Created by me on 5/11/16.
 */
public final class DefaultConceptState extends ConceptState {

    public int beliefsMaxEte;
    public int goalsMaxEte;
    private final int beliefsMinEte;
    private final int goalsMinEte;
    private final MutableInteger questionsMax;

    //public final MutableInteger termLinksCapacityMax, termLinksCapacityMin, taskLinksCapacityMax, taskLinksCapacityMin;
    private final IntToIntFunction termlinksCapacity, tasklinksCapacity;

    public int beliefsMaxTemp;
    public int beliefsMinTemp;
    public int goalsMaxTemp;
    public int goalsMinTemp;

    /**
     * minimum of 3 beliefs per belief table. for eternal, this allows revision between two goals to produce a third
     */
    public DefaultConceptState(String id, int beliefsCapTotal, int goalsCapTotal, int questionsMax) {
        this(id,
                new MutableInteger(clamp(beliefsCapTotal / 4, 1, 6)), //belief ete ~1/4
                new MutableInteger(clamp(goalsCapTotal / 4, 1, 6)),   //goal ete  ~1/4
                new MutableInteger(Math.max(3, beliefsCapTotal)), //belief temp
                new MutableInteger(Math.max(3, goalsCapTotal)), //goal temp
                new MutableInteger(questionsMax),
                (vol) -> { //vol to termlinks

                    //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiI2NC8oMl4oeC80KSkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsIndpbmRvdyI6WyItNzQuMzIzMjM3MjA4NTc0ODciLCI3Ny4wMTY2ODExNjY0NDAwOCIsIi0yMi4zNjE2Njk2NTc0OTEwOCIsIjcwLjc3MDU4NzgwNDA1NjU4Il19XQ--
                    //HACK

                    int maxLinks = 48;
                    return Math.round((maxLinks) / (1f+((float)Math.sqrt(vol))/2f));

                    //int l = Math.max(minLinks, (int) Math.round(maxLinks / (Math.pow(2, (vol - 1) / (Math.sqrt(maxVol))))));
                    //System.out.println(l + " <- " + vol);
                    //return l;
                },
                (vol) -> { //vol to tasklinks

                    //HACK
                    int maxLinks = 48;
                    return Math.round((maxLinks) / (1f+((float)Math.sqrt(vol))/2f));


//                    int maxVol = 32;
//                    int maxLinks = 32;
//                    int minLinks = 16;
//                    int l = Math.max(minLinks, (int) Math.round(maxLinks / (Math.pow(2, (vol - 1) / (Math.sqrt(maxVol))))));
//                    return l;
                }
        );
    }

    DefaultConceptState(String id, MutableInteger beliefsMaxEte, MutableInteger goalsMaxEte,
                        MutableInteger beliefsMaxTemp, MutableInteger goalsMaxTemp,
                        MutableInteger questionsMax, IntToIntFunction termlinksCapacity, IntToIntFunction taskLinksCapacity) {
        super("___" + id);
        this.beliefsMaxEte = beliefsMaxEte.intValue();
        this.beliefsMinEte = 2;
        this.beliefsMaxTemp = beliefsMaxTemp.intValue();
        this.beliefsMinTemp = 2;
        this.goalsMaxEte = goalsMaxEte.intValue();
        this.goalsMinEte = 2;
        this.goalsMaxTemp = goalsMaxTemp.intValue();
        this.goalsMinTemp = 2;
        this.questionsMax = questionsMax;

        this.termlinksCapacity = termlinksCapacity;
        this.tasklinksCapacity = taskLinksCapacity;
    }


    @Override
    public int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        int max, min;

        if (beliefOrGoal) {
            max = eternalOrTemporal ? beliefsMaxEte : beliefsMaxTemp;
            min = eternalOrTemporal ? beliefsMinEte : beliefsMinTemp;
        } else {
            max = eternalOrTemporal ? goalsMaxEte : goalsMaxTemp;
            min = eternalOrTemporal ? goalsMinEte : goalsMinTemp;
        }

        int c = Util.lerp(Util.unitize((-1 + concept.complexity()) / 32f), max, min);

        if (concept instanceof PermanentConcept) {
            if (!eternalOrTemporal) {
                c *= 2;//double temporal for PermanentConcept's
            } else {
                if (concept instanceof ActionConcept) {
                    c = 0; //disable eternal TODO this might be too extreme
                }
            }
        }

        return c;
        //return (int) Math.ceil(max * Math.min(1f, (1f / (compoundConcept.volume()/ beliefComplexityCapacity))));
    }

    @Override
    public int linkCap(Concept concept, boolean termOrTask) {
        if (termOrTask) {

            return termlinksCapacity.valueOf(concept.volume());

        } else {
            return tasklinksCapacity.valueOf(concept.volume());
        }
    }

    public static int lerp(Concept c, MutableInteger _min, MutableInteger _max) {

        int min = _min.intValue();
        int max = _max.intValue();

        float v = c.complexity();
        float complexityFactor = ((v - 1) / 32); //(nar.compoundVolumeMax.intValue()/2f); //HEURISTIC
        complexityFactor = Util.sqr(Util.unitize(complexityFactor)); //clip at +1

        return Util.lerp(complexityFactor, max, min); //at least enough for its templates
    }

    @Override
    public final int questionCap(TaskConcept concept, boolean questionOrQuest) {
        return Util.lerp( 1f - Math.min(1f,(((float)concept.volume()) / 32)), 1, questionsMax.intValue());
    }

//        public int getBeliefsCapacity(Termed t);
//        public int getGoalsCapabity(Termed t);
//        public int getTermLinksCapacity(Termed t);
//        public int getTaskLinksCapacity(Termed t);
}
