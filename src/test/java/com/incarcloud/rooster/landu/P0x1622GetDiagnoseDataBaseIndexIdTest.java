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
 * 3.2.3 根据索引 ID 取得相应的检测数据(0x1622)<br>
 *     格式：【命令字】+【远程诊断仪串号（设备号）】+【TripID】+【VID】+【VIN 码】+【项数】+【【【ID】+【检测结果】】+…】<br>
 * <i>注：未找到测试数据，按照文档格式解析数据</i>
 *
 * @author Aaric, created on 2017-06-09T09:38.
 * @since 1.0-SNAPSHOT
 */
public class P0x1622GetDiagnoseDataBaseIndexIdTest {

    private ByteBuf buffer;

    @Before
    public void begin() throws IOException {
        // 准备数据
        String testFile = System.getProperty("user.dir") + "/src/test/resources/landu-dats/1601-发动机点火.dat";
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
        // 2.判断命令字-0x1622
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x22 == buffer.getByte(offset+1)) {
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

            // 7.项数
            int testingTotal = LanduDataPackUtil.readWord(buffer);
            System.out.printf("testingTotal: %s\n", testingTotal);

            // 8.检测结果
            // 格式：【【ID】+【检测结果】】+…】
            int testingId;
            String testingValue;
            for (int i = 0; i < testingTotal; i++) {
                testingId = LanduDataPackUtil.readWord(buffer);
                testingValue = LanduDataPackUtil.readString(buffer);
                System.out.printf("%d-(testingId: %s, testingValue: %s)\n", (i+1), ByteBufUtil.hexDump(new byte[]{(byte) ((testingId >> 8) & 0xFF), (byte) (testingId & 0xFF)}), testingValue);
            }
        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(8, buffer.readerIndex());
    }
}
