package com.incarcloud.rooster.datapack;

import io.netty.buffer.ByteBuf;
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
     * 验证数据包
     *
     * @param buffer buffer
     * @return
     */
    private boolean validate(ByteBuf buffer) {
        if(null != buffer) {
            int total = buffer.readableBytes();
            // 1.数据包长度必须大于9个字节(最小10个字节)
            if(9 < total) {
                // 2.判断数据包标志
                if(0xAA == (buffer.getByte(0) & 0xFF) && 0x55 == (buffer.getByte(1) & 0xFF)) {
                    // 3.校验数据包长度(取反校验+数值校验)
                    if(buffer.getByte(2) == ~buffer.getByte(4) && buffer.getByte(3) == ~buffer.getByte(5)) {
                        int length = (buffer.getByte(2) & 0xFF) << 8 | (buffer.getByte(3) & 0xFF);
                        // 长度 = 总长度 - 2(2字节标志位)
                        if(length == (total - 2)) {
                            // 4.校验和校验
                            int sum = 0;
                            int sumCheck = (buffer.getByte(total-2) & 0xFF) << 8 | (buffer.getByte(total-1));
                            for (int i = 2; i < total - 2; i++) {
                                sum += (buffer.getByte(i) & 0xFF);
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
            // 版本(第7个字节为协议格式版本)
            String version = null;
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

            // 打包
            DataPack dataPack = new DataPack("china", "landu", version);
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
    private byte[] responseBytes(ByteBuf buffer, byte errorCode) {
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
                sum += (bytes[i] & 0xFF);
            }
            bytes[11] = (byte) ((sum >> 8) & 0xFF);
            bytes[12] = (byte) (sum & 0xFF);
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
            // 封装返回数据(成功返回0x00)
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
