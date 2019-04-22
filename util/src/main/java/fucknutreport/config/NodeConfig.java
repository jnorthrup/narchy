//package fucknutreport.config;
//
//import kotlin.Metadata;
//import kotlin.jvm.JvmStatic;
//import kotlin.jvm.internal.Intrinsics;
//import kotlin.text.StringsKt;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//@Metadata(
//   mv = {1, 1, 13},
//   bv = {1, 0, 3},
//   k = 1,
//   d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0011\bÆ\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u0011\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\nH\u0087\u0004J\u001a\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\nH\u0007J\u0012\u0010\f\u001a\u0004\u0018\u00010\n2\u0006\u0010\r\u001a\u00020\nH\u0007J&\u0010\u000e\u001a\u0004\u0018\u00010\n2\u0006\u0010\u000f\u001a\u00020\n2\b\u0010\u0010\u001a\u0004\u0018\u00010\n2\b\b\u0002\u0010\u0011\u001a\u00020\u0004H\u0007J\u0018\u0010\u0012\u001a\u00020\n2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0013\u001a\u00020\nH\u0007J\u0011\u0010\u0014\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\nH\u0087\u0004J\u0012\u0010\u0015\u001a\u0004\u0018\u00010\n2\u0006\u0010\r\u001a\u00020\nH\u0007J\u001c\u0010\u0016\u001a\u0004\u0018\u00010\n2\u0006\u0010\u0017\u001a\u00020\n2\b\u0010\u0010\u001a\u0004\u0018\u00010\nH\u0007J\u0018\u0010\u0018\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\nH\u0007R\u001c\u0010\u0003\u001a\u00020\u00048\u0006X\u0087\u0004¢\u0006\u000e\n\u0000\u0012\u0004\b\u0005\u0010\u0002\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u001b"},
//   d2 = {"Lfucknutreport/config/NodeConfig;", "", "()V", "insecure", "", "insecure$annotations", "getInsecure", "()Z", "configIs", "key", "", "def", "get", "s", "get2", "configKey", "defaultVal", "quiet", "getK", "default", "notConfig", "qget", "qget2", "nodeVarName", "reportConfig", "javapropname", "val", "eclipserepodeps"}
//)
//public final class NodeConfig {
//   private static final boolean insecure;
//   public static final NodeConfig INSTANCE;
//
//   /** @deprecated */
//   // $FF: synthetic method
//   @JvmStatic
//   public static void insecure$annotations() {
//   }
//
//   public static final boolean getInsecure() {
//      return insecure;
//   }
//
//   @JvmStatic
//   @Nullable
//   public static final String qget2(@NotNull String nodeVarName, @Nullable String defaultVal) {
//      Intrinsics.checkParameterIsNotNull(nodeVarName, "nodeVarName");
//      return get2(nodeVarName, defaultVal, true);
//   }
//
//   @JvmStatic
//   @Nullable
//   public static final String get2(@NotNull String configKey, @Nullable String defaultVal, boolean quiet) {
//      Intrinsics.checkParameterIsNotNull(configKey, "configKey");
//      boolean var5 = false;
//      String var10000 = configKey.toLowerCase();
//      Intrinsics.checkExpressionValueIsNotNull(var10000, "(this as java.lang.String).toLowerCase()");
//      String javapropname = StringsKt.replace$default(var10000, '_', '.', false, 4, (Object)null);
//      var10000 = System.getenv(configKey);
//      if (var10000 == null) {
//         var10000 = System.getProperty(javapropname);
//      }
//
//      if (var10000 == null) {
//         var10000 = defaultVal;
//      }
//
//      if (var10000 != null) {
//         String var4 = var10000;
//         var5 = false;
//         boolean var6 = false;
//         int var8 = false;
//         System.setProperty(javapropname, var4);
//         if (!quiet || insecure) {
//            reportConfig(javapropname, var4);
//         }
//
//         var10000 = var4;
//      } else {
//         var10000 = null;
//      }
//
//      return var10000;
//   }
//
//   // $FF: synthetic method
//   @JvmStatic
//   @Nullable
//   public static String get2$default(String var0, String var1, boolean var2, int var3, Object var4) {
//      if ((var3 & 4) != 0) {
//         var2 = false;
//      }
//
//      return get2(var0, var1, var2);
//   }
//
//   @JvmStatic
//   @NotNull
//   public static final String reportConfig(@NotNull String javapropname, @NotNull String val) {
//      Intrinsics.checkParameterIsNotNull(javapropname, "javapropname");
//      Intrinsics.checkParameterIsNotNull(val, "val");
//      boolean var3 = false;
//      boolean var4 = false;
//      int var6 = false;
//      System.err.println("-D" + javapropname + "=\"" + val + '"');
//      return val;
//   }
//
//   @JvmStatic
//   public static final boolean configIs(@NotNull String key, @NotNull String def) {
//      Intrinsics.checkParameterIsNotNull(key, "key");
//      Intrinsics.checkParameterIsNotNull(def, "def");
//      return Intrinsics.areEqual(getK(key, def), def);
//   }
//
//   // $FF: synthetic method
//   @JvmStatic
//   public static boolean configIs$default(String var0, String var1, int var2, Object var3) {
//      if ((var2 & 2) != 0) {
//         var1 = "true";
//      }
//
//      return configIs(var0, var1);
//   }
//
//   @JvmStatic
//   public static final boolean configIs(@NotNull String key) {
//      Intrinsics.checkParameterIsNotNull(key, "key");
//      boolean var2 = false;
//      boolean var3 = false;
//      int var5 = false;
//      return configIs(key, "true");
//   }
//
//   @JvmStatic
//   public static final boolean notConfig(@NotNull String key) {
//      Intrinsics.checkParameterIsNotNull(key, "key");
//      return configIs(key, "false");
//   }
//
//   @JvmStatic
//   @NotNull
//   public static final String getK(@NotNull String key, @NotNull String var1) {
//      Intrinsics.checkParameterIsNotNull(key, "key");
//      Intrinsics.checkParameterIsNotNull(var1, "default");
//      String var10000 = get2(key, var1, false);
//      if (var10000 == null) {
//         Intrinsics.throwNpe();
//      }
//
//      return var10000;
//   }
//
//   @JvmStatic
//   @Nullable
//   public static final String get(@NotNull String s) {
//      Intrinsics.checkParameterIsNotNull(s, "s");
//      return get2(s, (String)null, false);
//   }
//
//   @JvmStatic
//   @Nullable
//   public static final String qget(@NotNull String s) {
//      Intrinsics.checkParameterIsNotNull(s, "s");
//      return qget2(s, (String)null);
//   }
//
//   private NodeConfig() {
//   }
//
//   static {
//      NodeConfig var0 = new NodeConfig();
//      INSTANCE = var0;
//      insecure = Intrinsics.areEqual(System.getProperty("insecure", "false"), String.valueOf(Boolean.TRUE));
//   }
//}
