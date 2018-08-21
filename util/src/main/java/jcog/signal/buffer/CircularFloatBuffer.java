/*
 * Copyright (c) 2016, Wayne Tam
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jcog.signal.buffer;

/**
 * Created by Wayne on 5/20/2015.
 * CircularFloatBuffer
 */
@SuppressWarnings("unused")
public class CircularFloatBuffer extends CircularBuffer{
    public float[] data;

    public CircularFloatBuffer() {
        super();
    }

    public CircularFloatBuffer(int capacity) {
        super(capacity);
    }

    @Override
    protected void setCapacityInternal(int capacity) {
        data = new float[capacity];
    }

    @Override
    public int getCapacityInternal() {
        return data.length;
    }

    public int write(float[] data) {
        return write(data, 0, data.length, false);
    }

    public int write(float[] data, int length) {
        return write(data, 0, length, false);
    }

    public int write(float[] data, int offset, int length, boolean blocking) {
        int len = length;
        _lock.lock();
        try {
            if (length > 0) {
                int emptySize = this.data.length - _bufferSize.get();
                while (blocking && emptySize < length) {
                    try {
                        _writeCondition.await();
                    } catch (InterruptedException e) {
                        return -1;
                    }
                    emptySize = this.data.length - _bufferSize.get();
                }
                if (emptySize > 0) {
                    if (len > emptySize)
                        len = emptySize;

                    int tmpIdx = _bufEnd + len;
                    int tmpLen;
                    if (tmpIdx > this.data.length) {
                        tmpLen = this.data.length - _bufEnd;
                        System.arraycopy(data, offset, this.data, _bufEnd, tmpLen);
                        _bufEnd = (tmpIdx) % this.data.length;
                        System.arraycopy(data, tmpLen + offset, this.data, 0, _bufEnd);
                    } else {
                        System.arraycopy(data, offset, this.data, _bufEnd, len);
                        _bufEnd = (tmpIdx) % this.data.length;
                    }
                    _bufferSize.addAndGet(len);
                    return len;
                }
            }
            return 0;
        } finally {
            _readCondition.signalAll();
            _lock.unlock();
            if (_threadPool != null)
                _threadPool.submit(_notifyListener);
        }
    }

    public float[] peekLast(float[] data) {
        return peekLast(data, data.length);
    }

    public float[] peekLast(float[] data, int len) {
        _lock.lock();
        try {
            int start = Math.max(0, this._viewPtr - len);
            int tmpIdx = start + len;
            int tmpLen;
            if (tmpIdx > this.data.length) {
                tmpLen = this.data.length - start;
                System.arraycopy(this.data, start, data, 0, tmpLen);
                start = (tmpIdx) % this.data.length;
                System.arraycopy(this.data, 0, data, tmpLen, start);
            } else {
                System.arraycopy(this.data, start, data, 0, len);
                //start = (tmpIdx) % this.data.length;
            }
            return data;
        } finally {
            _lock.unlock();
        }
    }


    /** this manipulates some cursor position variables, TODO make that optional and combine with peekLast API to select any part of the buffer */
    public int peek(float[] data, int length) {
        int len = length;
        _lock.lock();
        try {
            int remSize = _bufferSize.get() - _currOffset.get();
            if (length > 0 && remSize > 0) {
                if (len > remSize)
                    len = remSize;
                int tmpIdx = _viewPtr + len;
                int tmpLen;
                if (tmpIdx > this.data.length) {
                    tmpLen = this.data.length - _viewPtr;
                    System.arraycopy(this.data, _viewPtr, data, 0, tmpLen);
                    _viewPtr = (tmpIdx) % this.data.length;
                    System.arraycopy(this.data, 0, data, tmpLen, _viewPtr);
                } else {
                    System.arraycopy(this.data, _viewPtr, data, 0, len);
                    _viewPtr = (tmpIdx) % this.data.length;
                }
                _currOffset.addAndGet(len);
                return len;
            }
            return 0;
        } finally {
            _lock.unlock();
        }
    }

    public int readFully(float[] data, int offset, int length) {
        _lock.lock();
        try {
            if (length > 0) {
                int minSize = _minSize < 0 ? 0 : _minSize;
                while (_bufferSize.get() - minSize < length) {
                    try {
                        _readCondition.await();
                    } catch (InterruptedException e) {
                        _wasMarked = false;
                        return -1;
                    }
                }
                return read(data, offset, length, false);
            }
            return 0;
        } finally {
            _writeCondition.signalAll();
            _lock.unlock();
        }
    }

    public int read(float[] data, int length, boolean blocking) {
        return read(data, 0, length, blocking);
    }

    public int read(float[] data, int offset, int length, boolean blocking) {
        int len = length;
        _lock.lock();
        try {
            _wasMarked = false;
            if (length > 0) {
                int bs = _bufferSize.get();
                while (blocking && _minSize > -1 && bs <= _minSize) {
                    try {
                        _readCondition.await();
                    } catch (InterruptedException e) {
                        return -1;
                    }
                }
                int minSize = _minSize < 0 ? 0 : _minSize;
                if (bs > 0) {
                    if (len > bs - minSize)
                        len = bs - minSize;
                    int tmpLen;
                    BufMark m = _marks.peek();
                    if (m != null) {
                        tmpLen = calcMarkSize(m);
                        if (tmpLen <= len) {
                            _marks.poll();
                            len = tmpLen;
                            _wasMarked = true;
                        }
                    }
                    if (len > 0) {
                        int tmpIdx = _bufStart + len;
                        if (tmpIdx > this.data.length) {
                            tmpLen = this.data.length - _bufStart;
                            System.arraycopy(this.data, _bufStart, data, offset, tmpLen);
                            _bufStart = (tmpIdx) % this.data.length;
                            System.arraycopy(this.data, 0, data, offset + tmpLen, _bufStart);
                        } else {
                            System.arraycopy(this.data, _bufStart, data, offset, len);
                            _bufStart = (tmpIdx) % this.data.length;
                        }
                        if (tmpIdx < _viewPtr)
                            _currOffset.set(_viewPtr - _bufStart);
                        else {
                            _viewPtr = _bufStart;
                            _currOffset.set(0);
                        }
                        _bufferSize.addAndGet(-len);
                    }
                    return len;
                }
            }
            return 0;
        } finally {
            _writeCondition.signalAll();
            _lock.unlock();
            if (_threadPool != null)
                _threadPool.submit(_notifyListener);
        }
    }
}
