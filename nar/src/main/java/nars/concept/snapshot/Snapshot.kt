package nars.concept.snapshot

import nars.NAR
import nars.concept.Concept
import nars.term.Term
import nars.time.Tense

import java.lang.ref.Reference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiFunction
import java.util.function.Supplier
import java.util.function.UnaryOperator

/** a container for time-stamped expiring cache of data that can be stored in Concept meta maps  */
 class Snapshot<X> {

/** TODO use AtomicFieldUpdater  */
	protected val busy = AtomicBoolean(false)

/** occurrence time after which the data should be invalidated or refreshed  */
	@Volatile protected var expires = java.lang.Long.MIN_VALUE

/** of type:
 * X, SoftReference<X>, WeakReference<X> </X></X> */
	@Volatile protected var value:Any?=null


 fun get():X? = if (value is Reference<*>) (value as Reference<X>).get() else value as X

/** here the value may be returned as-is or wrapped in a soft or weak ref  */
	protected fun wrap(x: X? ):Any? = x

/** ttl = cycles of cached value before next expiration ( >= 0 )
 * or -1 to never expire  */
	 fun get(now:Long, ttl:Int, updater:UnaryOperator<X?>):X? {
val e = expires
val current = get()
if ((now >= e || current == null) && busy.compareAndSet(false, true))
{
try
{
val nextX = updater.apply(current)
this.expires = if (ttl >= 0) now + ttl.toLong() else Tense.TIMELESS /* forever */
this.value = wrap(nextX)
return nextX
}

finally
{
busy.set(false)
}
}
else
{
 //return the current value
			return current
}
}

companion object {


/** concept()'s the given term in the given NAR  */
     fun <X> get(src:Term, nar:NAR, id:String, now:Long, ttl:Int, updater:BiFunction<Concept, X ?, X?>):X? {
val c = nar.conceptualize(src)
return if (c != null) get<X>(c!!, id, now, ttl, updater) else null
}

 fun <X> get(src:Concept, id:String, now:Long, ttl:Int, updater:BiFunction<Concept, X?, X?>):X? {
return src.meta<Snapshot<X>>(id, Supplier<Snapshot<X>> { Snapshot() })
.get(now, ttl, object:UnaryOperator<X?> {
public override fun apply(existing:X?):X? = updater.apply(src, existing)
})
}
}
}
