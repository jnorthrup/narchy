package spacegraph.space2d.widget.chip;

import com.googlecode.lanterna.input.KeyType;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.TextPort;

import java.util.function.Consumer;

public class ReplChip extends Gridding {

    private final ReplModel model;
    private final TextPort in;
    private final TextPort out;

    @FunctionalInterface
    public interface ReplModel {
        void input(String cmd, Consumer<String> receive);
    }

    public ReplChip(ReplModel m) {
        this.model = m;
        set(
                in = new TextPort(), //TODO a Port
                out = new TextPort() //TODO a Port
        );

        out.edit.resize(40, 8);

        //in.on(z -> {
           //TODO
        //});

        in.edit.onKey(x -> {
            if (x.getKeyType() == KeyType.Enter && (!enterOrControlEnter() || x.isCtrlDown())) {
                String cmd = in.edit.text();

                if (clearOnEnter())
                    in.edit.text("");

                model.input(cmd, (e) -> {
                    if (appendOrReplace()) {
                        out.edit.append(e);
                        out.out(out.edit.text());
                    } else {
                        out.edit.text(e);
                    }
                });
            }
        });
    }

    public boolean appendOrReplace() { //append mode will require a clear button
        return false;
    }

    protected boolean clearOnEnter() {
        return true;
    }

    protected boolean enterOrControlEnter() {
        return true;
    }
}
