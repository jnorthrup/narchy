package nars.time.part;

import nars.$;
import nars.NAR;

import java.util.function.Consumer;

public final class DurNARConsumer extends DurLoop {

	final Consumer<NAR> r;

	public DurNARConsumer(Consumer<NAR> r) {
		super($.INSTANCE.identity(r));
		this.r = r;
	}

	@Override
	protected void run(NAR n, long dt) {
		r.accept(n);
	}



}
