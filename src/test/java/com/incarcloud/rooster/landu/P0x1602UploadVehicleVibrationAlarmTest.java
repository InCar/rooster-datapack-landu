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
 * 3.1.2 上传车辆报警(0x1602)<br>
 *     格式：【命令字】+【OBD 串号（设备号）】+【TripID】+【VID】+【VIN 码】+【取得检测数据时间戮】+【报警类型】+【定位信息】+[报警数据]
 *
 * @author Aaric, created on 2017-06-07T17:19.
 * @since 1.0-SNAPSHOT
 */
public class P0x1602UploadVehicleVibrationAlarmTest {

    private ByteBuf buffer;

    @Before
    public void begin() throws IOException {
        // 准备数据
        String testFile = System.getProperty("user.dir") + "/src/test/resources/landu-dats/1602-拔下OBD报警.dat";
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
        // 2.判断命令字-0x1602
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x02 == buffer.getByte(offset+1)) {
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

            // 7.取得检测数据时间戳
            String receiveDate = DataPackUtil.readString(buffer);
            System.out.printf("receiveDate: %s\n", receiveDate);

            // 8.报警类型
            int alarmType = DataPackUtil.readByte(buffer);
            System.out.printf("alarmType: %d\n", alarmType);

            // 9.定位信息
            // 数据格式：【定位信息】::=【车速】+【当前行程行驶距离】+【经度】+【分割符】+【纬度】+【分割符】+【方向】+【分割符】+【定位时间】+【分割符】+【定位方式】
            // 9.1 车速
            String speed = DataPackUtil.readString(buffer);
            System.out.printf("speed: %s\n", speed);

            // 92 当前行程行驶距离
            String travelDistance = DataPackUtil.readString(buffer);
            System.out.printf("travelDistance: %s\n", travelDistance);

            // 9.3 经度
            String longitude = DataPackUtil.readStringEmic(buffer);
            System.out.printf("longitude: %s\n", longitude);

            // 9.4 纬度
            String latitude = DataPackUtil.readStringEmic(buffer);
            System.out.printf("latitude: %s\n", latitude);

            // 9.5 方向
            String direction = DataPackUtil.readStringEmic(buffer);
            System.out.printf("direction: %s\n", direction);

            // 9.6 定位时间
            String locationDate = DataPackUtil.readStringEmic(buffer);
            System.out.printf("locationDate: %s\n", locationDate);

            // 9.7 定位方式
            String locationMode = DataPackUtil.readString(buffer);
            System.out.printf("locationMode: %s\n", locationMode);

            // 10.报警数据
            switch (alarmType) {
                case 0x01:
                    // 10.1 0x01-新故障码报警
                    // 注：未找到测试数据，按照文档格式解析数据
                    System.out.println("## 0x01-新故障码报警");
                    // 10.1.1 故障码个数
                    int troubleCodeTotal = DataPackUtil.readByte(buffer);
                    System.out.printf("troubleCodeTotal: %s\n", troubleCodeTotal);

                    // 10.1.2 故障信息
                    // 格式：【【故障码】+【故障码属性】+【故障码描述】】+……】
                    String troubleCode, troubleAttr, troubleDesc;
                    for(int i = 0; i < troubleCodeTotal; i++) {
                        troubleCode = DataPackUtil.readString(buffer);
                        troubleAttr = DataPackUtil.readString(buffer);
                        troubleDesc = DataPackUtil.readString(buffer);
                        System.out.printf("%d-(troubleCode: %s, troubleAttr: %s, troubleDesc: %s)\n", (i+1), troubleCode, troubleAttr, troubleDesc);
                    }
                    break;
                case 0x02:
                    // 10.2 0x02-碰撞报警
                    System.out.println("## 0x02-碰撞报警");
                    // 【报警数据】：：无
                    break;
                case 0x03:
                    // 10.3 0x03-防盗报警
                    System.out.println("## 0x03-防盗报警");
                    // 【报警数据】：：无
                    break;
                case 0x04:
                    // 10.4 0x04-水温报警
                    // 注：未找到测试数据，按照文档格式解析数据
                    System.out.println("## 0x04-水温报警");
                    String waterTemperature = DataPackUtil.readString(buffer);
                    System.out.printf("waterTemperature: %s\n", waterTemperature);
                    break;
                case 0x05:
                    // 10.5 0x05-充电电压报警（小于13.1伏）
                    System.out.println("## 0x05-充电电压报警（小于13.1伏）");
                    // 10.5.1 充电电压值
                    String batteryVoltage = DataPackUtil.readString(buffer);
                    System.out.printf("batteryVoltage: %s\n", batteryVoltage);
                    break;
                case 0xF0:
                    // 10.6 0xF0-拔下OBD报警
                    System.out.println("## 0xF0-拔下OBD报警");
                    // 【报警数据】：：无
                    break;
                default:
                    System.out.println("## 无效");
            }

        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(22, buffer.writerIndex() - buffer.readerIndex());
    }
}
