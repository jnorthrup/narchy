package spacegraph.widget.button;

import org.jetbrains.annotations.Nullable;
import spacegraph.layout.AspectAlign;
import spacegraph.widget.text.Label;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends AbstractButton {

    private final Label label;


    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
        this("");
    }

    public PushButton(String s) {
        super();
        content((label = new Label(s)).scale(0.8f).align(AspectAlign.Align.Center));
    }

    public PushButton(Consumer<PushButton> onClick) {
        this();
        click(onClick);
    }

    public PushButton(String s, Runnable onClick) {
        this(s, (p) -> onClick.run());
    }

    public PushButton(String s, @Nullable Consumer<PushButton> onClick) {
        this(s);
        click(onClick);
    }

    public PushButton click(@Nullable Runnable onClick) {
        return click((cb)->onClick.run());
    }

    public PushButton click(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
        return this;
    }

    public void setLabel(String s) {
        this.label.text(s);
    }


    @Override
    protected void onClick() {
        if (onClick!=null)
            onClick.accept(this);
    }

}
