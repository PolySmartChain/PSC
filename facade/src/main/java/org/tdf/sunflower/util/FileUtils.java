package org.tdf.sunflower.util;

import lombok.SneakyThrows;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;

public class FileUtils {
    /**
     * Represents the end-of-file (or stream).
     *
     * @since 2.5 (made public)
     */
    public static final int EOF = -1;
    /**
     * The default buffer size ({@value}) to use for
     * {@link #copyLarge(InputStream, OutputStream)}.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static ClassLoader loader = FileUtils.class.getClassLoader();

    public static void setClassLoader(ClassLoader loader) {
        FileUtils.loader = loader;
    }

    /**
     * @param pathOrUrl path or url priority url > file system > class path
     * @return input stream
     */
    @SneakyThrows
    public static InputStream getInputStream(String pathOrUrl) {
        try {
            URL url = new URL(pathOrUrl);
            return url.openStream();
        } catch (Exception ignored) {

        }
        File f = new File(pathOrUrl);
        if (f.exists()) {
            return new FileInputStream(f);
        }
        return loader.getResourceAsStream(pathOrUrl);
    }

    public static boolean recursiveDelete(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            //check if the file is a directory
            if (file.isDirectory()) {
                if ((file.list()).length > 0) {
                    for (String s : file.list()) {
                        //call deletion of file individually
                        recursiveDelete(fileName + System.getProperty("file.separator") + s);
                    }
                }
            }

            file.setWritable(true);
            boolean result = file.delete();
            return result;
        } else {
            return false;
        }
    }

    @SneakyThrows
    public static void write(byte[] data, String path) {
        File f = Paths.get(path).toFile();
        OutputStream os = new FileOutputStream(f);
        copy(new ByteArrayInputStream(data), os);
        try {
            os.close();
        } catch (Exception ignored) {
        }
    }

    // copy from InputStream
    //-----------------------------------------------------------------------

    /**
     * Copies bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * Large streams (over 2GB) will return a bytes copied value of
     * <code>-1</code> after the copy has completed since the correct
     * number of bytes cannot be returned as an int. For large streams
     * use the <code>copyLarge(InputStream, OutputStream)</code> method.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    public static int copy(final InputStream input, final OutputStream output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }


    /**
     * Copies bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.3
     */
    public static long copyLarge(final InputStream input, final OutputStream output)
        throws IOException {

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}
