//package jcog.optimize;
//
//import com.google.common.collect.LinkedHashMultimap;
//import jcog.math.FloatSupplier;
//import net.bytebuddy.ByteBuddy;
//import net.bytebuddy.dynamic.DynamicType;
//import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
//import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
//import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//
//public class PatchOptimize extends Optimize<ByteBuddy> {
//
//    private final ClassLoader rootClassLoader;
//
//    final LinkedHashMultimap<String,
//            Function<DynamicType.Builder,DynamicType.Builder>> mods = LinkedHashMultimap.create();
//
//
//    public PatchOptimize() {
//        super(ByteBuddy::new
//                //.with(TypeValidation.DISABLED)
//                );
//        this.rootClassLoader =
//                Thread.currentThread().getContextClassLoader();
//    }
//
//    public Optimize.Result run(int maxIters, FloatSupplier test) {
//        return run(maxIters, (b) -> {
//
//            final Map<String, byte[]> overrides = new HashMap();
//
//
//            final float[] result = {Float.NEGATIVE_INFINITY};
//
//            Thread t = new Thread(() -> {
//                mods.keys().forEach(targetName -> {
//                    Class target = null;
//                    try {
//                        target = Thread.currentThread().getContextClassLoader().loadClass(targetName);
//                    } catch (ClassNotFoundException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    DynamicType.Builder bb = b.redefine(target)
//                            .name(target.getName());
//
//                    for (Function<DynamicType.Builder,DynamicType.Builder> f : mods.get(targetName)) {
//                        bb = f.apply(bb);
//                    }
//
//                    overrides.put(targetName, bb.make()
//                            .getBytes());
//                });
//
//
//
//
//
//
//                ClassLoader classloader =
//                        new ByteArrayClassLoader.ChildFirst(rootClassLoader,
//                                overrides
//                        );
//
//
//                DynamicType.Loaded<? extends FloatSupplier> t1 = b.redefine(test.getClass()).name("TEST").make()
//                        .load(classloader, ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT);
//                System.out.println(t1.getLoadedAuxiliaryTypes());
//                System.out.println(t1.getAllTypes());
//
//                try {
//                    Class<?> tc = t1.getLoaded();
//                    FloatSupplier fs = (FloatSupplier) (tc.newInstance());
//                    result[0] = fs.asFloat(); //just run it
//                } catch (Throwable e) {
//                    //logger.error('{')
//                    //e.printStackTrace();
//                    result[0] = Float.NEGATIVE_INFINITY;
//                }
//
//            });
//            t.start();
//
//            try {
//
//                t.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            return result[0];
//
//
//        });
//    }
//
//    public PatchOptimize tweakStatic(int min, int max, int inc, String c, String field) {
//
//
//        tweak(min, max, inc, new TweakStatic(c, field));
//
//        return this;
//    }
//
//    private class TweakStatic implements FloatObjectProcedure<ByteBuddy> {
//
//        private final String c;
//        private final String field;
//
//        public TweakStatic(String c, String field) {
//            this.c = c;
//            this.field = field;
//        }
//
//        @Override
//        public String toString() {
//            return c + " " + mods.get(c);
//        }
//
//        @Override
//        public void value(float x, ByteBuddy b) {
//            mods.put(c, (DynamicType.Builder bb) -> {
//                //bb.initializer(Assigner.Typing.valueOf(field).)
//
//                //bb.initializer(AllArguments.Assignment.STRICT. FieldAccessor.ofField(field).appender(Target.))
//                return bb;
//
//                //return bb.field(named("x"+field)).
//
//                //return bb.defineField(field, int.class, Visibility.PUBLIC, Ownership.STATIC).
//                //      value(Math.round(x));
////                transform(new Transformer<FieldDescription>() {
////
////                            @Override
////                            public FieldDescription transform(TypeDescription instrumentedType, FieldDescription target) {
////                                return target;
////                            }
////                        });
//            });
//        }
//    }
//}
