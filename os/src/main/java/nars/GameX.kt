package nars

import com.google.common.util.concurrent.AtomicDouble
import jcog.Util
import jcog.exe.Loop
import jcog.func.IntIntToObjectFunction
import jcog.learn.pid.MiniPID
import jcog.learn.ql.HaiQae
import jcog.pri.ScalarValue
import jcog.signal.wave2d.Bitmap2D
import jcog.signal.wave2d.MonoBufImgBitmap2D
import jcog.signal.wave2d.ScaledBitmap2D
import nars.Op.BELIEF
import nars.`$`.`$$`
import nars.attention.TaskLinkWhat
import nars.attention.What
import nars.control.MetaGoal
import nars.control.NARPart
import nars.control.Should
import nars.derive.Deriver
import nars.derive.Derivers
import nars.derive.time.ActionTiming
import nars.derive.time.MixedTimeFocus
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming
import nars.exe.impl.WorkerExec
import nars.game.Game
import nars.game.GameTime
import nars.game.MetaAgent
import nars.gui.NARui
import nars.memory.CaffeineMemory
import nars.op.Arithmeticize
import nars.op.AutoencodedBitmap
import nars.op.Factorize
import nars.op.mental.Inperience
import nars.op.stm.ConjClustering
import nars.op.stm.STMLinker
import nars.sensor.Bitmap2DSensor
import nars.sensor.PixelBag
import nars.table.dynamic.SeriesBeliefTable
import nars.task.util.PriBuffer
import nars.term.Term
import nars.term.atom.Atomic
import nars.time.clock.RealTime
import nars.video.SwingBitmap2D
import nars.video.WaveletBag
import spacegraph.SpaceGraph.window
import spacegraph.space2d.Surface
import spacegraph.space2d.container.grid.Gridding
import spacegraph.space2d.widget.meta.ObjectSurface
import spacegraph.space2d.widget.meter.PaintUpdateMatrixView
import spacegraph.space2d.widget.meter.Plot2D
import spacegraph.video.OrthoSurfaceGraph
import java.awt.Component
import java.awt.image.BufferedImage
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.streams.toList

/**
 * Extensions to NAgent interface:
 *
 *
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
abstract class GameX(id: Term, gameTime: GameTime, n: NAR?) : Game(id, gameTime, n) {

    @Deprecated("")
    constructor(id: String) : this(`$$`<Term>(id), GameTime.durs(1f), null) {
    }

    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected fun senseCamera(id: String, w: java.awt.Container, pw: Int, ph: Int)   =
            senseCamera(id, SwingBitmap2D(w), pw, ph)


    private fun senseCamera(id: String, w: Supplier<BufferedImage>, pw: Int, ph: Int) =
            senseCamera(id, ScaledBitmap2D(w, pw, ph))

    protected fun senseCameraRetina(id: String, w: Component, pw: Int, ph: Int)=
            senseCameraRetina(id, SwingBitmap2D(w), pw, ph)


    private fun senseCameraRetina(id: String, w: Supplier<BufferedImage>, pw: Int, ph: Int)=
            senseCameraRetina(`$$`<Term>(id), w, pw, ph)


    private fun senseCameraRetina(id: Term, w: Supplier<BufferedImage>, pw: Int, ph: Int)=
            senseCamera(id, PixelBag(MonoBufImgBitmap2D(w), pw, ph))

    protected fun senseCameraRetina(id: Term, w: Bitmap2D, pw: Int, ph: Int)=
            senseCamera(id, PixelBag(w, pw, ph))

    protected fun senseCameraFreq(id: String, w: Supplier<BufferedImage>, pw: Int, ph: Int)=
            senseCamera(id, WaveletBag(w, pw, ph))

    protected fun <C : Bitmap2D> senseCamera(id: String?, bc: C): Bitmap2DSensor<C> =
            senseCamera((if (id != null) `$$`<Term>(id) else null) as Term, bc)

    protected fun <C : Bitmap2D> senseCamera(id: Term?, bc: C): Bitmap2DSensor<C> = Bitmap2DSensor(id, bc, nar()).also { addSensor(it) }

    protected fun <C : Bitmap2D> senseCamera(id: IntIntToObjectFunction<nars.term.Term>?, bc: C): Bitmap2DSensor<C> = Bitmap2DSensor(nar(), bc, id).also { addSensor(it) }

    protected fun <C : Bitmap2D> senseCamera(id: IntIntToObjectFunction<nars.term.Term>?, bc: C, defaultFreq: Float): Bitmap2DSensor<C> = Bitmap2DSensor(nar(), defaultFreq, bc, id).also { addSensor(it) }

    protected fun <C : Bitmap2D> addCameraCoded(id: Term?, bc: Supplier<BufferedImage>, sx: Int, sy: Int, ox: Int, oy: Int) = Bitmap2DSensor(id, AutoencodedBitmap(MonoBufImgBitmap2D(bc), sx, sy, ox, oy), nar()).also { addSensor(it) }

    protected fun <C : Bitmap2D> addCameraCoded(id: Term?, bc: C, sx: Int, sy: Int,
                                                ox: Int, oy: Int) = Bitmap2DSensor(id, AutoencodedBitmap(bc, sx, sy, ox, oy), nar()).also { addSensor(it) }


    class HaiQChip(private val q: HaiQae) : Gridding() {
        var plot: Plot2D? = null
            private set
        var rewardSum: AtomicDouble? = null
            private set

        override fun starting() {
            super.starting()
            val `in` = FloatArray(q.ae.inputs())
            plot = Plot2D(1024, Plot2D.Line)
            val inner = Gridding(
                    ObjectSurface(q) as Surface,
                    Gridding(Gridding.VERTICAL,
                            PaintUpdateMatrixView(`in`),
                            PaintUpdateMatrixView(q.ae.x),
                            PaintUpdateMatrixView(q.ae.W),
                            PaintUpdateMatrixView(q.ae.y)
                    ) as Surface,
                    Gridding(Gridding.VERTICAL,
                            PaintUpdateMatrixView(q.q),
                            PaintUpdateMatrixView(q.et)
                    ) as Surface,
                    plot as Surface
            )


            rewardSum = AtomicDouble()


            set(inner)
        }

        operator fun next() {

            plot!!.commit()
        }
    }


    protected class SpaceGraphPart internal constructor(private val surface: Supplier<Surface>, private val w: Int, private val h: Int) : NARPart() {
        private var win: OrthoSurfaceGraph? = null

        override fun starting(nar: NAR) {
            win = window(surface.get(), w, h)
        }

        override fun stopping(nar: NAR) {
            win!!.delete()
            win = null
        }
    }

    companion object {

        internal val initMeta = true
        internal val initMetaRL = false
        internal val metaAllowPause = false
        internal val FRAME = Atomic.atom("frame")
        /**
         * determines memory strength
         */
        internal var durationsPerFrame = 1f

        @Deprecated("")
        fun initFn(narFPS: Float, init: Function<NAR, Game>): NAR = initFn(-1, narFPS, init)

        fun initC(narFPS: Float, init: Consumer<NAR>): NAR = initC(-1, narFPS, init)

        @Deprecated("")
        fun initFn(threads: Int, narFPS: Float, init: Function<NAR, Game>): NAR =
                initC(threads, narFPS, Consumer { init.apply(it) })

        fun initC(threads: Int, narFPS: Float, init: Consumer<NAR>): NAR {

            val n = baseNAR(narFPS * durationsPerFrame, threads)

            init.accept(n)

            initPlugins(n)

            if (initMeta) {

                val metaFPS = narFPS / 2

                n.parts(Game::class.java).filter { g -> g !is MetaAgent }.forEach { g ->
                    val fps = metaFPS
                    val gm = MetaAgent.GameMetaAgent(g, metaAllowPause)
                    n.add(gm)

                }

                val self = MetaAgent.SelfMetaAgent(n)
                if (initMetaRL)
                    self.addRLBoost()
                n.add(self)
                self.pri(0.1f)

            }

            val loop = n.startFPS(narFPS)


            n.runLater {
                n.synch()
                window(NARui.top(n), 1024, 800)
                val map = n.parts(Game::class.java).map { NARui.game(it) }.toList()
                window(Gridding(map), 1024, 768)
            }


            return n
        }

        /**
         * agent builder should name each agent instance uniquely
         * ex: new PoleCart($.p(Atomic.the(PoleCart.class.getSimpleName()), n.self()), n);
         */
        @Deprecated("")
        fun runRTNet(threads: Int, narFPS: Float, netFPS: Float, a: Function<NAR, Game>): NAR {
            return initFn(threads, narFPS, a)
        }

        fun baseNAR(durFPS: Float, _threads: Int): NAR {

            val clock = RealTime.MS()

            clock.durFPS(durFPS.toDouble())

            val threads = if (_threads <= 0) Util.concurrencyExcept(1) else _threads

            val ramGB = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0 * 1024.0)
            return NARS().what { w ->
                TaskLinkWhat(w,
                        256,
                        PriBuffer.DirectTaskBuffer()
                )
            }.exe(WorkerExec(threads, false))
                    .time(clock)
                    .index(when {
                        ramGB >= 0.5 -> CaffeineMemory(Math.round(ramGB * 96.0 * 1024.0))
                        else -> CaffeineMemory.soft()
                    }


                    )
                    .get { config(it) }
        }

        fun config(n: NAR) {
            n.main.pri(0f)
            n.what.remove(n.main.id)

            n.dtDither.set(

                    10


            )

            NAR.causeCapacity.set(16)
            n.termVolMax.set(

                    48

            )


            n.beliefPriDefault.pri(0.5f)
            n.goalPriDefault.pri(0.5f)
            n.questionPriDefault.pri(0.5f)
            n.questPriDefault.pri(0.5f)

            n.beliefConfDefault.set(0.5f)
            n.goalConfDefault.set(0.5f)


            n.emotion.want(MetaGoal.Perceive, 0f /*-0.005f*/)
            n.emotion.want(MetaGoal.Believe, 0.01f)
            n.emotion.want(MetaGoal.Desire, 0.05f)


        }

        private fun initPlugins(n: NAR) {


            n.exe.governor = Should.predictMLP

            Loop.of { n.stats(false, true, System.out); println() }.fps = 0.25f


            Deriver(
                    Derivers.nal(n, 1, 8, "motivation.nal")
                            .add(STMLinker(1))
                            .add(Arithmeticize.ArithmeticIntroduction())
                            .add(Factorize.FactorIntroduction())
                            .add(Inperience())
                            .add(ConjClustering(n, BELIEF, BELIEF, 6, 32) { t -> true }),
                    MixedTimeFocus(
                            NonEternalTaskOccurenceOrPresentDeriverTiming(),

                            ActionTiming()
                    )
            )


        }

        private fun addClock(n: NAR) {
            n.parts(Game::class.java).forEach { g ->
                g.onFrame { ->
                    val now = n.time()
                    val X = g.iterationCount()
                    val radix = 16
                    val x = `$`.pRecurse(false, *`$`.radixArray(X, radix, Integer.MAX_VALUE))

                    val f = `$`.funcImg(FRAME, g.id, x)
                    val t = SeriesBeliefTable.SeriesTask(f, BELIEF, `$`.t(1f, n.confDefault(BELIEF)),
                            now, Math.round(now + g.durLoop()).toLong(),
                            longArrayOf(n.time.nextStamp()))
                    t.pri<ScalarValue>(n.priDefault(BELIEF) * g.what().pri())

                    g.what().accept(t)
                }
            }
        }

        private fun addFuelInjection(n: NAR) {
            n.parts(What::class.java).filter { w -> w.inBuffer is PriBuffer.BagTaskBuffer }.map { w -> w.inBuffer as PriBuffer.BagTaskBuffer }.forEach { b ->
                val pid = MiniPID(0.007, 0.005, 0.0025, 0.0)
                pid.outLimit(0.0, 1.0)
                pid.setOutMomentum(0.1)
                val ideal = 0.5f
                n.onDur { -> b.valve.set(pid.out((ideal - b.load()).toDouble(), 0.0)) }
            }

        }
    }
}

