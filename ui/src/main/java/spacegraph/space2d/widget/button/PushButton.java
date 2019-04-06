package spacegraph.space2d.widget.button;

import jcog.exe.Exe;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.ImageTexture;

import java.util.function.Consumer;

public class PushButton extends AbstractButton {

    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
        super();
    }

    public PushButton(String s) {
        this();
        label(s);
    }

    public PushButton(Surface content) {
        super(content);
    }

    public PushButton(Consumer<PushButton> clicked) {
        this();
        clicked(clicked);
    }

    public PushButton(String s, Runnable clicked) {
        //this(s, (p) -> onClick.run());
        this(s);
        clicked(clicked);
    }


    public PushButton(ImageTexture tex) {
        this(tex.view());
    }

    public static PushButton awesome(String icon) {
        return new PushButton(ImageTexture.awesome(icon).view(1));
    }
    public static PushButton awesome(String icon, String label) {
        return new PushButton(Splitting.column(
                new VectorLabel(label), 0.1f,
                ImageTexture.awesome(icon).view(1)));
    }

    public PushButton clicked(@Nullable Runnable onClick) {
        return clicked((cb)->onClick.run());
    }

    public PushButton clicked(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
        return this;
    }

    public PushButton icon(String s) {
        set(new ImageTexture(s).view());
        return this;
    }

    public PushButton label(String s) {
        set(

            new BitmapLabel(s)
            //s.length() < 32 ? new BitmapLabel(s) : new VectorLabel(s)
            //new VectorLabel(s)
        );
        return this;
    }

    @Override
    protected void onClick() {
        Consumer<PushButton> c = this.onClick;
        if (c !=null) {
            Exe.invoke(
                ()->c.accept(this)
            );
        }
    }

}
