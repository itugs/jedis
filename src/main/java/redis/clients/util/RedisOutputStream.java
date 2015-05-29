package redis.clients.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The class implements a buffered output stream without synchronization There are also special
 * operations like in-place string encoding. This stream fully ignore mark/reset and should not be
 * used outside Jedis
 */
public final class RedisOutputStream extends FilterOutputStream {
  protected final byte buf[];

  protected int count;

  public RedisOutputStream(final OutputStream out) {
    this(out, 8192);
  }

  public RedisOutputStream(final OutputStream out, final int size) {
    super(out);
    if (size <= 0) {
      throw new IllegalArgumentException("Buffer size <= 0");
    }
    buf = new byte[size];
  }

  private void ensureBuffer(int size) throws IOException {
    if (size >= buf.length - count) {
      flushBuffer();
    }
  }

  private void flushBuffer() throws IOException {
    if (count > 0) {
      out.write(buf, 0, count);
      count = 0;
    }
  }

  public void write(final byte b) throws IOException {
	ensureBuffer(1);
    buf[count++] = b;
  }

  public void write(final byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  public void write(final byte b[], final int off, final int len) throws IOException {
    if (len >= buf.length) {
      flushBuffer();
      out.write(b, off, len);
    } else {
      ensureBuffer(len);
      System.arraycopy(b, off, buf, count, len);
      count += len;
    }
  }

  public void writeCrLf() throws IOException {
    ensureBuffer(2);
    buf[count++] = '\r';
    buf[count++] = '\n';
  }

  private final static int[] sizeTable = { 0, 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999,
      999999999, Integer.MAX_VALUE };

  private final static byte[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
  private final static byte[] minInt = { '-', '2', '1', '4', '7', '4', '8', '3', '6', '4', '8',
      '\r', '\n' };

  private final static int cacheMax = 100;
  private final static byte[][] intCache = new byte[cacheMax + 1][];

  /**
   * cache byte representation of int from 1 ~ 100
   */
  static {
    intCache[0] = new byte[] { '0', '\r', '\n' };

    for (int cached = 1; cached <= cacheMax; cached++) {
      int value = cached;
      int size = 0;
      while (value > sizeTable[size]) {
        size++;
      }

      intCache[cached] = new byte[size + 2];
      int charPos = size;

      for (int i = 0; i < size; i++) {
        intCache[cached][--charPos] = digits[value % 10];
        value /= 10;
      }
      intCache[cached][intCache[cached].length - 2] = '\r';
      intCache[cached][intCache[cached].length - 1] = '\n';
    }
  }

  private boolean writeCachedIntCrLf(final int value) throws IOException {
    if (value >= 0 && value <= cacheMax) {
      write(intCache[value]);
      return true;
    } else if (value == Integer.MIN_VALUE) {
      write(minInt);
      return true;
    }

    return false;
  }

  public void writeIntCrLf(int value) throws IOException {
    if (writeCachedIntCrLf(value)) {
      return;
    }

    if (value < 0) {
      write((byte) '-');
      value = -value;
    }

    int size = 0;
    while (value > sizeTable[size]) {
      size++;
    }

    ensureBuffer(size);

    int charPos = count + size;
    for (int i = 0; i < size; i++) {
      buf[--charPos] = digits[value % 10];
      value /= 10;
	}

    count += size;

    writeCrLf();
  }

  public void flush() throws IOException {
    flushBuffer();
    out.flush();
  }
}
