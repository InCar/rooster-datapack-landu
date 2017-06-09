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
 * 3.1.7 怠速车况数据(0x1608)<br>
 *     格式：【命令字】+【OBD 串号（设备号）】+【TripID】+【VID】+【VIN 码】+【取得检测数据时间戮】+【数据内容】
 *
 * @author Aaric, created on 2017-06-08T14:42.
 * @since 1.0-SNAPSHOT
 */
public class P0x1608IdlingDataTest {

    private ByteBuf buffer;

    @Before
    public void begin() throws IOException {
        // 准备数据
        String testFile = System.getProperty("user.dir") + "/src/test/resources/landu-dats/1608-怠速车况数据.dat";
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
        // 2.判断命令字-0x1608
        offset = buffer.readerIndex();
        if(0x16 == buffer.getByte(offset) && 0x08 == buffer.getByte(offset+1)) {
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

            // 8.数据内容
            // 格式：【数据内容】：：=【故障码】+【实时数据流(车况信息)】
            // 8.1 故障码
            // 格式：【故障码】::=【故障码列表】::=【故障码个数】+【【【故障码】+【故障码属性】+【故障码描述】】+……】
            // 8.1.1 故障码个数
            int troubleCodeTotal = DataPackUtil.readByte(buffer);
            System.out.printf("troubleCodeTotal: %d\n", troubleCodeTotal);

            // 8.1.2 故障码内容
            // 注：未找到测试数据，按照文档格式解析数据
            String troubleCode, troubleAttr, troubleDesc;
            for(int i = 0; i < troubleCodeTotal; i++) {
                // 8.1.2.1 故障码
                troubleCode = DataPackUtil.readString(buffer);
                // 8.1.2.2 故障码属性
                troubleAttr = DataPackUtil.readString(buffer);
                // 8.1.2.3 故障码描述
                troubleDesc = DataPackUtil.readString(buffer);
                // 打印
                System.out.printf("%d-(troubleCode: %s, troubleAttr: %s, troubleDesc: %s)\n", (i+1), troubleCode, troubleAttr, troubleDesc);
            }

            // 8.2 实时数据流(车况信息)
            // 格式：【实时数据流(车况信息)】::=【数据流个数】+【数据流内容】
            // 8.2.1 数据流个数
            int vehicleConditionTotal = DataPackUtil.readWord(buffer);
            System.out.printf("vehicleConditionTotal: %d\n", vehicleConditionTotal);

            // 8.2.1 数据流内容
            // 格式：【数据流内容】::=【【【ID】+【数据流值】】+……】
            int vehicleConditionId;
            String vehicleConditionContent;
            for(int i = 0; i < vehicleConditionTotal; i++) {
                vehicleConditionId = DataPackUtil.readWord(buffer);
                vehicleConditionContent = DataPackUtil.readString(buffer);
                System.out.printf("%d-(vehicleConditionId: 0x%s, vehicleConditionContent: %s)\n", (i+1), ByteBufUtil.hexDump(new byte[]{(byte) ((vehicleConditionId >> 8) & 0xFF), (byte) (vehicleConditionId & 0xFF)}), vehicleConditionContent);
            }
        }
        System.out.println("-------------------------end");
        System.out.printf("readerIndex: %d, writerIndex: %d\n", buffer.readerIndex(), buffer.writerIndex());
        Assert.assertEquals(2, buffer.writerIndex() - buffer.readerIndex());
    }
}
