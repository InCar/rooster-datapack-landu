package com.incarcloud.rooster.landu;

import com.incarcloud.rooster.datapack.util.DataPackUtil;
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
 * 3.1.3 从服务器取得参数(0x1603)<br>
 *     格式：【数据头信息】+【车辆信息】+【模块信息】+【执行动作初值】<br>
 * <i>注意：回复数据比较复杂，待解决</i>
 *
 * @author Aaric, created on 2017-06-08T11:02.
 * @since 1.0-SNAPSHOT
 */
public class P0x1603ParametersDerivedFromTheServerTest {

    private ByteBuf buffer;

    @Before
    public void begin() throws IOException {
        // 准备数据
        String testFile = System.getProperty("user.dir") + "/src/test/resources/landu-dats/1603-参数数据.dat";
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
        // 2.判断命令字-0x1603
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x03 == buffer.getByte(offset+1)) {
            // 丢弃命令字
            buffer.skipBytes(2);

            // 3.OBD 串号（设备号）
            String obdCode = DataPackUtil.readString(buffer);
            System.out.printf("obdCode: %s\n", obdCode);

            // 4.TripID
            int tripId = DataPackUtil.readDWord(buffer);
            System.out.printf("tripId: %d\n", tripId);

            // 5.VID
            String vid = DataPackUtil.readString(buffer);
            System.out.printf("vid: %s\n", vid);

            // 6.VIN码
            String vin = DataPackUtil.readString(buffer);
            System.out.printf("vin: %s\n", vin);

            // 7.模块信息
            // 格式：【模块信息】::=【硬件版本号】+【固件版本号】+【软件版本号】+【诊断程序类型】
            // 7.1 硬件版本号
            String hardwareVersionNumber = DataPackUtil.readString(buffer);
            System.out.printf("hardwareVersionNumber: %s\n", hardwareVersionNumber);

            // 7.2 固件版本号
            String firmwareVersionNumber = DataPackUtil.readString(buffer);
            System.out.printf("firmwareVersionNumber: %s\n", firmwareVersionNumber);

            // 7.3 软件版本号
            String softwareVersionNumber = DataPackUtil.readString(buffer);
            System.out.printf("softwareVersionNumber: %s\n", softwareVersionNumber);

            // 7.4 诊断程序类型
            int diagnosticProgramType = DataPackUtil.readByte(buffer);
            System.out.printf("diagnosticProgramType: %s\n", diagnosticProgramType);

            // 8.执行动作初值
            // 格式：【执行动作初值】::=【恢复出厂设置序号】
            int restoreFactorySettingsSerialNumber = DataPackUtil.readByte(buffer);
            System.out.printf("restoreFactorySettingsSerialNumber: %s\n", restoreFactorySettingsSerialNumber);
        }

        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(2, buffer.writerIndex() - buffer.readerIndex());
    }
}
