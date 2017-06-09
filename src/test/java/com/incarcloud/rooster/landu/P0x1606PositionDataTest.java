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
 * 3.1.5 位置数据(0x1606)<br>
 *     格式：【命令字】+【OBD 串号（设备号）】+【TripID】+【VID】+【VIN 码】+【数据内容】
 *
 * @author Aaric, created on 2017-06-08T13:48.
 * @since 1.0-SNAPSHOT
 */
public class P0x1606PositionDataTest {

    private ByteBuf buffer;

    @Before
    public void begin() throws IOException {
        // 准备数据
        String testFile = System.getProperty("user.dir") + "/src/test/resources/landu-dats/1606-位置数据.dat";
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
        // 2.判断命令字-0x1606
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x06 == buffer.getByte(offset+1)) {
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

            // 7.数据内容
            // 【数据内容】：：=【定位数据个数】+【【定位信息】+…】
            // 7.1 定位数据个数
            int locationDataTotal = DataPackUtil.readWord(buffer);
            System.out.printf("locationDataTotal: %d\n", locationDataTotal);
            // 7.2 定位信息
            // 数据格式：【定位信息】::=【车速】+【当前行程行驶距离】+【经度】+【分割符】+【纬度】+【分割符】+【方向】+【分割符】+【定位时间】+【分割符】+【定位方式】
            String speed, travelDistance;
            String longitude, latitude, direction;
            String locationDate, locationMode;
            for (int i = 0; i < locationDataTotal; i++) {
                System.out.printf("## %d\n", (i+1));
                // 7.2.1 车速
                speed = DataPackUtil.readString(buffer);
                System.out.printf("speed: %s\n", speed);

                // 7.2.2 当前行程行驶距离
                travelDistance = DataPackUtil.readString(buffer);
                System.out.printf("travelDistance: %s\n", travelDistance);

                // 7.2.3 经度
                longitude = DataPackUtil.readStringEmic(buffer);
                System.out.printf("longitude: %s\n", longitude);

                // 7.2.4 纬度
                latitude = DataPackUtil.readStringEmic(buffer);
                System.out.printf("latitude: %s\n", latitude);

                // 7.2.5 方向
                direction = DataPackUtil.readStringEmic(buffer);
                System.out.printf("direction: %s\n", direction);

                // 7.2.6 定位时间
                locationDate = DataPackUtil.readStringEmic(buffer);
                System.out.printf("locationDate: %s\n", locationDate);

                // 7.2.7 定位方式
                locationMode = DataPackUtil.readString(buffer);
                System.out.printf("locationMode: %s\n", locationMode);
            }
        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(2, buffer.writerIndex() - buffer.readerIndex());
    }
}
