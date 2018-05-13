package nars;

/** high-level trans-personal memory interface
 *
 * ```
 * high level memory control vocabulary
 *      load(fromContext[,filter]) - effectively copy(from, SELF, ...)
 *      save(toContext[,filter]) - effectively copy(SELF, to, ...)
 *      copy(from, to[, filter]) - copy from one context into another.  this is more like a mixing operation since it can be partial
 *
 *  contexts are terms and act like URI's.
 *    they define a namespace index offering access to entire sets of memories.
 *    just like URI's, certain patterns may indicate it is accessed by a particular strategy:
 *       RAM memory - side memories running parallel with the current SELF, which could theoreticaly be swapped to, or at least blended with through memory ops
 *       File system - provides user with convenient access to data files, especially for development
 *       File system /tmp - for data too large to fit in memory but not worth saving past the next reboot
 *       Network - any remote internet address and protocol URI)
 *       Spatiotemoral region - indicates its content is relevant to what is or was there. (lucene's 4D double range vector field allows r-tree (range) indexing in 4D: 3D space 1D time)
 *       Database & Search/etc. Index
 *
 *   filters specify desired include/exclude criteria for the memory transfer process.
 *   these may apply to individual items, like their contained characters, budgets, or complexities.
 *   or they may apply in the aggregate by limiting the min/max amount of items processed.
 *   0 filters, 1 filter, or N-filters as they can be chained or parallelized like any effects processing
 *   procedure, analog or digital.
 *
 *   the particular budget state of a set of tasks forming a "memory" (snapshot, as distinct from the
 *   runtime Memory) should not be considered *the* memory state but a possible one, at least the default
 *
 *   for example,
 *   the meaning of a group of tasks can change drastically by shifting the priorities among them.
 *   so the budget state,
 *   collected as one or more additional snapshots which can be applied or mixed.  these will
 *   need to be identified by names as well.  these are not like versions, although you could capture
 *   them at regular points in time to study how it evolves on its own. instead they provide access to
 *   a dimension of alternate cognitive possibilities existing within the same (non-budgeting aspects of a)
 *   collection of knowledge (tasks).
 *
 *   similar kinds of filters can shape runtime system parameters:
 *
 *      task(ctx, filter) - apply task-shaping filters to individual tasks, such as budget shaping
 *      links(ctx, filter) - link-shaping filters, link capacity heuristics / graph connectivity constraints
 *      beliefs(ctx, filter) - belief capacity heuristics
 *      questions(ctx, filter) - question capacity heuristics
 *
 *   each task uses the $pri (intensity) to decide the effort applied in the load/save process.
 *      by default full intensity would apply complete effort to a load and store operation which isnt necessarily instantaneous.  it could start and end in short or long ranges of time, or for example, a synchronization process
 * can be a ongoing background task that doesnt really end and yet it represents some kind of goal for
 * data transmission.
 *
 *      commands are resilient to repeated invocation although the effort could vary
 *  as much as the user/NAR wants.
 *
 * AtomicExec is designed for careful atomic invocation depending on belief/goal states as they
 * change from cycle to cycle.  when a goal concept with an associated AtomicExec operation
 * exceeds a threshold of desire and non-belief it invokes the operation.  when this desire/non-goal
 * threshold decreases or is replaced by either non-desire or sufficient belief (satisfied) then
 * the atomic operation is stopped.  https://github.com/automenta/narchy/blob/skynet5/nal/src/main/java/nars/util/AtomicExec.java#L31
 * ```
 * */
public class Memory {

    public static void main(String[] args) {

    }
}
