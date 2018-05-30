package nars.concept.dynamic;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.link.TaskLink;
import nars.link.Tasklinks;
import nars.table.TemporalBeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


/** computes dynamic truth according to implicit truth functions
 *  determined by recursive evaluation of the compound's sub-component's truths
 */
public class DynamicTruthBeliefTable extends DynamicBeliefTable {

    private final DynamicTruthModel model;


    public DynamicTruthBeliefTable(Term c, TemporalBeliefTable t, DynamicTruthModel model, boolean beliefOrGoal) {
        super(c, beliefOrGoal, t);
        this.model = model;
    }

    @Override
    public boolean isEmpty() {
        /** since this is a dynamic evaluation, we have to assume it is not empty */
        return false;
    }



    @Override
    protected Task taskDynamic(long start, long end, Term template, NAR nar) {




        if (template == null)
            template = term;

        DynTruth yy = model.eval(template, beliefOrGoal, start,end, true /* dont force projection */, nar);
        if (yy != null) {
            Task generated = yy.task(template, model, beliefOrGoal, nar);


            if (generated!=null && Param.TASKLINK_DYN_GENERATED_TASKS) {


                Concept c = generated.concept(nar, true);
                if (c != null) {
                    float pri = generated.priElseZero();
                    TaskLink.Tasklike seed = TaskLink.GeneralTaskLink.seed(generated, false, nar);
                    Tasklinks.linkTask(new TaskLink.GeneralTaskLink(seed, pri), pri, c.templates().concepts(nar, true), nar.random());
                }
            }





            return generated;
        } else {
            return null;
        }
    }

    @Override
    public void sampleDynamic(long start, long end, Consumer<Task> n, NAR nar) {
        
        Task x = taskDynamic(start, end, term, nar);
        if (x!=null)
            n.accept(x);
    }

    @Override
    protected @Nullable Truth truthDynamic(long start, long end, Term template, NAR nar) {

        if (template == null)
            template = term;

        DynTruth d = model.eval(template, beliefOrGoal, start, end,
                false /* force projection to the specific time */, nar);
        if (d!=null)
            return d.truth(template, model, beliefOrGoal, nar);
        else
            return null;
    }
























































































































}














































