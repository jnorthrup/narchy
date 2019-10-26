package nars.concept.snapshot

import jcog.decide.Roulette
import jcog.math.FloatSupplier
import jcog.pri.HashedPLink
import jcog.pri.PLink
import jcog.pri.PriReference
import jcog.pri.ScalarValue
import jcog.pri.bag.impl.PLinkArrayBag
import jcog.pri.op.PriMerge
import nars.link.TaskLink
import nars.term.Term
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction
import java.util.*
import java.util.function.Predicate

/**
 * caches an array of tasklinks tangent to an atom
 */
class TaskLinkSnapshot {

    val links = PLinkArrayBag<Term>(PriMerge.replace, 0)


    val isEmpty: Boolean
        get() = links.isEmpty

    fun commit(x: Term, items: Iterable<TaskLink>, itemCount: Int, reverse: Boolean) {

        links.capacity<jcog.pri.bag.Bag<Term, PriReference<Term>>>(cap(itemCount))

        links.commit()

        val xh = x.hashCodeShort()
        for (t in items) {
            val y = t.other(x, xh, reverse)
            if (y != null)
                links.put(HashedPLink(y, t.pri()))
        }
    }


    fun sample(filter: Predicate<Term>, punc: Byte, rng: Random): Term? {
        val ll = links.items()
        val lls = Math.min(links.size(), ll!!.size)
        if (lls == 0)
            return null
        else {

            val li = Roulette.selectRouletteCached(lls, IntToFloatFunction { i ->
                val x = ll[i] as PLink<*>
                if (x != null && filter.test(x.id as Term))
                    Math.max(ScalarValue.EPSILON,
                            //x.priPunc(punc)
                            x.pri()
                    )
                else
                    java.lang.Float.NaN
            }, FloatSupplier { rng.nextFloat() })

            val l = if (li >= 0) ll[li] as PLink<Term> else null

            return l?.id
        }

    }

    companion object {


        /**
         * caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference
         */

        protected fun cap(bagSize: Int): Int {
            return Math.max(4, Math.ceil(1 * Math.sqrt(bagSize.toDouble())).toInt() /* estimate */)
            //return bagSize;
        }
    }
}
