/*
 * The MIT License
 *
 * Copyright 2015 Kamnev Georgiy (nt.gocha@gmail.com).
 *
 * Данная лицензия разрешает, безвозмездно, лицам, получившим копию данного программного
 * обеспечения и сопутствующей документации (в дальнейшем именуемыми "Программное Обеспечение"),
 * использовать Программное Обеспечение без ограничений, включая неограниченное право на
 * использование, копирование, изменение, объединение, публикацию, распространение, сублицензирование
 * и/или продажу копий Программного Обеспечения, также как и лицам, которым предоставляется
 * данное Программное Обеспечение, при соблюдении следующих условий:
 *
 * Вышеупомянутый копирайт и данные условия должны быть включены во все копии
 * или значимые части данного Программного Обеспечения.
 *
 * ДАННОЕ ПРОГРАММНОЕ ОБЕСПЕЧЕНИЕ ПРЕДОСТАВЛЯЕТСЯ «КАК ЕСТЬ», БЕЗ ЛЮБОГО ВИДА ГАРАНТИЙ,
 * ЯВНО ВЫРАЖЕННЫХ ИЛИ ПОДРАЗУМЕВАЕМЫХ, ВКЛЮЧАЯ, НО НЕ ОГРАНИЧИВАЯСЬ ГАРАНТИЯМИ ТОВАРНОЙ ПРИГОДНОСТИ,
 * СООТВЕТСТВИЯ ПО ЕГО КОНКРЕТНОМУ НАЗНАЧЕНИЮ И НЕНАРУШЕНИЯ ПРАВ. НИ В КАКОМ СЛУЧАЕ АВТОРЫ
 * ИЛИ ПРАВООБЛАДАТЕЛИ НЕ НЕСУТ ОТВЕТСТВЕННОСТИ ПО ИСКАМ О ВОЗМЕЩЕНИИ УЩЕРБА, УБЫТКОВ
 * ИЛИ ДРУГИХ ТРЕБОВАНИЙ ПО ДЕЙСТВУЮЩИМ КОНТРАКТАМ, ДЕЛИКТАМ ИЛИ ИНОМУ, ВОЗНИКШИМ ИЗ, ИМЕЮЩИМ
 * ПРИЧИНОЙ ИЛИ СВЯЗАННЫМ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ ИЛИ ИСПОЛЬЗОВАНИЕМ ПРОГРАММНОГО ОБЕСПЕЧЕНИЯ
 * ИЛИ ИНЫМИ ДЕЙСТВИЯМИ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ.
 */

package jcog.reflect;


import jcog.data.list.FasterList;
import jcog.reflect.graph.Path;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
//import xyz.cofe.fs.FileSystem;
//import xyz.cofe.fs.FileSystems;

/**
 * Граф конвертирования типов с базовым набором типов. <br>
 * Базовые числа между собой: <br>
 * Byte, byte &lt;=&gt; byte, Byte <br>
 * Short, short &lt;=&gt; short, Short <br>
 * Integer, int &lt;=&gt; int, Integer <br>
 * Long, long &lt;=&gt; long, Long <br>
 * Float, float &lt;=&gt; float, Float <br>
 * Double, double &lt;=&gt; double, Double <br>
 * <br>
 * <p>
 * Числа между числами: <br>
 * Number, Byte &lt;=&gt; Byte, Number <br>
 * Number, Short &lt;=&gt; Short, Number <br>
 * Number, Integer &lt;=&gt; Integer, Number <br>
 * Number, Long &lt;=&gt; Long, Number <br>
 * Number, Float &lt;=&gt; Float, Number <br>
 * Number, Double &lt;=&gt; Double, Number <br>
 * <br>
 * <p>
 * BigInteger/BigDecimal между числами: <br>
 * Number, BigDecimal &lt;=&gt; BigDecimal, Number <br>
 * Number, BigInteger &lt;=&gt; BigInteger, Number <br>
 * <br>
 * <p>
 * boolean типы: <br>
 * Boolean, boolean &lt;=&gt; boolean, Boolean <br>
 * <br>
 * <p>
 * Символные типы: <br>
 * Character, char &lt;=&gt; char, Character <br>
 * <p>
 * char, Character &lt;=&gt; int, Integer <br>
 * char, Character &lt;=&gt; String <br>
 * <br>
 * <p>
 * boolean - числа - string типы: <br>
 * int, Integer &lt;=&gt; boolean, Boolean <br>
 * <p>
 * BigDecimal, BigInteger &lt;=&gt; BigInteger, BigDecimal <br>
 * <p>
 * Number &#x2192; String <br>
 * String &#x2192; Integer <br>
 * String &#x2192; int <br>
 * String &#x2192; Long <br>
 * String &#x2192; long <br>
 * String &#x2192; Double <br>
 * String &#x2192; double <br>
 * String &#x2192; BigDecimal <br>
 * <br>
 * <p>
 * Даты: <br>
 * java.util.Date &lt;=&gt; String <br>
 * java.util.Date &lt;=&gt; java.sql.Date <br>
 * java.util.Date &lt;=&gt; java.sql.Time <br>
 * java.util.Date &lt;=&gt; java.sql.Timestamp <br>
 * <p>
 * String &#x2192; java.sql.Date <br>
 * String &#x2192; java.sql.Time <br>
 * String &#x2192; java.sql.Timestamp <br>
 * <br>
 * <p>
 * Бинарные данные: <br>
 * byte[] &lt;=&gt; String <br>
 * Byte[] &lt;=&gt; String <br>
 * char[] &lt;=&gt; String <br>
 * Character[] &lt;=&gt; String <br>
 * <p>
 * java.sql.Clob &#x2192; String <br>
 * java.sql.NClob &#x2192; String <br>
 * <br>
 * <p>
 * Файлы (путь): <br>
 * java.net.URL &lt;=&gt; String <br>
 * java.net.URI &lt;=&gt; String <br>
 * java.io.File &lt;=&gt; String <br>
 * java.io.File &#x2192; URL <br>
 * java.io.File &#x2192; URI <br>
 * xyz.cofe.fs.File &lt;=&gt; String <br>
 * xyz.cofe.io.File &lt;=&gt; String <br>
 * xyz.cofe.io.File &lt;=&gt; java.nio.file.Path <br>
 * xyz.cofe.io.File &lt;=&gt; java.io.File <br>
 * java.nio.charset.Charset &lt;=&gt; String <br>
 *
 * @author Kamnev Georgiy (nt.gocha@gmail.com)
 */
public class CastGraph extends TypeCastGraph {
    //<editor-fold defaultstate="collapsed" desc="Базовые типы">
    //<editor-fold defaultstate="collapsed" desc="Числовые типы">
    //<editor-fold defaultstate="collapsed" desc="числовые примитивы integer, byte, ... Integer, Byte, ...">
    //<editor-fold defaultstate="collapsed" desc="integer - int">
    public static final Function int2Integer = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "int2Integer";
        }
    };
    public static final Function integer2Int = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "integer2Int";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="Byte - byte">
    public static final Function byte2Byte = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "byte2Byte";
        }
    };
    public static final Function Byte2byte = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Byte2byte";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="Short - short">
    public static final Function short2Short = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "short2Short";
        }
    };
    public static final Function Short2short = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Short2short";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="Long - long">
    public static final Function long2Long = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "long2Long";
        }
    };
    //</editor-fold>
    public static final Function Long2long = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Long2long";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="Float - float">
    public static final Function float2Float = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Long2long";
        }
    };
    public static final Function Float2float = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Float2float";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="Double - double">
    public static final Function double2Double = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "double2Double";
        }
    };
    public static final Function Double2double = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Double2double";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="byte, short,int,... - Number">
    //<editor-fold defaultstate="collapsed" desc="Number - Byte">
    public static final Function Byte2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Byte2Number";
        }
    };
    public static final Function Number2Byte = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return ((Number) from).byteValue();
        }

        @Override
        public String toString() {
            return "Number2Byte";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - Short">
    public static final Function Short2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Short2Number";
        }
    };
    public static final Function Number2Short = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return ((Number) from).shortValue();
        }

        @Override
        public String toString() {
            return "Number2Short";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - Integer">
    public static final Function Integer2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Integer2Number";
        }
    };
    public static final Function Number2Integer = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return ((Number) from).intValue();
        }

        @Override
        public String toString() {
            return "Number2Integer";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - Long">
    public static final Function Long2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Long2Number";
        }
    };
    public static final Function Number2Long = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return ((Number) from).longValue();
        }

        @Override
        public String toString() {
            return "Number2Long";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - Float">
    public static final Function Float2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Float2Number";
        }
    };
    public static final Function Number2Float = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return ((Number) from).floatValue();
        }

        @Override
        public String toString() {
            return "Number2Float";
        }
    };
    //</editor-fold>
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - Double">
    public static final Function Double2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Double2Number";
        }
    };
    public static final Function Number2Double = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return ((Number) from).doubleValue();
        }

        @Override
        public String toString() {
            return "Number2Double";
        }
    };
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - BigDecimal">
    public static final Function BigDecimal2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "BigDecimal2Number";
        }
    };
    public static final Function Number2BigDecimal = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            if (from instanceof Byte) {
                Byte b = ((Byte) from);
                return new BigDecimal(b.intValue());
            }
            if (from instanceof Short) {
                Short s = (Short) from;
                return new BigDecimal(s.intValue());
            }
            if (from instanceof Integer) {
                Integer i = (Integer) from;
                return new BigDecimal(i);
            }
            if (from instanceof Long) {
                Long l = (Long) from;
                return new BigDecimal(l);
            }
            if (from instanceof Float) {
                Float f = (Float) from;
                return new BigDecimal(f);
            }
            if (from instanceof Double) {
                Double d = (Double) from;
                return new BigDecimal(d);
            }
            if (from instanceof Number) {
                double d = ((Number) from).doubleValue();
                return new BigDecimal(d);
            }
            throw new Error("can't " + from + " cast to BigDecimal");
        }

        @Override
        public String toString() {
            return "Number2BigDecimal";
        }
    };
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - BigInteger">
    public static final Function BigInteger2Number = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "BigInteger2Number";
        }
    };
    public static final Function Number2BigInteger = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            if (from instanceof Byte) {
                Byte b = ((Byte) from);
                return BigInteger.valueOf(b);
            }
            if (from instanceof Short) {
                Short s = (Short) from;
                return BigInteger.valueOf(s);
            }
            if (from instanceof Integer) {
                Integer i = (Integer) from;
                return BigInteger.valueOf(i);
            }
            if (from instanceof Long) {
                Long l = (Long) from;
                return BigInteger.valueOf(l);
            }
            if (from instanceof Float) {
                Float f = (Float) from;
                return BigInteger.valueOf(f.longValue());
            }
            if (from instanceof Double) {
                Double d = (Double) from;
                return BigInteger.valueOf(d.longValue());
            }
            if (from instanceof Number) {
                return BigInteger.valueOf(((Number) from).longValue());
            }
            throw new Error("can't " + from + " cast to BigInteger");
        }

        @Override
        public String toString() {
            return "Number2BigInteger";
        }
    };
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="BigDecimal - BigInteger">
    public static final Function BigInteger2BigDecimal = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            BigInteger v = (BigInteger) from;
            return new BigDecimal(v);
        }

        @Override
        public String toString() {
            return "BigInteger2BigDecimal";
        }
    };
    public static final Function BigDecimal2BigInteger = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            BigDecimal v = (BigDecimal) from;
            return v.toBigInteger();
        }

        @Override
        public String toString() {
            return "BigDecimal2BigInteger";
        }
    };
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Boolean - boolean">
    public static final Function boolean2Boolean = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "boolean2Boolean";
        }
    };
    public static final Function Boolean2boolean = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Boolean2boolean";
        }
    };
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Boolean - String">
    public static final Function Boolean2String = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return ((Boolean) from).toString();
        }

        @Override
        public String toString() {
            return "Boolean2String";
        }
    };
    public static final Function String2Boolean = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            String str = from.toString().trim();
            if (str.equalsIgnoreCase("true")) return true;
            if (str.equalsIgnoreCase("false")) return false;
            throw new Error("can't cast string(" + str + ") to boolean");
        }

        @Override
        public String toString() {
            return "String2Boolean";
        }
    };
//</editor-fold>
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Character - char">
    public static final Function char2Character = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "char2Character";
        }
    };
    public static final Function Character2char = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from;
        }

        @Override
        public String toString() {
            return "Character2char";
        }
    };
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="char - int">
    public static final Function char2int = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return (int) (char) (Character) from;
        }

        @Override
        public String toString() {
            return "char2int";
        }
    };
    public static final Function int2char = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            int i = ((Integer) from);
            return (char) i;
        }

        @Override
        public String toString() {
            return "int2char";
        }
    };
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="char - string">
    public static final Function char2String = new MutableWeightedCaster(2) {
        @Override
        public Object apply(Object from) {
            char c = (Character) from;
            return String.valueOf(c);
        }

        @Override
        public String toString() {
            return "char2String";
        }
    };
    public static final Function String2char = new MutableWeightedCaster(2) {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return !str.isEmpty() ? str.charAt(0) : (char) 0;
        }

        @Override
        public String toString() {
            return "String2char";
        }
    };
    //</editor-fold>
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Integer - boolean">
    public static final Function Integer2Boolean = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            int v = ((Integer) from);
            if (v == 0) return false;
            return v == 1;
        }

        @Override
        public String toString() {
            return "Integer2Boolean";
        }
    };
    public static final Function Boolean2Integer = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            boolean v = ((Boolean) from);
            return v ? 1 : 0;
        }

        @Override
        public String toString() {
            return "Boolean2Integer";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Number - String">
    public static final Function Number2String = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return from.toString();
        }

        @Override
        public String toString() {
            return "Number2String";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="parse string to number">
    //<editor-fold defaultstate="collapsed" desc="String 2 Integer">
    public static final Function String2Integer = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return Integer.parseInt(str);
        }

        @Override
        public String toString() {
            return "String2Integer";
        }
    };
    //</editor-fold>
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="String 2 int">
    public static final Function String2int = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return Integer.parseInt(str);
        }

        @Override
        public String toString() {
            return "String2int";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="String 2 Long">
    public static final Function String2Long = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return Long.parseLong(str);
        }

        @Override
        public String toString() {
            return "String2Long";
        }
    };
    //</editor-fold>
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="String 2 long">
    public static final Function String2long = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return Long.parseLong(str);
        }

        @Override
        public String toString() {
            return "String2long";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="String 2 Double">
    public static final Function String2Double = new MutableWeightedCaster(2) {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return Double.parseDouble(str);
        }

        @Override
        public String toString() {
            return "String2Double";
        }
    };
    //</editor-fold>
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="String 2 Double">
    public static final Function String2double = new MutableWeightedCaster(2) {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return Double.parseDouble(str);
        }

        @Override
        public String toString() {
            return "String2double";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="String 2 BigDecimal">
    public static final Function String2BigDecimal = new MutableWeightedCaster(2) {
        @Override
        public Object apply(Object from) {
            String str = (String) from;
            return new BigDecimal(str);
        }

        @Override
        public String toString() {
            return "String2BigDecimal";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="char[] 2 String">
    public static final Function charArr2String = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            char[] ca = (char[]) from;
            return new String(ca);
        }

        @Override
        public String toString() {
            return "charArr2String";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="Character[] 2 String">
    public static final Function CharArr2String = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            Character[] ca = (Character[]) from;
            StringBuilder sb = new StringBuilder();
            for (Character c : ca) {
                sb.append(c);
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "CharArr2String";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="String 2 char[]">
    public static final Function String2charArr = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            String str = ((String) from);
            char[] arr = new char[str.length()];
            for (int i = 0; i < arr.length; i++) arr[i] = str.charAt(i);
            return arr;
        }

        @Override
        public String toString() {
            return "String2charArr";
        }
    };
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="String 2 Character[]">
    public static final Function String2CharArr = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            String str = ((String) from);
            Character[] arr = new Character[str.length()];
            for (int i = 0; i < arr.length; i++) arr[i] = str.charAt(i);
            return arr;
        }

        @Override
        public String toString() {
            return "String2CharArr";
        }
    };
    //<editor-fold defaultstate="collapsed" desc="Clob 2 String">
    public static final Function Clob2String = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            Clob clob = ((Clob) from);
            return ClobToString.stringOf(clob);
        }

        @Override
        public String toString() {
            return "Clob2String";
        }
    };
    //</editor-fold>
    public final Function Date2SqlDate = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            Date d = (Date) from;
            return new java.sql.Date(d.getTime());
        }

        @Override
        public String toString() {
            return "Date2SqlDate";
        }
    };
    public final Function SqlDate2Date = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return (java.sql.Date) from;
        }

        @Override
        public String toString() {
            return "SqlDate2Date";
        }
    };
    //</editor-fold>
    public final Function Date2SqlTime = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            Date d = (Date) from;
            return new java.sql.Time(d.getTime());
        }

        @Override
        public String toString() {
            return "Date2SqlTime";
        }
    };
    public final Function SqlTime2Date = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            return (java.sql.Time) from;
        }

        @Override
        public String toString() {
            return "SqlTime2Date";
        }
    };
    //</editor-fold>
    public final Function Date2SqlTimestamp = new MutableWeightedCaster() {
        @Override
        public Object apply(Object from) {
            Date d = (Date) from;
            return new java.sql.Timestamp(d.getTime());
        }

        @Override
        public String toString() {
            return "Date2SqlTimestamp";
        }
    };
    //</editor-fold>
    //</editor-fold>
//
//    // TODO use proj text
//    //<editor-fold defaultstate="collapsed" desc="byte / char arrays">
//    //<editor-fold defaultstate="collapsed" desc="String 2 byte[]">
////    public static final Convertor String2byteArr = new MutableWeightedCaster() {
////        @Override
////        public Object convert(Object from) {
////            return xyz.cofe.text.Text.decodeHex( (String)from );
////        }
////        @Override public String toString(){ return "String2byteArr"; }
////    };
//    //</editor-fold>
//
//    // TODO use proj text
//    //<editor-fold defaultstate="collapsed" desc="byte[] 2 String">
////    public static final Convertor byteArr2String = new MutableWeightedCaster() {
////        @Override
////        public Object convert(Object from) {
////            byte[] ba = (byte[])from;
////            return xyz.cofe.text.Text.encodeHex(ba);
////        }
////        @Override public String toString(){ return "byteArr2String"; }
////    };
//    //</editor-fold>
//
//    // TODO use proj text
//    //<editor-fold defaultstate="collapsed" desc="Byte[] 2 String">
////    public static final Convertor ByteArr2String = new MutableWeightedCaster() {
////        @Override
////        public Object convert(Object from) {
////            Byte[] ba = (Byte[])from;
////            return xyz.cofe.text.Text.encodeHex(ba);
////        }
////        @Override public String toString(){ return "ByteArr2String"; }
////    };
//    //</editor-fold>
//
//    // TODO use proj text
//    //<editor-fold defaultstate="collapsed" desc="String 2 Byte[]">
////    public static final Convertor String2ByteArr = new MutableWeightedCaster() {
////        @Override
////        public Object convert(Object from) {
////            return xyz.cofe.text.Text.decodeHexBytes((String)from);
////        }
////        @Override public String toString(){ return "String2ByteArr"; }
////    };
//    //</editor-fold>
//    public final Function SqlTimestamp2Date = new MutableWeightedCaster() {
//        @Override
//        public Object apply(Object from) {
//            return (java.sql.Timestamp) from;
//        }
//
//        @Override
//        public String toString() {
//            return "SqlTimestamp2Date";
//        }
//    };
//    //</editor-fold>
//    public final Function String2SqlDate = new MutableWeightedCaster() {
//        @Override
//        public Object apply(Object from) {
//            synchronized (CastGraph.this) {
//                //java.util.Date d = getDateFormat().parse((String)from);
//                Date d = (Date) String2Date.apply(from);
//                return new java.sql.Date(d.getTime());
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "String2SqlDate";
//        }
//    };
//    //</editor-fold>
//    public final Function String2SqlTime = new MutableWeightedCaster() {
//        @Override
//        public Object apply(Object from) {
//            synchronized (CastGraph.this) {
//                Date d = (Date) String2Date.apply(from);
//                return new java.sql.Time(d.getTime());
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "String2SqlTime";
//        }
//    };
//    //</editor-fold>
//    public final Function String2SqlTimestamp = new MutableWeightedCaster() {
//        @Override
//        public Object apply(Object from) {
//            synchronized (CastGraph.this) {
//                Date d = (Date) String2Date.apply(from);
//                return new java.sql.Timestamp(d.getTime());
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "String2SqlTimestamp";
//        }
//    };
    //</editor-fold>
//</editor-fold>
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="date and time">
    //<editor-fold defaultstate="collapsed" desc="date string format">
    private SimpleDateFormat[] dateFormats = new SimpleDateFormat[]{
            new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ")
    };
//    //<editor-fold defaultstate="collapsed" desc="date time convertors">
//    public final Function Date2String = new MutableWeightedCaster() {
//        @Override
//        public Object apply(Object from) {
//            synchronized (CastGraph.this) {
//                Date d = (Date) from;
//                SimpleDateFormat[] dfs = getDateFormats();
//                SimpleDateFormat df = dfs != null && dfs.length > 0 ? dfs[0] : new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
//                return df.format(d);
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "Date2String";
//        }
//    };
//    public final Function String2Date = new MutableWeightedCaster() {
//        @Override
//        public Object apply(Object from) {
//            synchronized (CastGraph.this) {
//                SimpleDateFormat[] dfs = getDateFormats();
//                if (dfs == null) throw new IllegalStateException("date formats not setted");
//                for (SimpleDateFormat df : dfs) {
//                    try {
//                        return df.parse((String) from);
//                    } catch (ParseException ex) {
//                    }
//                }
//                throw new Error("can't cast from " + from + " to java.util.Date");
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "String2Date";
//        }
//    };
    //</editor-fold>

//    /**
//     * Конструктор копирования
//     *
//     * @param src Исходный объект
//     */
//    public CastGraph(CastGraph src) {
//        super(src);
//        if (src != null) {
//            this.dateFormats = src.dateFormats;
//        }
//    }

    /**
     * Базовый конструктор
     */
    public CastGraph() {
        set(Integer.class, int.class, integer2Int);
        set(int.class, Integer.class, int2Integer);

        set(Byte.class, byte.class, Byte2byte);
        set(byte.class, Byte.class, byte2Byte);

        set(Short.class, short.class, Short2short);
        set(short.class, Short.class, short2Short);

        set(Long.class, long.class, Long2long);
        set(long.class, Long.class, long2Long);

        set(Float.class, float.class, Float2float);
        set(float.class, Float.class, float2Float);

        set(Double.class, double.class, Double2double);
        set(double.class, Double.class, double2Double);

        set(Number.class, Byte.class, Number2Byte);
        set(Byte.class, Number.class, Byte2Number);

        set(Number.class, Short.class, Number2Short);
        set(Short.class, Number.class, Short2Number);

        set(Number.class, Integer.class, Number2Integer);
        set(Integer.class, Number.class, Integer2Number);

        set(Number.class, Long.class, Number2Long);
        set(Long.class, Number.class, Long2Number);

        set(Number.class, Float.class, Number2Float);
        set(Float.class, Number.class, Float2Number);

        set(Number.class, Double.class, Number2Double);
        set(Double.class, Number.class, Double2Number);

        set(Number.class, BigDecimal.class, Number2BigDecimal);
        set(BigDecimal.class, Number.class, BigDecimal2Number);

        set(Number.class, BigInteger.class, Number2BigInteger);
        set(BigInteger.class, Number.class, BigInteger2Number);

        set(Boolean.class, boolean.class, Boolean2boolean);
        set(boolean.class, Boolean.class, boolean2Boolean);

        set(Character.class, char.class, Character2char);
        set(char.class, Character.class, char2Character);

        // ..........

        set(Number.class, String.class, Number2String);

        set(char.class, int.class, char2int);
        set(Character.class, int.class, char2int);
        set(char.class, Integer.class, char2int);
        set(Character.class, Integer.class, char2int);

        set(int.class, char.class, int2char);
        set(int.class, Character.class, int2char);
        set(Integer.class, char.class, int2char);
        set(Integer.class, Character.class, int2char);

        set(char.class, String.class, char2String);
        set(Character.class, String.class, char2String);
        set(String.class, char.class, String2char);
        set(String.class, Character.class, String2char);

        set(Integer.class, Boolean.class, Integer2Boolean);
        set(int.class, Boolean.class, Integer2Boolean);
        set(Integer.class, boolean.class, Integer2Boolean);
        set(int.class, boolean.class, Integer2Boolean);

        set(Boolean.class, Integer.class, Boolean2Integer);
        set(Boolean.class, int.class, Boolean2Integer);
        set(boolean.class, Integer.class, Boolean2Integer);
        set(boolean.class, int.class, Boolean2Integer);

        set(Boolean.class, String.class, Boolean2String);
        set(boolean.class, String.class, Boolean2String);

        set(String.class, Boolean.class, String2Boolean);
        set(String.class, boolean.class, String2Boolean);

        set(BigInteger.class, BigDecimal.class, BigInteger2BigDecimal);
        set(BigDecimal.class, BigInteger.class, BigDecimal2BigInteger);

        set(String.class, Integer.class, String2Integer);
        set(String.class, int.class, String2int);

        set(String.class, Long.class, String2Long);
        set(String.class, long.class, String2long);

        set(String.class, Double.class, String2Double);
        set(String.class, double.class, String2double);

        set(String.class, BigDecimal.class, String2BigDecimal);

        byte[] ba = new byte[]{};
        Byte[] Ba = new Byte[]{};
        char[] ca = new char[]{};
        Character[] Ca = new Character[]{};

        // TODO use proj text
//        setAt( String.class, ba.getClass(), String2byteArr );
        // TODO use proj text
//        setAt( ba.getClass(), String.class, byteArr2String );

        // TODO use proj text
//        setAt( Ba.getClass(), String.class, ByteArr2String );
        // TODO use proj text
//        setAt( String.class, Ba.getClass(), String2ByteArr );

        set(ca.getClass(), String.class, charArr2String);
        set(Ca.getClass(), String.class, CharArr2String);

        set(String.class, ca.getClass(), String2charArr);
        set(String.class, Ca.getClass(), String2CharArr);

        set(Date.class, java.sql.Date.class, Date2SqlDate);
        set(Date.class, java.sql.Time.class, Date2SqlTime);
        set(Date.class, java.sql.Timestamp.class, Date2SqlTimestamp);
        set(java.sql.Date.class, Date.class, SqlDate2Date);
        set(java.sql.Time.class, Date.class, SqlTime2Date);
//        set(java.sql.Timestamp.class, Date.class, SqlTimestamp2Date);

//        set(Date.class, String.class, Date2String);
//        set(String.class, Date.class, String2Date);

//        setAt( Clob.class, String.class, Clob2String );
//        setAt( NClob.class, String.class, NClob2String );
//
//        setAt( java.net.URL.class, String.class, URL2String );
//        setAt( String.class, java.net.URL.class, String2URL );
//
//        setAt( java.net.URI.class, String.class, URI2String );
//        setAt( String.class, java.net.URI.class, String2URI );
//
//        setAt( java.io.File.class, String.class, JavaIoFile2String );
//        setAt( String.class, java.io.File.class, String2JavaIoFile );
//
//        setAt( java.io.File.class, java.net.URI.class, JavaIoFile2URI );
//        setAt( java.io.File.class, java.net.URL.class, JavaIoFile2URL );

        // TODO export to spi
//        setAt( xyz.cofe.fs.File.class, String.class, XyzCofeFile2String );
//        setAt( String.class, xyz.cofe.fs.File.class, String2XyzCofeFile );

//        setAt( Charset.class, String.class, Charset2String );
//        setAt( String.class, Charset.class, String2Charset );
//
//        setAt( xyz.cofe.io.File.class, String.class, CofeIOFile2String );
//        setAt( String.class, xyz.cofe.io.File.class, String2CofeIOFile );
//
//        setAt( xyz.cofe.io.File.class, java.nio.file.Path.class, CofeIOFile2Path );
//        setAt( java.nio.file.Path.class, xyz.cofe.io.File.class, Path2CofeIOFile );
//
//        setAt( xyz.cofe.io.File.class, java.io.File.class, CofeIOFile2File );
//        setAt( java.io.File.class, xyz.cofe.io.File.class, JavaFile2CofeIOFile );
    }

//    /**
//     * Клонирование объекта
//     *
//     * @return клон
//     */
//    @Override
//    public BaseCastGraph clone() {
//        return new BaseCastGraph(this);
//    }

    public SimpleDateFormat[] getDateFormats() {
        synchronized (this) {
            if (dateFormats == null) {
                dateFormats = new SimpleDateFormat[]{
                        new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ"),
                        new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSSZ"),
                        new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS"),
                        new SimpleDateFormat("yyy-MM-dd HH:mm:ss"),
                        new SimpleDateFormat("yyy-MM-dd HH:mm"),
                        new SimpleDateFormat("yyy-MM-dd"),
                };
            }
            return dateFormats;
        }
    }
    //</editor-fold>
    //</editor-fold>

    public void setDateFormat(SimpleDateFormat[] df) {
        synchronized (this) {
            this.dateFormats = df;
        }
    }

    public <X,Y> List<Function<X, Y>> convertors(Class<? extends X> cfrom, Class<? extends Y> cto) {

        List<Class> roots = roots(cfrom, false, true, true, false);
        if (roots.isEmpty())
            return List.of();

        List<Function<X, Y>> convertors = new FasterList(roots.size());
        int i = -1;
        for( Class cf : roots ){
            Path<Class, Function> path = findPath(cf, cto);
            if( path!=null ){
                i++;
                convertors.add(Converter.the(path));
            }
        }

        return convertors;
    }

    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="NClob 2 String">
//    public static final Convertor NClob2String = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            NClob clob = ((NClob)from);
//            return NClobToString.getStringOf(clob);
//        }
//        @Override public String toString(){ return "NClob2String"; }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="URL - String">
//    //<editor-fold defaultstate="collapsed" desc="URL2String">
//    public static final Convertor URL2String = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            java.net.URL url = ((java.net.URL)from);
//            return url.toString();
//        }
//        @Override public String toString(){ return "URL2String"; }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="String2URL">
//    public static final Convertor String2URL = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            String url = ((String)from);
//            try {
//                return new java.net.URL(url);
//            } catch (MalformedURLException ex) {
//                throw new ClassCastException(
//                        "can't cast from "+url+" to java.net.URL\n"+
//                                ex.getMessage()
//                );
//            }
//        }
//        @Override public String toString(){ return "String2URL"; }
//    };
//    //</editor-fold>
////</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="URI - String">
//    //<editor-fold defaultstate="collapsed" desc="URI2String">
//    public static final Convertor URI2String = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            java.net.URI uri = ((java.net.URI)from);
//            return uri.toString();
//        }
//        @Override public String toString(){ return "URI2String"; }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="String2URI">
//    public static final Convertor String2URI = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            String url = ((String)from);
//            try {
//                return new java.net.URI(url);
//            } catch (URISyntaxException ex) {
//                throw new ClassCastException(
//                        "can't cast from "+url+" to java.net.URL\n"+
//                                ex.getMessage()
//                );
//            }
//        }
//        @Override public String toString(){ return "String2URI"; }
//    };
//    //</editor-fold>
////</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="java.io.File - String">
//    //<editor-fold defaultstate="collapsed" desc="JavaIoFile2String">
//    public static final Convertor JavaIoFile2String = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            java.io.File file = ((java.io.File)from);
//            return file.toString();
//        }
//        @Override public String toString(){ return "JavaIoFile2String"; }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="String2JavaIoFile">
//    public static final Convertor String2JavaIoFile = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            String url = ((String)from);
//            return new java.io.File(url);
//        }
//        @Override public String toString(){ return "String2JavaIoFile"; }
//    };
//    //</editor-fold>
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="xyz.cofe.io.File String">
//    public static final Convertor CofeIOFile2String = new MutableWeightedCaster(){
//        @Override
//        public Object convert(Object from) {
//            xyz.cofe.io.File file = (xyz.cofe.io.File)from;
//            return file.toString();
//        }
//    };
//
//    public static final Convertor CofeIOFile2Path = new MutableWeightedCaster(){
//        @Override
//        public Object convert(Object from) {
//            xyz.cofe.io.File file = (xyz.cofe.io.File)from;
//            return file.path;
//        }
//    };
//
//    public static final Convertor CofeIOFile2File = new MutableWeightedCaster(){
//        @Override
//        public Object convert(Object from) {
//            xyz.cofe.io.File file = (xyz.cofe.io.File)from;
//            return file.toFile();
//        }
//    };
//
//    public static final Convertor String2CofeIOFile = new MutableWeightedCaster(){
//        @Override
//        public Object convert(Object from) {
//            String str = (String)from;
//            xyz.cofe.io.File file = new xyz.cofe.io.File(str);
//            return file;
//        }
//    };
//
//    public static final Convertor Path2CofeIOFile = new MutableWeightedCaster(){
//        @Override
//        public Object convert(Object from) {
//            java.nio.file.Path path = (java.nio.file.Path)from;
//            xyz.cofe.io.File file = new xyz.cofe.io.File(path);
//            return file;
//        }
//    };
//
//    public static final Convertor JavaFile2CofeIOFile = new MutableWeightedCaster(){
//        @Override
//        public Object convert(Object from) {
//            java.io.File f = (java.io.File)from;
//            xyz.cofe.io.File file = new xyz.cofe.io.File(f.toPath());
//            return file;
//        }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="JavaIoFile2URI">
//    public final Convertor JavaIoFile2URI = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            java.io.File file = ((java.io.File)from);
//            return file.toURI();
//        }
//        @Override public String toString(){ return "JavaIoFile2URI"; }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="JavaIoFile2URI">
//    public final Convertor JavaIoFile2URL = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            java.io.File file = ((java.io.File)from);
//            try {
//                return file.toURI().toURL();
//            } catch (MalformedURLException ex) {
//                throw new ClassCastException(
//                        "can't cast from "+file+" to java.net.URL\n"+
//                                ex.getMessage()
//                );
//            }
//        }
//        @Override public String toString(){ return "JavaIoFile2URL"; }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="Charset2String">
//    public final Convertor Charset2String = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            Charset str = ((Charset)from);
//            return str.name();
//        }
//        @Override public String toString(){ return "Charset2String"; }
//    };
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="String2Charset">
//    public final Convertor String2Charset = new MutableWeightedCaster() {
//        @Override
//        public Object convert(Object from) {
//            String str = ((String)from);
//            return Charset.forName(str);
//        }
//        @Override public String toString(){ return "String2Charset"; }
//    };
//    //</editor-fold>
}
