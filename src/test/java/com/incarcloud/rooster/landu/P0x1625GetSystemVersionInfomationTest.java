package com.incarcloud.rooster.landu;

import com.incarcloud.rooster.util.LanduDataPackUtil;
import io.netty.buffer.ByteBuf;
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
 * 3.2.6 取得系统版本信息(0x1625)<br>
 *     格式：【命令字】+【远程诊断仪串号（设备号）】+【TripID】+【VID】+【VIN 码】+【硬件版本号】+【固件版本号】+【软件版本号】+【软件类别 ID】<br>
 * <i>注：未找到测试数据，按照文档格式解析数据</i><br>
 * <i>注：V3.12 之后版本不再支持</i>
 *
 * @author Aaric, created on 2017-06-09T10:23.
 * @since 1.0-SNAPSHOT
 */
public class P0x1625GetSystemVersionInfomationTest {

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
        // 2.判断命令字-0x1625
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x25 == buffer.getByte(offset+1)) {
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

            // 格式：【硬件版本号】+【固件版本号】+【软件版本号】+【软件类别 ID】
            // 7.硬件版本号
            String hardwareVersionNumber = LanduDataPackUtil.readString(buffer);
            System.out.printf("hardwareVersionNumber: %s\n", hardwareVersionNumber);

            // 8.固件版本号
            String firmwareVersionNumber = LanduDataPackUtil.readString(buffer);
            System.out.printf("firmwareVersionNumber: %s\n", firmwareVersionNumber);

            // 9.软件版本号
            String softwareVersionNumber = LanduDataPackUtil.readString(buffer);
            System.out.printf("softwareVersionNumber: %s\n", softwareVersionNumber);

            // 10.软件类别 ID
            // 注：【软件类别 ID】未说明
            //int softwareTypeId = LanduDataPackUtil.readByte(buffer);
            //System.out.printf("softwareTypeId: %s\n", softwareTypeId);
        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(8, buffer.readerIndex());
    }
}
