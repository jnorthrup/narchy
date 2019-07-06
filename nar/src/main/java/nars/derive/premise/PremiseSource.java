package nars.derive.premise;

import nars.Task;
import nars.derive.model.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;
import nars.time.When;

import java.util.function.Consumer;

abstract public class PremiseSource {

    public abstract void premises(When when, int premisesPerIteration, int termlinksPerTaskLink, TaskLinks links, Derivation d, Consumer<Premise> p);

    /**
     * samples premises
     */
    @Deprecated public final void derive(When when, int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, TaskLinks links, Derivation d) {
        premises(when, premisesPerIteration, termlinksPerTaskLink, links, d, premise-> premise.derive(d, matchTTL, deriveTTL));
    }

    public void commit() {
        /* default: nothing */
    }


    /** unbuffered */
    public static class DefaultPremiseSource extends PremiseSource {

        @Override public void premises(When when, int premisesPerIteration, int termlinksPerTaskLink, TaskLinks links, Derivation d, Consumer<Premise> p) {
            d.what.sample(d.random, (int) Math.max(1, Math.ceil(((float)premisesPerIteration) / termlinksPerTaskLink)), tasklink -> {
                Task task = tasklink.get(when);
                if (task != null && !task.isDeleted()) {
                    hypothesize(tasklink, task, termlinksPerTaskLink, links, d, p);
                }
            });

        }

    }

    protected void hypothesize(TaskLink tasklink, Task task, int termlinksPerTaskLink, TaskLinks links, Derivation d, Consumer<Premise> each) {


//        Task task2 = Abbreviation.unabbreviate(task, d);
//        if (task!=task2 && task2!=null) {
//            if (task2.term().volume() <= ((float)(d.termVolMax/2))) {
//                //System.out.println(task + " " + task2);
//                task = task2; //use decompressed form if small enough
//            } else {
//                //remain compressed
//                //System.out.println(task + " " + task2);
//            }
//        }


        for (int i = 0; i < termlinksPerTaskLink; i++) {
            Term term = links.term(tasklink, task, d);
            if (term != null)
                each.accept(new Premise(task, term));
        }
    }

}
