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
package jcog.reflect;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Делегат множества.
 *
 * @author gocha
 */
public class SetWrapper<E> implements Set<E> {
    protected Set<E> delegate;

    /**
     * Конструктор
     *
     * @param set исходное множество
     */
    public SetWrapper(Set<E> set) {
        if (set == null) {
            throw new IllegalArgumentException("setAt == null");
        }
        this.delegate = set;
    }

    /**
     * Возвращает исходное множество, на которое происходят делегация вызывов
     *
     * @return Исходное множество
     */
    public Set<E> getWrappedSet() {
        return delegate;
    }

    /* (non-Javadoc) @see Set */
    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    /* (non-Javadoc) @see Set */
    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    /* (non-Javadoc) @see Set */
    @Override
    public int size() {
        return delegate.size();
    }

    /* (non-Javadoc) @see Set */
    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    /* (non-Javadoc) @see Set */
    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    /* (non-Javadoc) @see Set */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    /* (non-Javadoc) @see Set */
    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    /* (non-Javadoc) @see Set */
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /* (non-Javadoc) @see Set */
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    /* (non-Javadoc) @see Set */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    /* (non-Javadoc) @see Set */
    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    /* (non-Javadoc) @see Set */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    /* (non-Javadoc) @see Set */
    @Override
    public void clear() {
        delegate.clear();
    }

    /* (non-Javadoc) @see Set */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    /* (non-Javadoc) @see Set */
    @Override
    public boolean add(E e) {
        return delegate.add(e);
    }
}
