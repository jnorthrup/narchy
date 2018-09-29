/*
 * The MIT License
 *
 * Copyright 2014 Kamnev Georgiy (nt.gocha@gmail.com).
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
package jcog.reflect.simple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

/**
 * Конвертор из чисел в строку и обратно
 *
 * @author gocha
 */
public class NumberConvertor implements Function<String,Object>  {
    private final NumberType type;

    public NumberConvertor(NumberType type) {
        if (type == null) {
            throw new IllegalArgumentException("type == null");
        }
        this.type = type;
    }

    /* (non-javadoc)
     * @see org.gocha.text.simpletypes.ToStringConvertor#convertToString
     */

    public static final Function<Object,String> toString = (Object srcData) -> {
        if (srcData == null) {
            throw new IllegalArgumentException("srcData == null");
        }
        return srcData.toString();
    };

    @Override
    public Object apply(String value) {
        if (value == null) {
            return null;
        }

        Object result = null;

        try {
            switch (type) {
                case BYTE:
                    result = Byte.parseByte(value);
                    break;
                case DOUBLE:
                    result = Double.parseDouble(value);
                    break;
                case FLOAT:
                    result = Float.parseFloat(value);
                    break;
                case INTEGER:
                    result = Integer.parseInt(value);
                    break;
                case LONG:
                    result = Long.parseLong(value);
                    break;
                case SHORT:
                    result = Short.parseShort(value);
                    break;
                case BIGDECIMAL:
                    result = new BigDecimal(value);
                    break;
                case BIGINTEGER:
                    result = new BigInteger(value);
                    break;
            }
        } catch (NumberFormatException e) {
            result = null;
        }

        return result;
    }
}
