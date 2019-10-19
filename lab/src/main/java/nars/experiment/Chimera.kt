package nars.experiment

import nars.GameX
import nars.NAR
import nars.`$`.`$$`
import nars.control.NARPart
import nars.experiment.ArkaNAR.cam
import nars.experiment.ArkaNAR.numeric
import nars.experiment.Tetris.*
import nars.game.Game
import nars.gui.NARui
import nars.gui.sensor.VectorSensorChart
import nars.term.Term
import spacegraph.SpaceGraph.window
import spacegraph.space2d.container.grid.Gridding
import java.util.function.Function

object Chimera {

    @JvmStatic
    fun main(args: Array<String>) {   //potential boredom without the down button.
        System.setProperty("tetris.fall.rate", "5")
        System.setProperty("tetris.can.fall", "true")
        //survival only.
        System.setProperty("tetris.use.density", "false")
        //dot
        System.setProperty("tetris.easy", "true")
        //            reduce con io
        System.setProperty("avg.err", "false")

        GameX.runRT(FPS * thinkPerFrame, { nar: NAR ->
            /** "withg" is unnecessary here, but can be used to scope an object variable as "this" */
            with(nar) {
                //destructuring on RHS of a Pair
                val (a, b) = Pair(ArkaNAR(`$$`<Term>("(noid,a)"), nar, cam, numeric),
                        ArkaNAR(`$$`<Term>("(noid,b)"), nar, cam, numeric)).apply {
                    first.ballSpeed.set(0.7f * first.ballSpeed.toFloat())
                    second.ballSpeed.set(0.33f * first.ballSpeed.toFloat())
                    window(Gridding(
                            Gridding(NARui.game(first), VectorSensorChart(first.cc, first).withControls()),
                            Gridding(NARui.game(second), VectorSensorChart(second.cc, second).withControls())), 800, 800)
                }
                val (t, g, x) = Triple(
                        Tetris(nar, tetris_width, tetris_height).also {
                            window(VectorSensorChart(it.gridVision, it).withControls(), 400, 800)
                        },
                        Gradius(nar),
                        NARio(nar)
                )

                //gratuitous mix-in and operator
                this + a + b + x + t + g

                //we're in a Function that returns a Game -- "g" doesn't explode when we retturn it.
                g
            }
        } as Function<NAR, Game>)
    }
}

private operator fun NAR.plus(a: NARPart) = apply { add(a) }    // [.] apply = return "this",similar with .also{...}

