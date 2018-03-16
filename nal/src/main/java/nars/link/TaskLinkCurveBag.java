package nars.link;

import jcog.bag.impl.CurveBag;
import nars.Param;

import java.util.Map;

public class TaskLinkCurveBag extends CurveBag<TaskLink> {
    public TaskLinkCurveBag(Map sharedMap) {
        super(Param.tasklinkMerge, sharedMap, 0);
    }


    @Override
    public void onRemove(TaskLink _value) {
        _value.reincarnate(this);
    }
}
