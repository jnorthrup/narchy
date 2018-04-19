package nars.experiment.racer;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import spacegraph.SpaceGraph;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.FixtureDef;
import spacegraph.space2d.widget.windo.Dyn2DSurface;

import static nars.experiment.racer.Vehicle2D.Wheel.PIXEL_TO_METER_RATIO_DEFAULT;

/**
 * A AndEngine demo based on the RacerGameActivity, and the Box2D tutorial from:
 * http://www.iforce2d.net/b2dtut/rotate-to-angle
 *
 * @author Florian Knoll (myfknoll(at)gmail.com)
 *
 */
public class Racer {

    // ===========================================================
    // Constants
    // ===========================================================

    private static final int RACETRACK_WIDTH = 128;

    private static final int OBSTACLE_SIZE = 16;

    private static final int CAMERA_WIDTH = RACETRACK_WIDTH * 5;
    private static final int CAMERA_HEIGHT = RACETRACK_WIDTH * 3;

	public static final float DEGTORAD = 0.0174532925199432957f;
	public static final float RADTODEG = 57.295779513082320876f;

	public static final int ACCELERATE = 1;
	public static final int ACCELERATE_NONE = 0;
	public static final int BREAK = -1;

	public static final int STEER_RIGHT = 1;
	public static final int STEER_NONE = 0;
	public static final int STEER_LEFT = -1;

    float m_currentTraction = 1;

    float m_maxForwardSpeed;
    float m_maxBackwardSpeed;
    float m_maxDriveForce;
    float m_maxLateralImpulse;

	// ===========================================================
    // Fields
    // ===========================================================

//    private Camera mCamera;
//
//    private BitmapTextureAtlas mBoxTexture;
//    private ITextureRegion mBoxTextureRegion;
//
//    private BitmapTextureAtlas mRacetrackTexture;
//    private ITextureRegion mRacetrackStraightTextureRegion;
//    private ITextureRegion mRacetrackCurveTextureRegion;
//
//    private BitmapTextureAtlas mOnScreenControlTexture;
//    private ITextureRegion mOnScreenControlBaseTextureRegion;
//    private ITextureRegion mOnScreenControlKnobTextureRegion;
//
//    private Scene mScene;

    private Dynamics2D mPhysicsWorld;

    private IVehicleControl control;
    private Vehicle2D vehicle;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================


    public static void main(String[] args) {
        new Racer();
    }
    public Racer() {
        //        this.mEngine.registerUpdateHandler(new FPSLogger());
//
//        this.mScene = new Scene();
//        this.mScene.setBackground(new Background(0, 0, 0));

        Dyn2DSurface p = SpaceGraph.wall(800, 800);

        this.mPhysicsWorld =
                p.W;
                //new Dynamics2D(new v2(0, 0));

//        this.initRacetrack();
        //this.initRacetrackBorders();
        this.initCar();
        //vehicle.pedalAccelerate();
        vehicle.steerLeft();
        p.root().onUpdate(j->{
            vehicle.onUpdate(j.dtMS()/1000f);
            ///vehicle.updateDrive();
        });
        ((Ortho)p.root()).window.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'a':
                        vehicle.steerLeft();
                        break;
                    case 'd':
                        vehicle.steerRight();
                        break;
                    case 's':
                        //vehicle.steerNone();
                        vehicle.pedalAccelerate();
                        break;
                    case 'x':
                        vehicle.pedalBreak();
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });


        //this.initObstacles();
//        this.initOnScreenControls();

        //this.mScene.registerUpdateHandler(this.mPhysicsWorld);

        //return this.mScene;


    }
//    @Override
//    public EngineOptions onCreateEngineOptions() {
//        this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
//
//        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
//    }

//    @Override
//    public void onCreateResources() {
//        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
//
//        this.mRacetrackTexture = new BitmapTextureAtlas(this.getTextureManager(), 128, 256, TextureOptions.REPEATING_NEAREST);
//        this.mRacetrackStraightTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_straight.png", 0, 0);
//        this.mRacetrackCurveTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_curve.png", 0, 128);
//        this.mRacetrackTexture.load();
//
//        this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
//        this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
//        this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);
//        this.mOnScreenControlTexture.load();
//
//        this.mBoxTexture = new BitmapTextureAtlas(this.getTextureManager(), 32, 32, TextureOptions.BILINEAR);
//        this.mBoxTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBoxTexture, this, "box.png", 0, 0);
//        this.mBoxTexture.load();
//    }

 // ===========================================================
    // Methods
    // ===========================================================

//    private void initOnScreenControls() {
//        final AnalogOnScreenControl analogOnScreenControl = new AnalogOnScreenControl(0, CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight(), this.mCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, this.getVertexBufferObjectManager(), new IAnalogOnScreenControlListener() {
//            @Override
//            public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
//                TopDownRaceGameActivity.this.controlChange(pBaseOnScreenControl, pValueX, pValueY);
//            }
//
//            @Override
//            public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
//                /* Nothing. */
//            }
//        });
//        analogOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        analogOnScreenControl.getControlBase().setAlpha(0.5f);
//        analogOnScreenControl.refreshControlKnobPosition();
//
//        this.mScene.setChildScene(analogOnScreenControl);
//    }

    protected void initCar() {
	    float maxForwardSpeed = 250;
        float maxBackwardSpeed = -40;
        float frontTireMaxDriveForce = 20;
        float frontTireMaxLateralImpulse = 0.0f;

        vehicle = new Vehicle2D(mPhysicsWorld);
        vehicle.setCharacteristics(maxForwardSpeed/PIXEL_TO_METER_RATIO_DEFAULT, maxBackwardSpeed/PIXEL_TO_METER_RATIO_DEFAULT, frontTireMaxDriveForce, frontTireMaxLateralImpulse);
        control = vehicle;
    }

    private void initObstacles() {
        this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
    }

    private void addObstacle(final float pX, final float pY) {
//        final Sprite box = new Sprite(pX, pY, OBSTACLE_SIZE, OBSTACLE_SIZE, this.mBoxTextureRegion, this.getVertexBufferObjectManager());

        final FixtureDef boxFixtureDef = new FixtureDef(PolygonShape.box(pX, pY), 0.1f, 0.5f);
        boxFixtureDef.restitution = 0.5f;
        final Body2D boxBody = mPhysicsWorld.addDynamic(boxFixtureDef); //PhysicsFactory.createBoxBody(this.mPhysicsWorld, box, BodyType.DynamicBody, boxFixtureDef);
        boxBody.setLinearDamping(10);
        boxBody.setAngularDamping(10);

//        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(box, boxBody, true, true));

//        this.mScene.attachChild(box);
    }
//
//   private void initRacetrack() {
//        /* Straights. */
//        {
//            final ITextureRegion racetrackHorizontalStraightTextureRegion = this.mRacetrackStraightTextureRegion.deepCopy();
//            racetrackHorizontalStraightTextureRegion.setTextureWidth(3 * this.mRacetrackStraightTextureRegion.getWidth());
//
//            final ITextureRegion racetrackVerticalStraightTextureRegion = this.mRacetrackStraightTextureRegion;
//
////            /* Top Straight */
////            this.mScene.attachChild(new Sprite(RACETRACK_WIDTH, 0, 3 * RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackHorizontalStraightTextureRegion, this.getVertexBufferObjectManager()));
////            /* Bottom Straight */
////            this.mScene.attachChild(new Sprite(RACETRACK_WIDTH, CAMERA_HEIGHT - RACETRACK_WIDTH, 3 * RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackHorizontalStraightTextureRegion, this.getVertexBufferObjectManager()));
//
//            /* Left Straight */
//            final Sprite leftVerticalStraight = new Sprite(0, RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackVerticalStraightTextureRegion, this.getVertexBufferObjectManager());
//            leftVerticalStraight.setRotation(90);
//            this.mScene.attachChild(leftVerticalStraight);
//            /* Right Straight */
//            final Sprite rightVerticalStraight = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackVerticalStraightTextureRegion, this.getVertexBufferObjectManager());
//            rightVerticalStraight.setRotation(90);
//            this.mScene.attachChild(rightVerticalStraight);
//        }
//
//        /* Edges */
//        {
//            final ITextureRegion racetrackCurveTextureRegion = this.mRacetrackCurveTextureRegion;
//
//            /* Upper Left */
//            final Sprite upperLeftCurve = new Sprite(0, 0, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
//            upperLeftCurve.setRotation(90);
//            this.mScene.attachChild(upperLeftCurve);
//
//            /* Upper Right */
//            final Sprite upperRightCurve = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, 0, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
//            upperRightCurve.setRotation(180);
//            this.mScene.attachChild(upperRightCurve);
//
//            /* Lower Right */
//            final Sprite lowerRightCurve = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, CAMERA_HEIGHT - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
//            lowerRightCurve.setRotation(270);
//            this.mScene.attachChild(lowerRightCurve);
//
//            /* Lower Left */
//            final Sprite lowerLeftCurve = new Sprite(0, CAMERA_HEIGHT - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
//            this.mScene.attachChild(lowerLeftCurve);
//        }
//    }

	protected void controlChange(final float pValueX, final float pValueY) {
		//System.out.println("X:"+pValueX+" Y:"+pValueY);

        if (pValueY < -0.1f) {
            control.pedalAccelerate();
        } else if (pValueY > 0.1f) {
            control.pedalBreak();
        } else {
            control.pedalNone();
        }

        if (pValueX > 0.1f) {
            control.steerRight();
        } else if (pValueX < -0.1f) {
            control.steerLeft();
        } else {
            control.steerNone();
        }
    }

	private void initRacetrackBorders() {
        //final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();

        final PolygonShape bottomOuter = PolygonShape.box(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
        final PolygonShape topOuter = PolygonShape.box(0, 0, CAMERA_WIDTH, 2);
        final PolygonShape leftOuter = PolygonShape.box(0, 0, 2, CAMERA_HEIGHT);
        final PolygonShape rightOuter = PolygonShape.box(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);

        final PolygonShape bottomInner = PolygonShape.box(RACETRACK_WIDTH, CAMERA_HEIGHT - 2 - RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2);
        final PolygonShape topInner = PolygonShape.box(RACETRACK_WIDTH, RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2);
        final PolygonShape leftInner = PolygonShape.box(RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH);
        final PolygonShape rightInner = PolygonShape.box(CAMERA_WIDTH - 2 - RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH);

//        final FixtureDef wallFixtureDef = new FixtureDef(bottomOuter, 0, 0.5f);
////        wallFixtureDef.restitution = 0.5f;
        this.mPhysicsWorld.addStatic(new FixtureDef(bottomOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(leftOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(rightOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(topOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(bottomInner, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(leftInner, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(rightInner, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(topInner, 0, 0.5f));

    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    /**
     * Interface for car controlling
     * @author Knoll
     *
     */
    public static interface IVehicleControl {

        void steerLeft();

        void steerRight();

        void steerNone();

        void pedalAccelerate();

        void pedalBreak();

        void pedalNone();

    }
}
//package nars.experiment.racer;
//
//import com.badlogic.gdx.physics.box2d.Body;
//import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
//import com.badlogic.gdx.physics.box2d.FixtureDef;
//import marytts.util.math.MathUtils;
//import org.andengine.entity.primitive.Rectangle;
//import org.andengine.entity.sprite.Sprite;
//import org.andengine.entity.sprite.TiledSprite;
//import org.andengine.extension.physics.box2d.PhysicsConnector;
//import org.andengine.extension.physics.box2d.PhysicsFactory;
//import org.andengine.opengl.texture.region.ITextureRegion;
//import org.andengine.opengl.vbo.VertexBufferObjectManager;
//import org.apache.commons.math3.util.ArithmeticUtils;
//import spacegraph.space2d.phys.dynamics.Body2D;
//import spacegraph.space2d.phys.dynamics.Dynamics2D;
//import spacegraph.math.v2;
//
///**
// * (c) 2010 Nicolas Gramlich
// * (c) 2011 Zynga
// *
// * @author Nicolas Gramlich
// * @since 22:43:20 - 15.07.2010
// */
//public class Racer {
//	// ===========================================================
//	// Constants
//	// ===========================================================
//
//	private static final int RACETRACK_WIDTH = 64;
//
//	private static final int OBSTACLE_SIZE = 16;
//	private static final int CAR_SIZE = 16;
//
//	private static final int CAMERA_WIDTH = RACETRACK_WIDTH * 5;
//	private static final int CAMERA_HEIGHT = RACETRACK_WIDTH * 3;
//
//	// ===========================================================
//	// Fields
//	// ===========================================================
//
////	private Camera mCamera;
//
////	private BitmapTextureAtlas mVehiclesTexture;
////	private TiledTextureRegion mVehiclesTextureRegion;
//
////	private BitmapTextureAtlas mBoxTexture;
////	private ITextureRegion mBoxTextureRegion;
//
////	private BitmapTextureAtlas mRacetrackTexture;
////	private ITextureRegion mRacetrackStraightTextureRegion;
////	private ITextureRegion mRacetrackCurveTextureRegion;
//
////	private BitmapTextureAtlas mOnScreenControlTexture;
////	private ITextureRegion mOnScreenControlBaseTextureRegion;
////	private ITextureRegion mOnScreenControlKnobTextureRegion;
//
//	//private Scene mScene;
//
//	private Dynamics2D mPhysicsWorld;
//
//	private Body2D mCarBody;
//	//private TiledSprite mCar;
//
//	// ===========================================================
//	// Constructors
//	// ===========================================================
//
//	// ===========================================================
//	// Getter & Setter
//	// ===========================================================
//
//	// ===========================================================
//	// Methods for/from SuperClass/Interfaces
//	// ===========================================================
//
////	@Override
////	public EngineOptions onCreateEngineOptions() {
////		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
////
////		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
////	}
//
////	@Override
////	public void onCreateResources() {
////		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
////
////		this.mVehiclesTexture = new BitmapTextureAtlas(this.getTextureManager(), 128, 16, TextureOptions.BILINEAR);
////		this.mVehiclesTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mVehiclesTexture, this, "vehicles.png", 0, 0, 6, 1);
////		this.mVehiclesTexture.load();
////
////		this.mRacetrackTexture = new BitmapTextureAtlas(this.getTextureManager(), 128, 256, TextureOptions.REPEATING_NEAREST);
////		this.mRacetrackStraightTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_straight.png", 0, 0);
////		this.mRacetrackCurveTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_curve.png", 0, 128);
////		this.mRacetrackTexture.load();
////
////		this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
////		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
////		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);
////		this.mOnScreenControlTexture.load();
////
////		this.mBoxTexture = new BitmapTextureAtlas(this.getTextureManager(), 32, 32, TextureOptions.BILINEAR);
////		this.mBoxTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBoxTexture, this, "box.png", 0, 0);
////		this.mBoxTexture.load();
////	}
//
//
//	public Racer() {
//		//this.mEngine.registerUpdateHandler(new FPSLogger());
//
////		this.mScene = new Scene();
////		this.mScene.setBackground(new Background(0, 0, 0));
//
//		this.mPhysicsWorld = new Dynamics2D(new v2(0,0));
//
//		this.initRacetrack();
//		this.initRacetrackBorders();
//		this.initCar();
//		this.initObstacles();
//		//this.initOnScreenControls();
//
////		this.mScene.registerUpdateHandler(this.mPhysicsWorld);
//
////		return this.mScene;
//	}
//
//
//
//	// ===========================================================
//	// Methods
//	// ===========================================================
//
////	private void initOnScreenControls() {
////		final AnalogOnScreenControl analogOnScreenControl = new AnalogOnScreenControl(0, CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight(), this.mCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, this.getVertexBufferObjectManager(), new IAnalogOnScreenControlListener() {
////			@Override
//			public void onControlChange(final float pValueX, final float pValueY) {
//				final Body2D carBody = Racer.this.mCarBody;
//
//				final v2 velocity = new v2(pValueX * 5, pValueY * 5);
//				carBody.setLinearVelocity(velocity);
//				
//
//				final float rotationInRad = (float)Math.atan2(-pValueX, pValueY);
//				carBody.setTransform(carBody.getWorldCenter(), rotationInRad);
//
//				mCar.setRotation(MathUtils.radian2degrees(rotationInRad));
//			}
//
////			@Override
////			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
////				/* Nothing. */
////			}
////		});
////		analogOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
////		analogOnScreenControl.getControlBase().setAlpha(0.5f);
////		//		analogOnScreenControl.getControlBase().setScaleCenter(0, 128);
////		//		analogOnScreenControl.getControlBase().setScale(0.75f);
////		//		analogOnScreenControl.getControlKnob().setScale(0.75f);
////		analogOnScreenControl.refreshControlKnobPosition();
////
////		this.mScene.setChildScene(analogOnScreenControl);
////	}
//
//	private void initCar() {
//		this.mCar = new TiledSprite(20, 20, CAR_SIZE, CAR_SIZE, this.mVehiclesTextureRegion, this.getVertexBufferObjectManager());
//		this.mCar.setCurrentTileIndex(0);
//
//		final FixtureDef carFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);
//		this.mCarBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, this.mCar, BodyType.DynamicBody, carFixtureDef);
//
//		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this.mCar, this.mCarBody, true, false));
//
//		this.mScene.attachChild(this.mCar);
//	}
//
//	private void initObstacles() {
//		this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
//		this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
//		this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
//		this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
//	}
//
//	private void addObstacle(final float pX, final float pY) {
//
//		final FixtureDef boxFixtureDef = PhysicsFactory.createFixtureDef(0.1f, 0.5f, 0.5f);
//		final Body boxBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, box, BodyType.DynamicBody, boxFixtureDef);
//		boxBody.setLinearDamping(10);
//		boxBody.setAngularDamping(10);
//
//		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(box, boxBody, true, true));
//
//		this.mScene.attachChild(box);
//	}
//
////	private void initRacetrack() {
////		/* Straights. */
////		{
////			final ITextureRegion racetrackHorizontalStraightTextureRegion = this.mRacetrackStraightTextureRegion.deepCopy();
////			racetrackHorizontalStraightTextureRegion.setTextureWidth(3 * this.mRacetrackStraightTextureRegion.getWidth());
////
////			final ITextureRegion racetrackVerticalStraightTextureRegion = this.mRacetrackStraightTextureRegion;
////
////			/* Top Straight */
////			this.mScene.attachChild(new Sprite(RACETRACK_WIDTH, 0, 3 * RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackHorizontalStraightTextureRegion, this.getVertexBufferObjectManager()));
////			/* Bottom Straight */
////			this.mScene.attachChild(new Sprite(RACETRACK_WIDTH, CAMERA_HEIGHT - RACETRACK_WIDTH, 3 * RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackHorizontalStraightTextureRegion, this.getVertexBufferObjectManager()));
////
////			/* Left Straight */
////			final Sprite leftVerticalStraight = new Sprite(0, RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackVerticalStraightTextureRegion, this.getVertexBufferObjectManager());
////			leftVerticalStraight.setRotation(90);
////			this.mScene.attachChild(leftVerticalStraight);
////			/* Right Straight */
////			final Sprite rightVerticalStraight = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackVerticalStraightTextureRegion, this.getVertexBufferObjectManager());
////			rightVerticalStraight.setRotation(90);
////			this.mScene.attachChild(rightVerticalStraight);
////		}
////
////		/* Edges */
////		{
////			final ITextureRegion racetrackCurveTextureRegion = this.mRacetrackCurveTextureRegion;
////
////			/* Upper Left */
////			final Sprite upperLeftCurve = new Sprite(0, 0, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
////			upperLeftCurve.setRotation(90);
////			this.mScene.attachChild(upperLeftCurve);
////
////			/* Upper Right */
////			final Sprite upperRightCurve = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, 0, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
////			upperRightCurve.setRotation(180);
////			this.mScene.attachChild(upperRightCurve);
////
////			/* Lower Right */
////			final Sprite lowerRightCurve = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, CAMERA_HEIGHT - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
////			lowerRightCurve.setRotation(270);
////			this.mScene.attachChild(lowerRightCurve);
////
////			/* Lower Left */
////			final Sprite lowerLeftCurve = new Sprite(0, CAMERA_HEIGHT - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
////			this.mScene.attachChild(lowerLeftCurve);
////		}
////	}
//
//
//	private void initRacetrackBorders() {
//		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
//
//		final Rectangle bottomOuter = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
//		final Rectangle topOuter = new Rectangle(0, 0, CAMERA_WIDTH, 2);
//		final Rectangle leftOuter = new Rectangle(0, 0, 2, CAMERA_HEIGHT);
//		final Rectangle rightOuter = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);
//
//		final Rectangle bottomInner = new Rectangle(RACETRACK_WIDTH, CAMERA_HEIGHT - 2 - RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2);
//		final Rectangle topInner = new Rectangle(RACETRACK_WIDTH, RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2);
//		final Rectangle leftInner = new Rectangle(RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH);
//		final Rectangle rightInner = new Rectangle(CAMERA_WIDTH - 2 - RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH);
//
//		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, bottomOuter, BodyType.STATIC, wallFixtureDef);
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, topOuter, BodyType.STATIC, wallFixtureDef);
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, leftOuter, BodyType.STATIC, wallFixtureDef);
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, rightOuter, BodyType.STATIC, wallFixtureDef);
//
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, bottomInner, BodyType.STATIC, wallFixtureDef);
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, topInner, BodyType.STATIC, wallFixtureDef);
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, leftInner, BodyType.STATIC, wallFixtureDef);
//		PhysicsFactory.createBoxBody(this.mPhysicsWorld, rightInner, BodyType.STATIC, wallFixtureDef);
//
//		this.mScene.attachChild(bottomOuter);
//		this.mScene.attachChild(topOuter);
//		this.mScene.attachChild(leftOuter);
//		this.mScene.attachChild(rightOuter);
//
//		this.mScene.attachChild(bottomInner);
//		this.mScene.attachChild(topInner);
//		this.mScene.attachChild(leftInner);
//		this.mScene.attachChild(rightInner);
//	}
//
//	// ===========================================================
//	// Inner and Anonymous Classes
//	// ===========================================================
//}
