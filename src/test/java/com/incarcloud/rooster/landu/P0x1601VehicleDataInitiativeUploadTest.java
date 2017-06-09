package com.incarcloud.rooster.landu;

import com.incarcloud.rooster.datapack.util.DataPackUtil;
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
 * 3.1.1 车辆检测数据主动上传(0x1601)<br>
 *     格式：【命令字】+【远程诊断仪串号（设备号）】+【TripID】+【VID】+【VIN 码】+【取得检测数据时间戳】+【数据属性标识】+【数据内容】
 *
 * @author Aaric, created on 2017-06-07T17:14.
 * @since 1.0-SNAPSHOT
 */
public class P0x1601VehicleDataInitiativeUploadTest {

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
        // 2.判断命令字-0x1601
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x01 == buffer.getByte(offset+1)) {
            // 丢弃命令字
            buffer.skipBytes(2);

            // 3.远程诊断仪串号（设备号）
            String obdCode = DataPackUtil.readString(buffer);
            System.out.printf("obdCode: %s\n", obdCode);

            // 4.TripID-4个字节
            int tripId = DataPackUtil.readDWord(buffer);
            System.out.printf("tripId: %d\n", tripId);

            // 5.VID
            // 扫描字符串
            String vid = DataPackUtil.readString(buffer);
            System.out.printf("vid: %s\n", vid);

            // 5.VIN
            // 扫描字符串
            String vin = DataPackUtil.readString(buffer);
            System.out.printf("vin: %s\n", vin);

            // 6.取得检测数据时间戳
            String receiveDate = DataPackUtil.readString(buffer);
            System.out.printf("receiveDate: %s\n", receiveDate);

            // 7.数据属性标识
            int dataAttrType = DataPackUtil.readByte(buffer);
            switch (dataAttrType) {
                case 0x01:
                    // 7.1 0x01-发动机点火时
                    // 数据格式: 【数据内容】::=【点火电压值】+【定位信息】
                    System.out.println("## 0x01-发动机点火时");

                    // 7.1.1 点火电源
                    String firingVoltageValue = DataPackUtil.readString(buffer);
                    System.out.printf("firingVoltageValue: %skm/h\n", firingVoltageValue);

                    // 7.1.2 定位信息
                    // 数据格式：【定位信息】::=【车速】+【当前行程行驶距离】+【经度】+【分割符】+【纬度】+【分割符】+【方向】+【分割符】+【定位时间】+【分割符】+【定位方式】
                    // 7.1.2.1 车速
                    String speed = DataPackUtil.readString(buffer);
                    System.out.printf("speed: %s\n", speed);

                    // 7.1.2.2 当前行程行驶距离
                    String travelDistance = DataPackUtil.readString(buffer);
                    System.out.printf("travelDistance: %s\n", travelDistance);

                    // 7.1.2.3 经度
                    String longitude = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("longitude: %s\n", longitude);

                    // 7.1.2.4 纬度
                    String latitude = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("latitude: %s\n", latitude);

                    // 7.1.2.5 方向
                    String direction = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("direction: %s\n", direction);

                    // 7.1.2.6 定位时间
                    String locationDate = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("locationDate: %s\n", locationDate);

                    // 7.1.2.7 定位方式
                    String locationMode = DataPackUtil.readString(buffer);
                    System.out.printf("locationMode: %s\n", locationMode);

                    break;
                case 0x02:
                    // 7.2 0x02-发动机运行中
                    // 数据格式: 【数据内容】::=【参数数量】+【【数据 ID】+【ID 数据内容】】+…
                    System.out.println("## 0x02-发动机运行中");

                    // 7.2.1 参数数量
                    int paramsTotal = DataPackUtil.readWord(buffer);
                    System.out.printf("paramsTotal: %d\n", paramsTotal);

                    // 7.2.1 参数键值对
                    int testingId;
                    String testingValue;
                    for(int i = 0; i < paramsTotal; i++) {
                        testingId = DataPackUtil.readWord(buffer);
                        testingValue = DataPackUtil.readString(buffer);
                        System.out.printf("%d-(testingId: 0x%s, testingValue: %s)\n", (i + 1), ByteBufUtil.hexDump(new byte[]{(byte) ((testingId >> 8) & 0xFF), (byte) (testingId & 0xFF)}), testingValue);
                    }
                    break;
                case 0x03:
                    // 7.3 0x03-发动机熄火时
                    // 数据格式: 【数据内容】::=【本行程数据小计】+【本行程车速分组统计】+【驾驶习惯统计】+【定位信息】
                    System.out.println("## 0x03-发动机熄火时");

                    // 7.3.1 【本行程数据小计】::=【本次发动机运行时间】+【本次行驶距离】+【本次平均油耗】+【累计行驶里程】+【累计平均油耗】
                    // 7.3.1.1 本次发动机运行时间
                    int runningSeconds = DataPackUtil.readWord(buffer);
                    System.out.printf("runningSeconds: %ds\n", runningSeconds);

                    // 7.3.1.2 本次行驶距离
                    int runningMeters = DataPackUtil.readLong(buffer);
                    System.out.printf("runningMeters: %dm\n", runningMeters);

                    // 7.3.1.3 本次平均油耗
                    int runningOilUsed = DataPackUtil.readWord(buffer);
                    System.out.printf("runningOilUsed: %d\n", runningOilUsed);

                    // 7.3.1.4 累计行驶里程
                    int runningTotalKilometers = DataPackUtil.readLong(buffer);
                    System.out.printf("runningTotalKilometers: %d\n", runningTotalKilometers);

                    // 7.3.1.5 累计平均油耗
                    int runningAvgOilUsed = DataPackUtil.readWord(buffer);
                    System.out.printf("runningAvgOilUsed: %d\n", runningAvgOilUsed);

                    // 7.3.2 【本行程车速分组统计】::=【数据组数】+【第 1 组数据】+...+【第 n 组数据】
                    // 7.3.2.1 数据组数
                    int dataArrayTotal = DataPackUtil.readByte(buffer);
                    System.out.printf("dataArrayTotal: %d\n", dataArrayTotal);

                    // 7.3.2.1 第i组数据
                    int runningSpeed, runningTimeUsed, runningDistance;
                    for (int i = 0; i < dataArrayTotal; i++) {
                        runningSpeed = DataPackUtil.readByte(buffer);
                        runningTimeUsed = DataPackUtil.readWord(buffer);
                        runningDistance = DataPackUtil.readLong(buffer);
                        System.out.printf("%d-(runningSpeed: %d, runningTimeUsed: %d, runningDistance: %d)\n", (i+1), runningSpeed, runningTimeUsed, runningDistance);
                    }

                    // 7.3.3 【驾驶习惯统计】::=【本次急加速次数】+【本次急减速次数】+【本次急转向次数】+【本次超速行驶时间】+【最高车速】
                    // 7.3.3.1 本次急加速次数
                    int thisAddSpeedTimes = DataPackUtil.readWord(buffer);
                    System.out.printf("thisAddSpeedTimes: %d\n", thisAddSpeedTimes);

                    // 7.3.3.2 本次急减速次数
                    int thisReduceSpeedTimes = DataPackUtil.readWord(buffer);
                    System.out.printf("thisReduceSpeedTimes: %d\n", thisReduceSpeedTimes);

                    // 7.3.3.3 本次急转向次数
                    int thisChangeDirectionTimes = DataPackUtil.readWord(buffer);
                    System.out.printf("thisChangeDirectionTimes: %d\n", thisChangeDirectionTimes);

                    // 7.3.3.4 本次超速行驶时间
                    int thisSpeedingDriveTime = DataPackUtil.readLong(buffer);
                    System.out.printf("thisSpeedingDriveTime: %d\n", thisSpeedingDriveTime);

                    // 7.3.3.5 最高车速
                    int thisHighSpeed = DataPackUtil.readByte(buffer);
                    System.out.printf("thisHighSpeed: %d\n", thisHighSpeed);

                    // 7.3.4 【定位信息】::=【车速】+【当前行程行驶距离】+【经度】+【分割符】+【纬度】+【分割符】+【方向】+【分割符】+【定位时间】+【分割符】+【定位方式】
                    // 7.3.4.1 车速
                    speed = DataPackUtil.readString(buffer);
                    System.out.printf("speed: %s\n", speed);

                    // 7.3.4.2 当前行程行驶距离
                    travelDistance = DataPackUtil.readString(buffer);
                    System.out.printf("travelDistance: %s\n", travelDistance);

                    // 7.3.4.3 经度
                    longitude = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("longitude: %s\n", longitude);

                    // 7.3.4.4 纬度
                    latitude = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("latitude: %s\n", latitude);

                    // 7.3.4.5 方向
                    direction = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("direction: %s\n", direction);

                    // 7.3.4.6 定位时间
                    locationDate = DataPackUtil.readStringEmic(buffer);
                    System.out.printf("locationDate: %s\n", locationDate);

                    // 7.3.4.7 定位方式
                    locationMode = DataPackUtil.readString(buffer);
                    System.out.printf("locationMode: %s\n", locationMode);
                    break;
                case 0x04:
                    // 7.4 0x04-发动机熄火后
                    // 数据格式: 【数据内容】::=【蓄电池电压值】
                    System.out.println("## 0x04-发动机熄火后");
                    // 7.4.1
                    String batteryVoltage = DataPackUtil.readString(buffer);
                    System.out.printf("batteryVoltage: %s\n", batteryVoltage);
                    break;
                case 0x05:
                    // 7.5 0x05-车辆不能检测
                    // 无【数据内容】上传
                    System.out.println("## 0x05-车辆不能检测");
                    break;
            }

        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(2, buffer.writerIndex() - buffer.readerIndex());
    }
}
