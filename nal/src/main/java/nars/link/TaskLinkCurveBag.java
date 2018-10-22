package nars.link;

import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import nars.task.Tasklike;

import java.util.Map;

public class TaskLinkCurveBag extends PLinkArrayBag<Tasklike> {


    public TaskLinkCurveBag(PriMerge merge, Map map, int cap) {
        super(cap, merge, map);
    }


}
