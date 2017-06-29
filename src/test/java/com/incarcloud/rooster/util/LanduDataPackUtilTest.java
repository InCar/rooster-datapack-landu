package com.incarcloud.rooster.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * LanduDataPackUtilTest
 *
 * @author Aaric, created on 2017-06-29T13:34.
 * @since 1.0-SNAPSHOT
 */
public class LanduDataPackUtilTest {

    private ByteBuf buffer;

    @Before
    public void begin() {
        byte[] bytes = new byte[]{(byte) 0xFE, 'A', 'B', 'C', 0x00, 'a', 'b', 'c', 0x00};
        buffer = Unpooled.wrappedBuffer(bytes);
    }

    @After
    public void end() {
        buffer.release();
    }

    @Test
    public void testReadWord() {
        Assert.assertEquals(65089, LanduDataPackUtil.readWord(buffer));
    }

    @Test
    public void testReadByte() {
        Assert.assertEquals(254, LanduDataPackUtil.readByte(buffer));
    }

    @Test
    public void testReadShort() {
        Assert.assertEquals(-447, LanduDataPackUtil.readShort(buffer));
    }

    @Test
    public void testReadLong() {
        Assert.assertEquals(-29277629, LanduDataPackUtil.readLong(buffer));
    }

    @Test
    public void testReadDWord() {
        Assert.assertEquals(4265689667L, LanduDataPackUtil.readDWord(buffer));
    }
}
