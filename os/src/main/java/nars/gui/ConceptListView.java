package nars.gui;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.attention.What;
import nars.term.Term;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

public class ConceptListView extends AbstractTaskView<Term> {

	public ConceptListView(What w,int capacity) {
		super(w, PriMerge.plus, capacity);
	}

	@Override
	protected Term transform(Task x) {
		return x.term().concept();
	}

	@Override
	public Surface apply(int x, int y, PriReference<Term> value) {
		return new PushButton(new VectorLabel(value.get().toString()));
	}
}
