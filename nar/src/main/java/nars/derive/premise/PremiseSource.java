package nars.derive.premise;

import nars.Task;
import nars.derive.model.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;
import nars.time.When;

import java.util.function.Consumer;

abstract public class PremiseSource {
    /**
     * samples premises
     */
    public abstract void derive(When when, int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, TaskLinks links, Derivation d);

    public void commit() {
        /* default: nothing */
    }


    /** unbuffered */
    public static class DefaultPremiseSource extends PremiseSource {

        @Override
        public void derive(When when, int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, TaskLinks links, Derivation d) {
            d.what.sample(d.random, (int) Math.max(1, Math.ceil(((float)premisesPerIteration) / termlinksPerTaskLink)), tasklink -> {
                Task task = tasklink.get(when);
                if (task != null && !task.isDeleted()) {
                    hypothesize(tasklink, task, termlinksPerTaskLink, links, d, (premise) -> {
                        premise.derive(d, matchTTL, deriveTTL);
                    });
                }
            });
        }
    }

    protected void hypothesize(TaskLink tasklink, Task task, int termlinksPerTaskLink, TaskLinks links, Derivation d, Consumer<Premise> each) {
        Term prevTerm = null;

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
            if (term != null) {
                if (prevTerm == null || !term.equals(prevTerm)) {

//                term = Abbreviation.unabbreviate(term, d.nar);
//                if (term == null || term instanceof Bool)
//                    continue;

                    each.accept(new Premise(task, term));
                }
                prevTerm = term;
            }
        }
    }

}
