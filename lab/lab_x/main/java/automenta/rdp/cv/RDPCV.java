package automenta.rdp.cv;

import automenta.rdp.RdesktopCanvas;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class RDPCV  {
    private final RdesktopCanvas canvas;
    private Image backgroundImage;

    ImageView baseView;
    private WritableImage baseImage = null;

    public RDPCV(RdesktopCanvas canvas) {
        this.canvas = canvas;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("CV");
                final JFXPanel fxPanel = new JFXPanel();
                frame.add(fxPanel);
                frame.setSize(300, 200);
                frame.setVisible(true);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        init(fxPanel);

                        new AnimationTimer() {

                            @Override
                            public void handle(long now) {
                                baseView.setImage(getBase());

                                for (RDPVis v : canvas.vis) {
                                    v.update();
                                }
                            }

                        }.start();
                    }
                });
            }
        });
    }

    private void init(JFXPanel fxPanel) {



        baseView = new ImageView(
                getBase()
        );

        baseView.setOpacity(0.75f);

        Scene scene = createScene();
        fxPanel.setScene(scene);


    }

    public Image getBase() {
        baseImage = SwingFXUtils.toFXImage(canvas.backstore.getImage(), baseImage);

        return baseImage;
    }

    private Scene createScene() {

        
        StackPane layout = new StackPane();
        layout.getChildren().setAll(
                baseView
                
        );

        

        for (RDPVis v : canvas.vis) {
            Node n = v.getNode();
            if (n!=null)
                layout.getChildren().add(n);
        }

        
        


        
        
        

        
        
        

        
        Scene scene = new Scene(layout); 
        return scene;
    }



    /**
     * @return a ScrollPane which scrolls the layout.
     */
    private ScrollPane createScrollPane(Pane layout) {
        ScrollPane scroll = new ScrollPane();
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setPrefSize(800, 600);
        scroll.setContent(layout);
        return scroll;
    }


}