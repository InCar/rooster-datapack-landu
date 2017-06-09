package com.incarcloud.rooster.datapack.util;

import io.netty.buffer.ByteBufUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * DataPackUtilTest
 *
 * @author Aaric, created on 2017-06-09T11:05.
 * @since 1.0-SNAPSHOT
 */
public class DataPackUtilTest {

    @Test
    public void testCommandBytes() throws Exception {
        byte[] bytes = DataPackUtil.commandBytes(new byte[]{0x16, 0x21});
        Assert.assertEquals("4c441621", ByteBufUtil.hexDump(bytes));
    }
}
