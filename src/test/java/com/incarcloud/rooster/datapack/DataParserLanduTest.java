package com.incarcloud.rooster.datapack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Test DataParserLANDU class
 *
 * @author Aaric
 * @since 2.0
 */
public class DataParserLanduTest {

    private static Logger s_logger = LoggerFactory.getLogger(DataParserLanduTest.class);

    @Test
    public void testReverseByteArray() throws Exception {
        byte tmp;
        byte[] bytes = {(byte) 0xFF,(byte) 0xB4};
        for(int i = 0; i < bytes.length; i++) {
            tmp = bytes[i];
            bytes[i] = (byte) ~tmp;
        }
        /*byte[] bytes = {(byte) 0xFF,(byte) 0xB4};
        bytes = DataParserLANDU.reverseByteArray(bytes);*/
        Assert.assertEquals("004b", ByteBufUtil.hexDump(bytes));
    }

    @Test
    public void testByteArray2ToInt() throws Exception {
        byte[] bytes = {0x00, 0x00, 0x00, 0x4B};
        ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        Assert.assertEquals(75, buffer.getInt(0));
        buffer.release();
        /*byte[] bytes = {0x00, 0x4B};
        Assert.assertEquals(75, DataParserLANDU.byteArray2ToInt(bytes));*/
    }

    @Test
    public void testExtract() throws Exception {
        // 测试数据-从服务器取得参数
        byte[] data = {
                (byte) 0xAA, 0x55, //数据包标志(AA55)-2字节
                0x00, 0x4B, //数据包长度-2字节
                (byte) 0xFF,(byte) 0xB4, //数据包长度校验(数据包长度取反)-2字节
                0x00, //数据包ID-1字节
                0x05, //保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)-1字节
                0x16, 0x03, 0x49, 0x4E, 0x43, 0x41, 0x52, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x00, 0x00, 0x00, 0x00, (byte) 0xCC, 0x35, 0x00, 0x4C, 0x53, 0x47, 0x50, 0x42, 0x36, 0x34, 0x55, 0x35, 0x42, 0x53, 0x32, 0x34, 0x30, 0x34, 0x36, 0x39, 0x00, 0x56, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x30, 0x30, 0x00, 0x56, 0x30, 0x2E, 0x30, 0x30, 0x2E, 0x30, 0x30, 0x00, 0x56, 0x33, 0x2E, 0x31, 0x34, 0x2E, 0x30, 0x32, 0x00, (byte) 0xFF, 0x00,
                0x0F, (byte) 0xDA //校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)-2字节
        };
        ByteBuf buffer = Unpooled.wrappedBuffer(data);
        IDataParser parser = new DataParserLandu();
        List<DataPack> dataPackList = parser.extract(buffer);
        Assert.assertEquals(1, dataPackList.size());

        for(DataPack dataPack: dataPackList) {
            dataPack.freeBuf();
        }
        buffer.release();
        Assert.assertEquals(0, buffer.refCnt());
    }
}
