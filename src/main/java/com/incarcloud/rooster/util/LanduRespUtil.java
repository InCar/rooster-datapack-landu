package com.incarcloud.rooster.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Land回复工具类
 *
 * @author Aaric, created on 2017-08-07T15:59.
 * @since 2.0
 */
public class LanduRespUtil {

    /**
     * 默认字符集
     */
    private static final Charset CharsetGBK = Charset.forName("GBK");

    /**
     * 格式化时间
     */
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 回复非0x1603命令字
     *
     * @param bytes 原始数据
     * @param errorCode 错误代码
     * @return
     */
    public static byte[] responseBytes(byte[] bytes, byte errorCode) {
        byte[] returnBytes = new byte[13];
        // 1.数据包标志
        returnBytes[0] = bytes[0];
        returnBytes[1] = bytes[1];
        // 2.数据包长度(固定长度13-2=11)
        returnBytes[2] = 0x00;
        returnBytes[3] = (byte) (13-2);
        // 3.数据包长度校验
        returnBytes[4] = (byte) ~returnBytes[2];
        returnBytes[5] = (byte) ~returnBytes[3];
        // 4.数据包ID
        returnBytes[6] = bytes[6];
        // 5.协议格式版本
        returnBytes[7] = bytes[7];
        // 6.数据内容(命令字)
        returnBytes[8] = bytes[8];
        returnBytes[9] = bytes[9];
        returnBytes[10] = errorCode; // 0x00-成功
        // 7.校验和
        int sum = 0;
        for (int i = 2; i < returnBytes.length - 2; i++) {
            sum += (returnBytes[i] & 0xFF);
        }
        returnBytes[11] = (byte) ((sum >> 8) & 0xFF);
        returnBytes[12] = (byte) (sum & 0xFF);

        return returnBytes;
    }

    /**
     * 回复0x1603命令字
     *
     * @param bytes 原始数据
     * @return
     */
    public static byte[] response0x1603Bytes(byte[] bytes) {
        // 初始化ByteBuf
        ByteBuf buffer = Unpooled.buffer(1024);

        // 1.数据包标志
        buffer.writeBytes(new byte[]{bytes[0], bytes[1]});
        // 2.数据包长度(预留空间)
        buffer.writeShort(0x0000);
        // 3.数据包长度校验(预留空间)
        buffer.writeShort(0xFFFF);
        // 4.数据包ID
        buffer.writeByte(bytes[6]);
        // 5.协议格式版本
        buffer.writeByte(bytes[7]);
        // 6.数据内容(命令字)
        buffer.writeBytes(new byte[]{bytes[8], bytes[9]});

        // 7.回复内容
        // 7.1 当前时刻时间戮（【STRING】YYYY-MM-DD hh:mm:ss）
        buffer.writeBytes(dateFormat.format(Calendar.getInstance().getTime()).getBytes(CharsetGBK));
        buffer.writeByte(0x00);
        // 7.2 执行动作值（【动作参数数量】+【恢复出厂设置序号】+【是否执行清码动作】）
        buffer.writeByte(0x00);
        // 7.3 车辆信息（【车辆信息参数数量】+【VID】+【品牌】+【系列】+【年款】+【排量】）
        buffer.writeByte(0x00);
        // 7.4 上传数据网络配置（【网络配置数量】+【网络配置 1】+...+【网络配置 n】）
        buffer.writeByte(0x00);
        // 7.5 车速分段统计设置（【分段数量】+【第 1 段最高车速】+…+【第 n 段最高车速】）
        buffer.writeByte(0x00);
        // 7.6 定位数据设置（【定位参数设置参数数量】+【定位间隔距离】+【定位间隔时间】+【距离与时间关系】）
        buffer.writeByte(0x00);
        // 7.7 报警设置（【报警设置参数数量】+【超速最小车速】+【超速报警的最小持续时间】+【报警水温值】+【充电电压报警值】）
        buffer.writeByte(0x00);
        // 7.8 熄火后数据设置（【熄火后数据数量】+【熄火后关闭时间点】+【关机临界电压值】+【熄火后电压设定】）
        buffer.writeByte(0x00);
        // 7.9 运行中数据设置（【数据 ID 数量】+【【数据间隔时间】+【【数据 ID】…+】】）
        buffer.writeByte(0x00);
        // 7.10 软件升级
        buffer.writeBytes("V0.00.00".getBytes(CharsetGBK));
        buffer.writeByte(0x00);

        // 8.获得字节码
        byte[] returnBytes = new byte[buffer.readableBytes() + 2];
        buffer.readBytes(returnBytes, 0, buffer.readableBytes());

        // 9.释放ByteBuf
        buffer.release();

        // 10.设置包长度和校验信息
        int length = returnBytes.length - 2;
        returnBytes[2] = (byte) ((length >> 8) & 0xFF);
        returnBytes[3] = (byte) (length & 0xFF);
        returnBytes[4] = (byte) ~returnBytes[2];
        returnBytes[5] = (byte) ~returnBytes[3];

        // 11.校验和
        int sum = 0;
        for (int i = 2; i < returnBytes.length - 2; i++) {
            sum += (returnBytes[i] & 0xFF);
        }
        returnBytes[returnBytes.length -2] = (byte) ((sum >> 8) & 0xFF);
        returnBytes[returnBytes.length -1] = (byte) (sum & 0xFF);
        return returnBytes;
    }

    protected LanduRespUtil() {}
}