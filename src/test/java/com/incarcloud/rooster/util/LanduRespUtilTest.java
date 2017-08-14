package com.incarcloud.rooster.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;

/**
 * LandRespUtilTest
 *
 * @author Aaric, created on 2017-08-07T15:26.
 * @since 2.0
 */
public class LanduRespUtilTest {

    protected byte[] data1601 = {
            //----------0x1601
            (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
            0x00, 0x6a, //2字节-数据包长度
            (byte) 0xff, (byte) 0x95, //2字节-数据包长度校验(数据包长度取反)
            0x00, //1字节-数据包ID
            0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
            0x16, 0x01, 0x49, 0x4e, 0x43, 0x41, 0x52, 0x30, 0x30, 0x30, 0x30, 0x30, 0x39, 0x00, 0x00, 0x00, 0x00, 0x52, 0x31, 0x33, 0x00, 0x00, 0x32, 0x30, 0x31, 0x34, 0x2d, 0x30, 0x39, 0x2d, 0x30, 0x34, 0x20, 0x32, 0x33, 0x3a, 0x31, 0x35, 0x3a, 0x32, 0x33, 0x00, 0x01, 0x37, 0x2e, 0x38, 0x00, 0x30, 0x00, 0x30, 0x00, 0x57, 0x30, 0x30, 0x30, 0x2e, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x2c, 0x53, 0x30, 0x30, 0x2e, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x2c, 0x30, 0x2c, 0x31, 0x39, 0x37, 0x30, 0x2d, 0x30, 0x31, 0x2d, 0x30, 0x31, 0x20, 0x30, 0x30, 0x3a, 0x30, 0x30, 0x3a, 0x30, 0x34, 0x2c, 0x30, 0x00,
            0x13, 0x01, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
    };

    protected byte[] data1603 = {
            //----------0x1603
            (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
            0x00, 0x50, //2字节-数据包长度
            (byte) 0xff, (byte) 0xaf, //2字节-数据包长度校验(数据包长度取反)
            0x04, //1字节-数据包ID
            0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
            0x16, 0x03, 0x49, 0x4e, 0x43, 0x41, 0x52, 0x31, 0x30, 0x30, 0x30, 0x31, 0x31, 0x36, 0x38, 0x36, 0x34, 0x33, 0x00, 0x00, 0x00, 0x0e, 0x6f, 0x31, 0x33, 0x32, 0x00, 0x4c, 0x46, 0x56, 0x32, 0x41, 0x32, 0x38, 0x56, 0x32, 0x45, 0x35, 0x30, 0x30, 0x39, 0x37, 0x31, 0x30, 0x00, 0x56, 0x31, 0x2e, 0x36, 0x31, 0x2e, 0x30, 0x30, 0x00, 0x56, 0x31, 0x2e, 0x30, 0x2e, 0x30, 0x00, 0x56, 0x33, 0x2e, 0x31, 0x36, 0x2e, 0x35, 0x37, 0x00, (byte) 0xff, 0x00,
            0x10, 0x6a, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
    };

    @Test
    public void testResponseBytes() {
        Assert.assertEquals("AA55000BFFF40005160100021A", DatatypeConverter.printHexBinary(LanduRespUtil.responseBytes(data1601, (byte) 0x00)));
    }

    @Test
    public void testResponse0x1603Bytes() {
        // AA55002CFFD304051603323031372D30382D31342031363A31373A3437000000000000000000302E302E300006CB
        System.out.println(DatatypeConverter.printHexBinary(LanduRespUtil.response0x1603Bytes(data1603)));
    }

    @Test
    public void testResponse0x1603Bytes2() throws Exception {
        // AA5500B8FF4704051603323031372D30382D30342031353A35303A3030000200FF00056465762E696E636172646174612E636F6D2E636E00232D6465762E696E636172646174612E636F6D2E636E00232D6465762E696E636172646174612E636F6D2E636E00232D6465762E696E636172646174612E636F6D2E636E00232D6465762E696E636172646174612E636F6D2E636E00232D04012D5AFF0004780006006E840302D0550002767902012C01FF00FF302E302E30003619
        byte[] helloBytes = LanduRespUtil.response0x1603Bytes(data1603, "dev.incardata.com.cn", 9005);
        System.out.println(DatatypeConverter.printHexBinary(helloBytes));

        ByteBuf respBuffer = Unpooled.wrappedBuffer(helloBytes);
        respBuffer.readBytes(8);

        System.out.println(String.format("命令字：%s", ByteBufUtil.hexDump(respBuffer.readBytes(2))));
        System.out.println(String.format("当前时刻时间戮：%s", LanduDataPackUtil.readString(respBuffer)));
        System.out.println(String.format("执行动作值数量：%d", LanduDataPackUtil.readByte(respBuffer)));
        System.out.println(ByteBufUtil.hexDump(respBuffer.readBytes(2)));
        int vehicleCount = LanduDataPackUtil.readByte(respBuffer);
        System.out.println(String.format("车辆信息数量：%d", vehicleCount));
        if(0 < vehicleCount) {
            System.out.println(LanduDataPackUtil.readString(respBuffer));
            System.out.println(ByteBufUtil.hexDump(respBuffer.readBytes(3)));
            System.out.println(LanduDataPackUtil.readString(respBuffer));
        }
        int networkCount = LanduDataPackUtil.readByte(respBuffer);
        System.out.println(String.format("网络配置数量：%d", networkCount));
        for (int i = 0; i < networkCount; i++) {
            System.out.print(LanduDataPackUtil.readString(respBuffer));
            System.out.print(":");
            System.out.println(LanduDataPackUtil.readWord(respBuffer));
        }
        int speedCount = LanduDataPackUtil.readByte(respBuffer);
        System.out.println(String.format("车速分段统计设置数量：%d", speedCount));
        for (int i = 0; i < speedCount; i++) {
            System.out.println(LanduDataPackUtil.readByte(respBuffer));
        }
        int positionCount = LanduDataPackUtil.readByte(respBuffer);
        System.out.println(String.format("定位参数设置参数数量：%d", positionCount));
        if(0 < positionCount) {
            System.out.println(LanduDataPackUtil.readWord(respBuffer));
            System.out.println(LanduDataPackUtil.readWord(respBuffer));
            System.out.println(LanduDataPackUtil.readByte(respBuffer));
        }
        int alarmCount = LanduDataPackUtil.readByte(respBuffer);
        System.out.println(String.format("报警设置参数数量：%d", alarmCount));
        if(0 < alarmCount) {
            System.out.println(LanduDataPackUtil.readByte(respBuffer));
            System.out.println(LanduDataPackUtil.readWord(respBuffer));
            System.out.println(LanduDataPackUtil.readWord(respBuffer));
            System.out.println(LanduDataPackUtil.readByte(respBuffer));
        }
        int flameOutCount = LanduDataPackUtil.readByte(respBuffer);
        System.out.println(String.format("熄火后数据数量：%d", flameOutCount));
        if(0 < flameOutCount) {
            System.out.println(LanduDataPackUtil.readWord(respBuffer));
            System.out.println(LanduDataPackUtil.readByte(respBuffer));
            int voltageCount = LanduDataPackUtil.readWord(respBuffer);
            System.out.println(voltageCount);
            for (int i = 0; i < voltageCount; i++) {
                System.out.println(LanduDataPackUtil.readByte(respBuffer));
            }
        }
        int runDataCount = LanduDataPackUtil.readByte(respBuffer);
        System.out.println(String.format("运行中数据设置数量：%d", runDataCount));
        if(0 < runDataCount) {
            System.out.println(LanduDataPackUtil.readWord(respBuffer));
            for (int i = 0; i < runDataCount; i++) {
                System.out.println(LanduDataPackUtil.readWord(respBuffer));
            }
        }
        System.out.println(String.format("软件升级ID：%s", LanduDataPackUtil.readString(respBuffer)));

        respBuffer.release();
    }
}
