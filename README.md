![NARchy Logo](https://bytebucket.org/seh/narchy/raw/skynet2/doc/narchy_banner.jpg)

Non-Axiomatic Reasoning System ([NARS](https://sites.google.com/site/narswang/home)) processes **Tasks** imposed by and perceived from its environment, which may include human or animal users, and other computer systems.

**NARchy** is derived from the general-purpose reasoning system [OpenNARS](https://github.com/opennars).

## Usage

**Tasks** can arrive at any time.  There are no restrictions on their content as far as they can be expressed in __Narsese__ (the I/O language of NARS).
 - By default, NARS makes *no assumptions* about the meaning or truth value of input beliefs and goals.
 - How to choose proper inputs and interpret possible outputs for each application is an *open problem* to be solved by its users. :warning: 

```task ::= [budget] <term> <punct> [occurrence] [truth]```

**Punctuation**
 - "."  Belief to be remembered, representing a specified amount of factual evidence with which to revise existing knowledge and derive novel conclusions. 
 - "!"  Goal to be realized, optionally resulting in invoked system operations that satisfy desire.
 - "?"  Question about belief state, find the best matching answer(s) according to active beliefs.
 - "@"  Quest about goal state, find the best matching procedural answers.

**Term**
```
               <term> ::=
                        | <atom>                             // an atomic constant term; Unicode string in an arbitrary alphabet
                        | <integer>                          // integer number
                        | <variable>                         // an atomic variable term
                        | <compound>                         // a term with internal structure

           <compound> ::=
                        | "(--," <term> ")"                  // negation
                        | "--" <term>                          // negation shorthand

                        | "(" <term> {","<term>} ")"         // product (ie. un-ordered vector or list, length >= 0)
                        | "{" <term> {","<term>} "}"         // extensional set (ordered, all unique, length >= 1)
                        | "[" <term> {","<term>} "]"         // intensional set (ordered, all unique, length >= 1)

                        | "("<term> "-->" <term>")"          // inheritance
                        |    <term> ":" <term>               // reverse-inheritance (shorthand)

                        | "("<term> "<->" <term>")"          // similarity (commutive)

                        | "("<term> "==>" <term>")"          // implication
                        | "("<term> "==>"<dt> <term>")"      // implication sequence
                        | "("<term> "=|>" <term>")"          // implication parallel (commutive, dt=0)

                        | "(&&," <term> {","<term>} ")"      // conjunction eternal (commutive)
                        |   "("<term> "&&" <term>")"           // conjunction eternal (commutive, shorthand for size=2)
                        |   "("<term> "&&"<dt> <term>")"       // conjunction sequence (size=2 only, preserving time direction)                        
                        |   "(&|," <term> {","<term>} ")"      // conjunction parallel (shorthand for &&+0), also: (x &| y)
                        |   "(&/," <term> {","<term>} ")"      // conjunction sequence, internally converted to balanced binary recursive (left-heavy) sequence conjunctions, with integer intervals embedded TODO

                        | "(||," <term> {","<term>} ")"      // disjunction, internally converts to negated conjunction of negations, also: (x || y)

                        | "("<term> "-{-" <term>")"          // instance, expanded on input to: {x} --> y
                        | "("<term> "-]-" <term>")"          // property, expanded on input to: x --> [y]
                        | "("<term> "{-]" <term>")"          // instance-property, expanded on input to: {x} --> [y]

                        | "(&," <term> {","<term>} ")"       // extensional intersection, also: (x & y)
                        | "(|," <term> {","<term>} ")"       // intensional intersection, also: (x | y)
                        | "(-," <term> "," <term> ")"        // extensional difference, also: (x - y)
                        | "(~," <term> "," <term> ")"        // intensional difference, also: (x ~ y)

                        | <term>"("<term> {","<term>} ")"    // an operation, function syntax: f(x,y) internally is: ((x,y)-->f)


                 <dt> ::= [+|-]<number>                      //delta-time amount (frames); positive = future, negative = past, +0 = simultaneous
                        | [+|-]<number>["min"|"hr"|"day"...] //delta-time amount (other time metrics) TODO
                  //note: <dt> is stored as 32-bit signed integer

```

Note:
 - Additional restrictions and reductions may be applied to input.  See Op.java.
 - Built-in 'Functors' are executed inline during the term building process. See BuiltIn.java.
   - Functors evaluated inner-most first, from left to right.
   - Results and their reductions may cascade when outer levels are evaluated.
   - Most functors will not evaluate (leaving it untouched) if any parameters are variables ("unbound").  Such variables may be eliminated in derivations allowing functor evaluation to proceed with the contant values.



**Truth** = (frequency, confidence)
```
<truth> ::= "%"<frequency>[";"<confidence>]"%" // two numbers in [0,1]x(0,1)
```
 - Frequency [0..1.0]
    -  0 : "never"
    - 0.5: "maybe"
    -  1 : "always"
 - Confidence (0..1.0]*
    - confidence=1.0 triggers a locked axiomatic belief state that overrides any additional beliefs in its table (EXPERIMENTAL)

**Occurrence** - (64 bit integer, can store resolutions up to Nanosecond precision)
 - specifies a relative (see <dt>) or absolute occurrence time. if unspecified, ETERNAL (TODO)

**Budget**
```
<budget> ::= "$"<priority>  // priority in [0,1]
```
 - Priority [0..1.0]
    - quantified demand for attention, relative to other items in a collection. 
    - if unspecified, a default priority will be assigned to input Tasks based on punctuation and/or truth

**Variables**
 - $X independent variable
    - must span a statement (appearing on both sides)
 - \#Y dependent variable
 - ?Z query variable
    - only useful in question tasks
 - %A pattern variable
    - the most general variable type which is used in meta-NAL to match terms (including other variables)


**Concept** = identified by non-variable, non-negated term
 - TermLinks (bag)
 - TaskLinks (bag)
 - Metadata table
 - Capacity Policy
 - Compound Concepts also include..
    - Belief, Goal, Question, and Quest Task Tables


## Reasoning
![Inference](https://raw.githubusercontent.com/automenta/narchy/skynet5/docs/derivation_pipeline2.png)

As a reasoning system, the [architecture of NARS](http://www.cis.temple.edu/~pwang/Implementation/NARS/architecture.pdf) consists of a **memory**, an **inference engine**, and a **control system**.

The **memory** manages a collection of concepts, a list of operators, and a buffer for new tasks. Each concept is identified by a term, and contains tasks and beliefs directly on the term, as well as links to related tasks and terms.

The **deriver** applies various type of inference, according to a set of built-in rules. Each inference rule derives certain new tasks from a given task and a belief that are related to the same concept.

The **control** determines the cyclical activity of the system:

 1. Select tasks in the buffer to insert into the corresponding concepts, which may include the creation of new concepts and beliefs, as well as direct processing on the tasks.
 2. Select a concept from the memory, then select a task and a belief from the concept.
 3. Feed the task and the belief to the inference engine to produce derived tasks.
 4. Add the derived tasks into the task buffer, and send report to the environment if a task provides a best-so-far answer to an input question, or indicates the realization of an input goal.
 5. Return the processed belief, task, and concept back to memory with feedback.

All choices in steps 1 and 2 are **probabilistic**,
in the sense that all the items (tasks, beliefs, or concepts)
within the scope of the selection are referenced with
varying priority budgets.

When a new item is produced, its priority value is determined
according to its parent items and the conditions of the process which
produces it.

At step 5, the priority values of all the involved items
are adjusted, according to the immediate feedback of the
current cycle.

## What's New

### Continuous-Time NAL7
The most significant difference is NARchy's completely redesigned Temporal Logic (NAL7) system
which uses numeric time differences embedded within temporal compounds.  These allow for 
arbitrary resolution in measuring and interpolating time as opposed to arbitrarily discretized
time intervals.  A concept's beliefs and goals co-locate all temporal and non-temporal varieties of 
its form into separate eternal and temporal belief tables which can not compete with each other
yet support each other when evaluating truth value.

NARchy avoids separate Parallel and Sequential term operator variations of Conjunctions,
 Equivalences, and Implications by using unified ONLY continuous-time Conjunction and Implication operators,
 sharing derivation rules where possibly with their eternal-time analogs.
 Equivalence, having been removed, forces reliance on the existence of bidirectional
 pairs of implication beliefs that would have constructed them -- however they, by themselves,
 more accurately reflect an input temporal model without the obscuration, distortion, and possible contradiction
 caused by the involvement and maintenance of partially redundant, and separate Equivalence beliefs.
 These simplifications are also expected to reduce the overall computation necessarily applied in
 derivation (ie. less rules) and generally 'smooths' certain discontinuities and edge cases caused by
 different temporal and non-temporal operator types, with or without negation and variable
 introduction or substitution.

 Please create an Issue with any contradictory evidence against these claims.
 
### Temporal Belief Tables w/ Microsphere Revection 
In order to fully utilize this added temporal expressiveness, temporal belief tables were
redesigned to support evaluation of concept truth value at any point in time using a 
generalized microsphere interpolation "revection" algorithm which combines revision (interpolation) and
projection (extrapolation).  Temporal revision can be thought of as lossy compression, in that
tasks (as data points in truth-time space) can be merged to empty room for incoming data.  The
 1D "microsphere interpolation" algorithm was chosen and adapted with support for
  varying "illumination" intensity (set to truth confidence values).  The top eternal
  belief/goal, if exists, is applied as the "background" light source in which
  temporal beliefs shine their frequency "color" to the evaluated time point.

### Multithreaded Execution
In a NAR, its Executioner implementation schedules the various types of Tasks input to and generated
by the system.  As an alternative to the original streamlined Single-thread execution mode, a multi-threaded Executioner
implementation offers scalable, asynchronous, and safe parallelism.  Thread-safe versions of
Bags and Task Tables are constructed as appropriate.

### Full-spectrum Negation
In keeping with a design preference for unity and balanced spectral continuity, there
 are no Negation concepts.  Instead, each concept stores
 its complete frequency spectrum within itself and Negation is handled automaticaly and
 transparently during derivation and input/output.  Subterms may be negated, and this
 results in unique compounds, but the top-level term of a task is always stored un-negated.
 This ultimately can result in less concepts (since a negation of a concept doesn't exist separately)
 and eliminates the possibility of a concept contradicting the beliefs of its negation which
 otheriwse would be stored in separate belief tables.  It also
 supports smooth and balanced revection across the 0.5 "maybe" midpoint of the frequency range,
 in both temporal and eternal modes.  Note: Certain Meta-NAL rules have been adapted to compensate
 for missing negations in premise task and belief terms, which are otherwise
 apparent by examination of the task's frequency (< 0.5).
 
### Enhanced Deriver
 NARchy's deriver follows a continued evolution from its beginnings in the OpenNARS 1.6..1.7 versions
 which featured the Termutator to manage the traversal of the space of possible permutations
 while obeying AIKR principles according to limit parameters.  It has some 
 additional features including inline 
 term rewrite functions (ex: set operations and 2nd-layer subtitutions) and integration of
  the temporal functions necessary to appropriately "temporalize" derivations according
  to the timing of premise components.
 
### Virtual Disjunctions 
 Disjunctions are only virtual operators as perceivable by input and displayed on output. They 
 are converted immediately to negated conjunction of negations via DeMorgan's laws.  By preferring 
 the conjunction representation,
 temporal information can not be lost through conversion to or from the non-temporal Disjunction type.

### HijackBag
 High performance, lock-free concurrent unsorted bag based on linear hash probing.  See HijackBag.java

### CurveBag
 Concurrent sorted bag; essentially a fusion of a Map and Sorted List.  See CurveBag.java

### Pressurized Auto-balanced Forgetting
 Auto-forgetting removes the need for specifying arbitrary forgetting rates.  Instead, a forgetting rate is
 determined as a balanced proportion by an accumulated activation "pressure" relative to the bag's existing mass.

### Concept Index 
 A central, concurrent concept index (cache) provides access to all inactive concepts.  The capacity
 of the index can be adjusted in various ways including maximum size, maximum "weight", and weak/soft
 references.  This cache can also serve as an asynchronous reader and writer to longer-term caches 
 which persist on disk or in a database.  The concurrent abilities of this index support
 arbitrarily parallelized reasoner operations along with concurrent concept data structures.
  While individual concept accesses are not yet entirely synchronization-free, this becomes less important as the number
   of concepts generally greatly exceeds the number of threads.
 
### Binary IO Codec for Terms and Tasks
 A compact byte-level codec for terms and tasks allows all concept data to be serialized to and from
 disk, off-heap memory, or network streams.  It is optionally compressed with Snappy compression
 algorithm which offers a tradeoff of speed and size savings.
 
### Concept Allocation Policies
 An adaptive concept "policy" system manages the allowed capacity of the different concept
 data structures according to activity, term complexity, confidence levels, or other heuristics. 
 This can be used, for example, to allow atomic concepts to support more termlinks than compounds, 
 or to allow more beliefs for a concept which has higher confidence values.  It also allows for
 shrinking capacities when a concept is deactivated, acting as another form of lossy
 concept compression which removes less essential components.
 
### NAgent Sensor/Motor API 
 A sensor/motor "NAgent" API for wrapping a NAR reasoner and attaching various sensor and
 motor concepts with specific abilities for transducing input to beliefs
 and effecting behaviors from goals.  This can be used to easily interface a NAR as a 
 reinforcement-learning agent with a specific environment or interface.   It also has support for
 Reward sensor concept which can be desired and focused as the object of procedural questions
 and future predictions with respect to the sensor and motor concepts of its context.

### InterNARchy
 The InterNARchy is a multi-agent p2p mesh network protocol allowing individual NAR peers to communicate
  asynchronously and remotely through messages containing serialized tasks.  In the InterNARchy,
  peers learn to intelligently route their own and others' communications according to the
  budget and/or truth heuristics inherent in the reasoning itself.  Another peer's beliefs can 
  be corroborated, doubted, augmented, summarized, misrepresented, or ignored.  Their questions
  can be answered, reiterated, or answered with more questions.  Goals can be obeyed, reinforced,
  or disobeyed.  The semantics of the various NAL operator and task punctuations covers the range
  of "performatives" offered by
  classical multi-agent communication protocols like FIPA and ACL, but perhaps in a more
  natural way, and enhanced with the added expressiveness of shades of NAL truth and budget. 

### Deep Variable Introduction
See VarIntroduction.java and subclasses, and which rules apply them.

### _Many other changes remain to be documented._
 

## Contents

 - **util** - JCog: supporting utilities
 - **ui** - SpaceGraph: Fractal GUI (OpenGL)
 - **logic** - Non-NARS specific TuProlog fork

 - **nal** - NARchy Non-Axiomatic Logic Reasoner
 
 - **app** - Applications and supporting tools
 - **web** - Web server and client

 - **lab** - Experiments & demos
 


## Requirements
 - Java 12+ http://jdk.java.net/
 - Gradle https://gradle.org/nightly/

## References

 - Offficial OpenNARS Github http://github.com/opennars
 - OpenNARS v2 aka Narjure (Clojure) http://github.com/opennars/opennars2
 - OpenNARS v1 (Java) http://github.com/opennars/opennars 

 - A comprehensive description of NARS [Rigid Flexibility: The Logic of Intelligence](http://www.springer.com/west/home/computer/artificial?SGWID=4-147-22-173659733-0) and [Non-Axiomatic Logic: A Model of Intelligent Reasoning](http://www.worldscientific.com/worldscibooks/10.1142/8665).
 - Papers discussing aspects of the system: [available here](http://www.cis.temple.edu/~pwang/papers.html)
 - Introduction: [The Logic of Intelligence](http://www.cis.temple.edu/~pwang/Publication/logic_intelligence.pdf)
 - High-level engineering plan: [From NARS to a Thinking Machine](http://www.cis.temple.edu/~pwang/Publication/roadmap.pdf)
 - Core Logic: [From Inheritance Relation to Non-Axiomatic Logic](http://www.cis.temple.edu/~pwang/Publication/inheritance_nal.pdf)
 - Semantics: [Experience-Grounded Semantics: A theory for intelligent systems](http://www.cis.temple.edu/~pwang/Publication/semantics.pdf)
 - Memory & Control: [Computation and Intelligence in Problem Solving](http://www.cis.temple.edu/~pwang/Writing/computation.pdf)
 - NAL Spec (2010) https://github.com/opennars/opennars2/blob/2.0.0_postdev1/docs/NAL-Specification.pdf

 - An (outdated) HTML user manual http://www.cis.temple.edu/~pwang/Implementation/NARS/NARS-GUI-Guide.html
 - Old project home page https://code.google.com/p/open-nars/
 - Discussion Group https://groups.google.com/forum/?fromgroups#!forum/open-nars

[![](https://images.microbadger.com/badges/image/automenta/narchy.svg)](http://microbadger.com/images/automenta/narchy "image badge from microbadger.com")