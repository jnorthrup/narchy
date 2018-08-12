package spacegraph.space2d.widget.button;

import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.ImageTexture;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends AbstractButton {




    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
        this("");
    }

    public PushButton(String s) {

        this(
            //new Label(s)
                new BitmapLabel(s)
        );
    }

    public PushButton(Surface content) {
        content(content);
    }

    public PushButton(Consumer<PushButton> onClick) {
        super();
        click(onClick);
    }

    public PushButton(String s, Runnable onClick) {
        this(s, (p) -> onClick.run());
    }

    public PushButton(String s, @Nullable Consumer<PushButton> onClick) {
        this(s);
        click(onClick);
    }

    public PushButton(ImageTexture tex) {
        this(tex.view());
    }

    public static PushButton awesome(String icon) {
        return new PushButton(ImageTexture.awesome(icon).view(1));
    }

    public PushButton click(@Nullable Runnable onClick) {
        return click((cb)->onClick.run());
    }

    public PushButton click(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
        return this;
    }

    public PushButton icon(String s) {
        content(new ImageTexture(s).view());
        return this;
    }
    public PushButton label(String s) {
        content(
            new VectorLabel(s)
        );
        return this;
    }

    @Override
    protected void onClick(Finger f) {
        if (onClick!=null)
            onClick.accept(this);
    }

}
