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
 * 3.2.2 取得车辆当前检测数据(0x1621)<br>
 *     格式：【命令字】+【远程诊断仪串号（设备号）】+【TripID】+【VID】+【VIN 码】+【故障分级内容】<br>
 *     格式：【故障分级内容】::= 【严重等级】+【个数】+【故障码】<br>
 * <i>本命令在 V3.12 版本之后不再支持</i>
 *
 * @author Aaric, created on 2017-06-08T16:12.
 * @since 1.0-SNAPSHOT
 */
public class P0x1621GetVehicleCheckDataTest {

    private ByteBuf buffer;

    @Before
    public void begin() throws IOException {
        // 准备数据
        String testFile = System.getProperty("user.dir") + "/src/test/resources/landu-dats/1621-系统正常.dat";
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
        // 2.判断命令字-0x1621
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x21 == buffer.getByte(offset+1)) {
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

            // 7.故障分级内容
            // 格式：【故障分级内容】::= 【严重等级】+【个数】+【故障码】
            // 7.1 严重等级
            int faultLevel = LanduDataPackUtil.readByte(buffer);
            System.out.printf("faultLevel: %s\n", faultLevel);
            switch (faultLevel) {
                case 0x00:
                    // 0x00-系统正常
                    System.out.println("## 系统正常");
                    break;
                case 0x01:
                    // 0x01-可忽略的故障
                    System.out.println("## 可忽略的故障");
                    break;
                case 0x02:
                    // 0x02-需要检修的故障
                    System.out.println("## 需要检修的故障");
                    break;
                case 0x03:
                    // 0x03-立即停车检修的故障
                    System.out.println("## 立即停车检修的故障");
                    break;
                case 0xFF:
                    // 0xFF-当前状态不适合读码
                    System.out.println("## 当前状态不适合读码");
                    break;
            }

            // 7.2 个数
            int faultCodeTotal = LanduDataPackUtil.readByte(buffer);
            System.out.printf("faultCodeTotal: %s\n", faultCodeTotal);

            // 7.3 故障码
            // 格式：【故障码】::= 【【故障码编号】+【故障码属性】+【故障码解释】+…】
            // 注：未找到测试数据，按照文档格式解析数据
            String faultCode, faultAttr, faultDesc;
            for (int i = 0; i < faultCodeTotal; i++) {
                faultCode = LanduDataPackUtil.readString(buffer);
                faultAttr = LanduDataPackUtil.readString(buffer);
                faultDesc = LanduDataPackUtil.readString(buffer);
                System.out.printf("%d-(faultCode: %s, faultAttr: %s, faultDesc: %s)\n", (i+1), faultCode, faultAttr, faultDesc);
            }
        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(2, buffer.writerIndex() - buffer.readerIndex());
    }
}
