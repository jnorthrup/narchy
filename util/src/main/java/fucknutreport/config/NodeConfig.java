package fucknutreport.config;

import com.google.common.io.Resources;
import jcog.Log;
import jcog.Texts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;

//import kotlin.jvm.JvmStatic;
//import kotlin.jvm.internal.Intrinsics;


public final class NodeConfig {
   private static final Logger logger = Log.logger(NodeConfig.class);

   private static final boolean insecure;
   private static final NodeConfig INSTANCE;

   /** @deprecated */
   // $FF: synthetic method
   /*@JvmStatic*/
   public static void insecure$annotations() {
   }

   public static boolean getInsecure() {
      return insecure;
   }

   /*@JvmStatic*/
   @Nullable
   private static String qget2(@NotNull String nodeVarName, @Nullable String defaultVal) {
      //Intrinsics.checkParameterIsNotNull(nodeVarName, "nodeVarName");
      return get2(nodeVarName, defaultVal, true);
   }

   /*@JvmStatic*/
   @Nullable
   private static String get2(@NotNull String configKey, @Nullable String defaultVal, boolean quiet) {
      //Intrinsics.checkParameterIsNotNull(configKey, "configKey");
      boolean var5 = false;
      String y = null;
      //Intrinsics.checkExpressionValueIsNotNull(var10000, "(this as java.lang.String).toLowerCase()");
      String javapropname = configKey.toLowerCase().replace('_', '.');//, false, 4, (Object)null);

      y = System.getenv(configKey); //HACK

      if (y == null) {
         y = System.getProperty(javapropname);
         if (y == null) {
            y = System.getenv(javapropname);
            if (y == null) {
               y = System.getProperty(configKey);
            }
         }
      }

      if (y != null) {

         y = Texts.unquote(y);

         boolean var6 = false;
//         int var8 = false;
         System.setProperty(javapropname, y);
         if (!quiet || insecure) {
            reportConfig(javapropname, y);
         }

      } else {
         if (defaultVal == null)
            throw new RuntimeException("configuration unknown: " + configKey);
         else
            y = defaultVal;
      }

      return y;
   }

   // $FF: synthetic method
   /*@JvmStatic*/
   @Nullable
   public static String get2$default(String var0, String var1, boolean var2, int var3, Object var4) {
      if ((var3 & 4) != 0) {
         var2 = false;
      }

      return get2(var0, var1, var2);
   }

   /*@JvmStatic*/
   @NotNull
   private static String reportConfig(@NotNull String javapropname, @NotNull String val) {
      //Intrinsics.checkParameterIsNotNull(javapropname, "javapropname");
      //Intrinsics.checkParameterIsNotNull(val, "val");
//      boolean var3 = false;
//      boolean var4 = false;
//      int var6 = false;
      //System.err.println("-D" + javapropname + "=\"" + val + '"');
      logger.info("-D{}={}", javapropname, val);
      return val;
   }

   /*@JvmStatic*/
   public static boolean configIs(@NotNull String key, @Nullable Boolean def) {
      return Boolean.valueOf( get2(key, def!=null ? def.toString() : null, false) );
   }

//   // $FF: synthetic method
//   /*@JvmStatic*/
//   public static boolean configIs$default(String var0, String var1, int var2, Object var3) {
//      if ((var2 & 2) != 0) {
//         var1 = "true";
//      }
//
//      return configIs(var0, var1);
//   }

   /*@JvmStatic*/
   public static boolean configIs(@NotNull String key) {
      //Intrinsics.checkParameterIsNotNull(key, "key");
//      boolean var2 = false;
//      boolean var3 = false;
//      int var5 = false;
      return configIs(key, null /* "true"*/);
   }

//   /*@JvmStatic*/
//   public static boolean notConfig(@NotNull String key) {
//      //Intrinsics.checkParameterIsNotNull(key, "key");
//      return configIs(key, "false");
//   }

//   /*@JvmStatic*/
//   @NotNull
//   public static String getK(@NotNull String key, @NotNull String var1) {
//      //Intrinsics.checkParameterIsNotNull(key, "key");
//      //Intrinsics.checkParameterIsNotNull(var1, "default");
//      String var10000 = get2(key, var1, false);
//      if (var10000 == null) {
//         //Intrinsics.throwNpe();
//      }
//
//      return var10000;
//   }

   /*@JvmStatic*/
   @Nullable
   public static String get(@NotNull String s) {
      //Intrinsics.checkParameterIsNotNull(s, "s");
      return get2(s, (String)null, false);
   }

   /*@JvmStatic*/
   @Nullable
   public static String qget(@NotNull String s) {
      //Intrinsics.checkParameterIsNotNull(s, "s");
      return qget2(s, (String)null);
   }

   private NodeConfig() {
   }

   static {
      NodeConfig var0 = new NodeConfig();
      try {
         System.getProperties().load(Resources.getResource("defaults.ini").openStream());
      } catch (IOException e) {
         e.printStackTrace();
      }

      INSTANCE = var0;
      //insecure = Intrinsics.areEqual(System.getProperty("insecure", "false"), String.valueOf(Boolean.TRUE));
      insecure = Objects.equals(System.getProperty("insecure", "false"), String.valueOf(Boolean.TRUE));
   }
}
