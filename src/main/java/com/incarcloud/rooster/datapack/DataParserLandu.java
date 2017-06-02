package com.incarcloud.rooster.datapack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * LANDU Parser.
 *
 * @author Aaric
 * @since 2.0
 */
public class DataParserLandu implements IDataParser {

    /**
     * 1字节的byte转无符号整型
     *
     * @param value byte
     * @return 无符号整型
     */
    private static int byteToInt(final byte value) {
        ByteBuf buffer = Unpooled.wrappedBuffer(new byte[]{0x00, value});
        int integer = buffer.getUnsignedShort(0);
        buffer.release();
        return  integer;
    }

    /**
     * 2字节的byte数组转无符号整型
     *
     * @param bytes byte数组
     * @return 无符号整型
     */
    private static int byteArrayToInt(final byte[] bytes) {
        ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        int integer = buffer.getUnsignedShort(0);
        buffer.release();
        return  integer;
    }

    /**
     * 验证数据包
     *
     * @param buffer buffer
     * @return
     */
    private static boolean validate(ByteBuf buffer) {
        if(null != buffer) {
            int total = buffer.readableBytes();
            // 1.数据包长度必须大于9个字节(最小10个字节)
            if(9 < total) {
                // 2.判断数据包标志
                byte[] flagBytes = ByteBufUtil.getBytes(buffer, 0, 2);
                String flagBytesString = ByteBufUtil.hexDump(flagBytes).toUpperCase();
                if("AA55".equals(flagBytesString)) {
                    // 3.校验数据包长度(取反校验+数值校验)
                    if(buffer.getByte(2) == ~buffer.getByte(4) && buffer.getByte(3) == ~buffer.getByte(5)) {
                        int length = byteArrayToInt(ByteBufUtil.getBytes(buffer, 2, 2));
                        // 长度 = 总长度 - 2(2字节标志位)
                        if(length == (total - 2)) {
                            // 4.校验和校验
                            int sum = 0;
                            int sumCheck = byteArrayToInt(ByteBufUtil.getBytes(buffer, total-2, 2));
                            for (int i = 2; i < total - 2; i++) {
                                sum += byteToInt(buffer.getByte(i));
                            }
                            // 校验
                            if(sum == sumCheck) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     *　获得设备版本信息<br>
     *  <b>第7个字节为协议格式版本</b>
     *
     * @param buffer buffer
     * @return
     */
    private static String getVersion(ByteBuf buffer) {
        String version = null;
        if(null != buffer) {
            switch (buffer.getByte(7)) {
                case 0x02:
                    version = "2.05";
                    break;
                case 0x05:
                    version = "3.08";
                    break;
                default:
                    version = "unknown";
            }
        }
        return version;
    }

    @Override
    public List<DataPack> extract(ByteBuf buffer){
        /**
         * ## LANDU数据包格式 ##
         * 0,1: 数据包标志(AA55)
         * 2,3: 数据包长度
         * 4,5: 数据包长度校验(数据包长度取反)
         * 6: 数据包ID
         * 7: 保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
         * ...: 数据内容(其长度为【数据包长度】– 4 – 2)
         * 最后2字节: 校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
         */
        List<DataPack> dataPackList = new ArrayList<>();
        if(validate(buffer)) {
            // 打包
            DataPack dataPack = new DataPack("china", "landu", getVersion(buffer));
            dataPack.setBuf(buffer.slice(0, buffer.readableBytes()));
            dataPackList.add(dataPack);
        }

        return dataPackList;
    }

    /**
     * 回复数据
     *
     * @param buffer 原始数据
     * @param errorCode 错误代码
     * @return
     */
    private static byte[] responseBytes(ByteBuf buffer, byte errorCode) {
        byte[] bytes = null;
        if(validate(buffer)) {
            bytes = new byte[13];
            // 1.数据包标志
            bytes[0] = buffer.getByte(0);
            bytes[1] = buffer.getByte(1);
            // 2.数据包长度(固定长度13-2=11)
            bytes[2] = 0x00;
            bytes[3] = (byte) (13-2);
            // 3.数据包长度校验
            bytes[4] = (byte) ~bytes[2];
            bytes[5] = (byte) ~bytes[3];
            // 4.数据包ID
            bytes[6] = buffer.getByte(6);
            // 5.协议格式版本
            bytes[7] = buffer.getByte(7);
            // 6.数据内容(命令字)
            bytes[8] = buffer.getByte(8);
            bytes[9] = buffer.getByte(9);
            bytes[10] = errorCode; // 0x00-成功
            // 7.校验和
            int sum = 0;
            for (int i = 2; i < bytes.length - 2; i++) {
                sum += byteToInt(bytes[i]);
            }
            bytes[11] = (byte) (sum / (16*16));
            bytes[12] = (byte) (sum % (16*16));
        }
        return bytes;
    }

    @Override
    public ByteBuf createResponse(DataPack requestPack, ERespReason reason) {
        ByteBuf responseBuf = null;
        // 回复设备数据
        if(null != requestPack && ERespReason.OK == reason) {
            // 校验数据
            ByteBuf originalBuffer = Unpooled.wrappedBuffer(Base64.getDecoder().decode(requestPack.getDataB64()));
            // 封装返回数据
            byte[] returnBytes = responseBytes(originalBuffer, (byte) 0x00);
            if(null != returnBytes) {
                responseBuf = Unpooled.wrappedBuffer(returnBytes);
            }
            // 释放originalBuffer
            originalBuffer.release();
        }
        return responseBuf;
    }

    @Override
    public void destroyResponse(ByteBuf responseBuf) {
        if(null != responseBuf) {
            responseBuf.release();
        }
    }
}
