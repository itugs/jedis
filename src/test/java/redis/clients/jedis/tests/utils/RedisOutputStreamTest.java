package redis.clients.jedis.tests.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import redis.clients.util.RedisOutputStream;

public class RedisOutputStreamTest {

  private MockOutputStream mockOutputStream = new MockOutputStream(8192);
  private RedisOutputStream cut = new RedisOutputStream(mockOutputStream);

  @Before
  public void before() throws Exception {
    mockOutputStream.clear();
  }

  @Test
  public void writeIntCrLf() throws Exception {
    assertBuffer(0, new byte[] { '0', '\r', '\n' }, 3);
    assertBuffer(123, new byte[] { '1', '2', '3', '\r', '\n' }, 5);
    assertBuffer(-321, new byte[] { '-', '3', '2', '1', '\r', '\n' }, 6);
    assertBuffer(Integer.MAX_VALUE, new byte[] { '2', '1', '4', '7', '4', '8', '3', '6', '4', '7',
        '\r', '\n' }, 12);
    assertBuffer(Integer.MIN_VALUE, new byte[] { '-', '2', '1', '4', '7', '4', '8', '3', '6', '4',
        '8', '\r', '\n' }, 13);
  }

  private void assertBuffer(int i, byte[] expect, int size) throws Exception {
    cut.writeIntCrLf(i);
    cut.flush();

    byte[] actual = Arrays.copyOfRange(mockOutputStream.buffer, 0, size);
    assertTrue("expect : " + Arrays.toString(expect) + ", actual : " + Arrays.toString(actual),
      Arrays.equals(expect, actual));
    assertEquals("expect : " + size + ", actual : " + mockOutputStream.index, size,
      mockOutputStream.index);

    mockOutputStream.clear();
  }

  static class MockOutputStream extends OutputStream {
    final byte[] buffer;
    int index;

    public MockOutputStream(int bufferSize) {
      if (bufferSize < 0) {
        throw new IllegalArgumentException();
      }

      buffer = new byte[bufferSize];
    }

    @Override
    public void write(int b) throws IOException {
      buffer[index++] = (byte) b;
    }

    public void clear() {
      Arrays.fill(buffer, (byte) 0);
      index = 0;
    }
  }
}
