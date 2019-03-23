package spacegraph.space2d.widget.button;

import jcog.exe.Exe;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.ImageTexture;

import java.util.function.Consumer;

public class PushButton extends AbstractButton {

    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
        this(new EmptySurface());
    }

    public PushButton(String s) {

        this(
            new VectorLabel(s)
            //    new BitmapLabel(s)
        );
    }

    public PushButton(Surface content) {
        super(content);
    }

    public PushButton(Consumer<PushButton> onClick) {
        this();
        clicking(onClick);
    }

    public PushButton(String s, Runnable onClick) {
        //this(s, (p) -> onClick.run());
        this(s);
        clicking(onClick);
    }

//    public PushButton(String s, @Nullable Consumer<PushButton> onClick) {
//        this(s);
//        click(onClick);
//    }

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

    public PushButton clicking(@Nullable Runnable onClick) {
        return clicking((cb)->onClick.run());
    }

    public PushButton clicking(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
        return this;
    }

    public PushButton icon(String s) {
        set(new ImageTexture(s).view());
        return this;
    }
    public PushButton label(String s) {
        set(
            new VectorLabel(s)
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
