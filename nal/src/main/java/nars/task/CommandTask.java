package nars.task;

import jcog.util.ArrayUtil;
import nars.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.Op.COMMAND;

public interface CommandTask extends Task {

	@Override
	default boolean isInput() {
		return true;
	}

	@Override
	default short[] why() {
		return ArrayUtil.EMPTY_SHORT_ARRAY;
	}

	@Override
	default boolean isCyclic() {
		return false;
	}

	@Override
	default void setCyclic(boolean b) {

	}

	@Override
	default long start() {
		return ETERNAL; //TODO maybe TIMELESS
	}

	@Override
	default long end() {
		return ETERNAL; //TODO maybe TIMELESS
	}

	@Override
	default long[] stamp() {
		return ArrayUtil.EMPTY_LONG_ARRAY;
	}

	@Override
	@Nullable
	default Truth truth() {
		return null;
	}


	@Override
	default byte punc() {
		return COMMAND;
	}

	@Override
	default float freq(long start, long end) {
		return Float.NaN;
	}

	@Override
	default boolean delete() {
		return false;
	}

	@Override
	default CommandTask pri(float ignored) {
		return this;
	}

	@Override
	default float pri() {
		return 0;
	}

	@Override
	default long creation() {
		return ETERNAL;
	}

	@Override
	default void setCreation(long creation) {
		//ignored
	}


}
