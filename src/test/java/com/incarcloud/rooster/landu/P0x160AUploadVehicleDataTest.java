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
 * 3.1.9 行为位置数据(0x160A)<br>
 *     【命令字】+【OBD 串号（设备号）】+【TripID】+【VID】+【VIN 码】+【取得检测数据时间戮】+【数据类型】++【上传数据内容】
 *
 * @author Aaric, created on 2017-06-08T15:24.
 * @since 1.0-SNAPSHOT
 */
public class P0x160AUploadVehicleDataTest {

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
        // 2.判断命令字-0x160A
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x0A == buffer.getByte(offset+1)) {
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

            // 8.数据类型
            int dataType = DataPackUtil.readByte(buffer);
            System.out.printf("dataType: %d\n", dataType);

            // 9.上传数据内容
            // 格式：【上传数据内容】::=【定位信息】::=【车速】+【当前行程行驶距离】+【经度】+【分割符】+【纬度】+【分割符】+【方向】+【分割符】+【定位时间】+【分割符】+【定位方式】
            switch (dataType) {
                case 0x01:
                    // 0x01-超速记录
                case 0x02:
                    // 0x02-急加速记录
                case 0x03:
                    // 0x03-急减速记录
                case 0x04:
                    // 0x04-急转弯记录

                    // 10.1 提示信息(无用的逻辑，纯粹显示说明)
                    switch (dataType) {
                        case 0x01:
                            System.out.println("## 超速记录");
                            break;
                        case 0x02:
                            System.out.println("## 急加速记录");
                            break;
                        case 0x03:
                            System.out.println("## 急减速记录");
                            break;
                        case 0x04:
                            System.out.println("## 急转弯记录");
                            break;
                    }

                    // 10.2 定位信息
                    // 10.1 车速
                    String speed = DataPackUtil.readString(buffer);
                    System.out.printf("speed: %s\n", speed);

                    // 10.2 当前行程行驶距离
                    String travelDistance = DataPackUtil.readString(buffer);
                    System.out.printf("travelDistance: %s\n", travelDistance);

                    // 10.3 经度
                    String longitude = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("longitude: %s\n", longitude);

                    // 10.4 纬度
                    String latitude = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("latitude: %s\n", latitude);

                    // 10.5 方向
                    String direction = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("direction: %s\n", direction);

                    // 10.6 定位时间
                    String locationDate = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("locationDate: %s\n", locationDate);

                    // 10.7 定位方式
                    String locationMode = DataPackUtil.readString(buffer);
                    System.out.printf("locationMode: %s\n", locationMode);
                    break;
                case 0xF0:
                    // 0xF0-拔下OBD记录
                    // 【上传数据内容】::=【定位信息】，详见前面说明
                    System.out.println("## 拔下OBD记录");
                    break;
                default:
                    System.out.println("## 无效");
            }
        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(8, buffer.readerIndex());
    }
}
