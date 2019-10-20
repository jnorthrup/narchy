/**
 * Copyright 2012 Kamran Zafar 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *      http:
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 */

package jcog.io.tar;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.file.Files;


/**
 * @author Kamran Zafar
 * 
 */
public class TarOutputStream extends OutputStream {
    private final OutputStream out;
    private long bytesWritten;
    private long currentFileSize;
    private TarEntry currentEntry;

    public TarOutputStream(OutputStream out) {
        this.out = out;
        bytesWritten = 0L;
        currentFileSize = 0L;
    }

    /**
     * Opens a file for (over)writing
     */
    public TarOutputStream(File fout) throws IOException {
        this.out = new BufferedOutputStream(Files.newOutputStream(fout.toPath()));
        bytesWritten = 0L;
        currentFileSize = 0L;
    }

    /**
     * Opens a file for writing or appending. 
     */
    public TarOutputStream(File fout, boolean append) throws IOException {
        @SuppressWarnings("resource")
        RandomAccessFile raf = new RandomAccessFile(fout, "rw");
        long fileSize = fout.length();
        if (append && fileSize > (long) TarConstants.EOF_BLOCK) {
            raf.seek(fileSize - (long) TarConstants.EOF_BLOCK);
        }
        out = new BufferedOutputStream(Channels.newOutputStream(raf.getChannel()));
    }

    /**
     * Appends the EOF record and closes the stream
     * 
     * @see java.io.FilterOutputStream#close()
     */
    @Override
    public void close() throws IOException {
        closeCurrentEntry();
        write( new byte[TarConstants.EOF_BLOCK] );
        out.close();
    }
    /**
     * Writes a byte to the stream and updates byte counters
     * 
     * @see java.io.FilterOutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException {
        out.write( b );
        bytesWritten += 1L;

        if (currentEntry != null) {
            currentFileSize += 1L;
        }
    }

    /**
     * Checks if the bytes being written exceed the current entry size.
     * 
     * @see java.io.FilterOutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (currentEntry != null && !currentEntry.isDirectory()) {
            if (currentEntry.getSize() < currentFileSize + (long) len) {
                throw new IOException( "The current entry[" + currentEntry.getName() + "] size["
                        + currentEntry.getSize() + "] is smaller than the bytes[" + ( currentFileSize + (long) len)
                        + "] being written." );
            }
        }

        out.write( b, off, len );

        bytesWritten = bytesWritten + (long) len;

        if (currentEntry != null) {
            currentFileSize = currentFileSize + (long) len;
        }        
    }

    /**
     * Writes the next tar entry header on the stream
     *
     * @throws IOException
     */
    public void putNextEntry(TarEntry entry) throws IOException {
        closeCurrentEntry();

        byte[] header = new byte[TarConstants.HEADER_BLOCK];
        entry.writeEntryHeader( header );

        write( header );

        currentEntry = entry;
    }

    /**
     * Closes the current tar entry
     * 
     * @throws IOException
     */
    private void closeCurrentEntry() throws IOException {
        if (currentEntry != null) {
            if (currentEntry.getSize() > currentFileSize) {
                throw new IOException( "The current entry[" + currentEntry.getName() + "] of size["
                        + currentEntry.getSize() + "] has not been fully written." );
            }

            currentEntry = null;
            currentFileSize = 0L;

            pad();
        }
    }

    /**
     * Pads the last content block
     * 
     * @throws IOException
     */
    private void pad() throws IOException {
        if (bytesWritten > 0L) {
            int extra = (int) ( bytesWritten % (long) TarConstants.DATA_BLOCK);

            if (extra > 0) {
                write( new byte[TarConstants.DATA_BLOCK - extra] );
            }
        }
    }
}
