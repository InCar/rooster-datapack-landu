package com.incarcloud.rooster.landu;

import com.incarcloud.rooster.util.LanduDataPackUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 3.1.6 冻结帧数据(0x1607)<br>
 *     格式：【命令字】+【OBD 串号（设备号）】+【TripID】+【VID】+【VIN 码】+【取得检测数据时间戮】+【数据内容】
 *
 * @author Aaric, created on 2017-06-08T14:11.
 * @since 1.0-SNAPSHOT
 */
public class P0x1607FreezeFrameTest {

    private ByteBuf buffer;

    @Before
    public void begin() throws IOException {
        // 准备数据
        String testFile = System.getProperty("user.dir") + "/src/test/resources/landu-dats/1607-冻结帧数据.dat";
        FileInputStream fio = new FileInputStream(new File(testFile));
        byte[] array = new byte[fio.available()];
        fio.read(array, 0, fio.available());
        fio.close();
        // 测试数据
        buffer = Unpooled.wrappedBuffer(array);
    }

    @After
    public void end() {
        ReferenceCountUtil.release(buffer);
    }

    @Test
    public void testParse() throws Exception {
        /* 假定无格式问题数据，开始解析数据内容 */
        System.out.println("-------------------------begin");
        int offset;
        // 1.丢弃协议头
        buffer.skipBytes(8);
        // 2.判断命令字-0x1607
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x07 == buffer.getByte(offset+1)) {
            // 丢弃命令字
            buffer.skipBytes(2);

            // 3.OBD 串号（设备号）
            String obdCode = LanduDataPackUtil.readString(buffer);
            System.out.printf("obdCode: %s\n", obdCode);

            // 4.TripID
            long tripId = LanduDataPackUtil.readDWord(buffer);
            System.out.printf("tripId: %d\n", tripId);

            // 5.VID
            String vid = LanduDataPackUtil.readString(buffer);
            System.out.printf("vid: %s\n", vid);

            // 6.VIN码
            String vin = LanduDataPackUtil.readString(buffer);
            System.out.printf("vin: %s\n", vin);

            // 7.取得检测数据时间戳
            String receiveDate = LanduDataPackUtil.readString(buffer);
            System.out.printf("receiveDate: %s\n", receiveDate);

            // 8.数据内容
            // 格式：【数据内容】::=【冻结帧个数】+【冻结帧内容】
            // 8.1 冻结帧个数
            int freezeFrameTotal = LanduDataPackUtil.readWord(buffer);
            System.out.printf("freezeFrameTotal: %s\n", freezeFrameTotal);

            // 8.2 冻结帧内容
            // 格式：【冻结帧内容】::=【【【ID】+【数据流值】】+……】
            int freezeFrameId;
            String freezeFrameContent;
            for (int i = 0; i < freezeFrameTotal; i++) {
                freezeFrameId = LanduDataPackUtil.readWord(buffer);
                freezeFrameContent = LanduDataPackUtil.readString(buffer);
                System.out.printf("%d-(freezeFrameId: 0x%s, freezeFrameContent: %s)\n", (i+1), ByteBufUtil.hexDump(new byte[]{(byte) ((freezeFrameId >> 8) & 0xFF), (byte) (freezeFrameId & 0xFF)}), freezeFrameContent);
            }
        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(2, buffer.writerIndex() - buffer.readerIndex());
    }
}
