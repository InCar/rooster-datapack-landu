package com.incarcloud.rooster.datapack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

/**
 * LANDU Parser.
 *
 * @author Aaric
 * @since 2.0
 */
public class DataParserLandu implements IDataParser {

    /**
     * 反转byte数组
     *
     * @param bytes byte数组
     * @return byte数组
     */
    private static byte[] reverseByteArray(final byte[] bytes) {
        if(null == bytes) {
            throw new IllegalArgumentException("bytes is null.");
        }
        byte[] newBytes = new byte[bytes.length];
        for(int i = 0; i < bytes.length; i++) {
            newBytes[i] = (byte) ~bytes[i];
        }
        return newBytes;
    }

    /**
     * 2字节的byte数组转整型
     *
     * @param bytes byte数组
     * @return 整型
     */
    private static int byteArray2ToInt(final byte[] bytes) {
        byte[] newBytes = new byte[]{0x00, 0x00, 0x00, 0x00};
        newBytes[2] = bytes[0];
        newBytes[3] = bytes[1];
        ByteBuf buffer = Unpooled.wrappedBuffer(newBytes);
        int integer = buffer.getInt(0);
        buffer.release();
        return  integer;
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
        int total = buffer.readableBytes();
        // 1.数据包长度必须大于9个字节(最小10个字节)
        if(9 < total) {
            // 2.判断数据包标志
            byte[] flagBytes = ByteBufUtil.getBytes(buffer, 0, 2);
            String flagBytesString = ByteBufUtil.hexDump(flagBytes).toUpperCase();
            if("AA55".equals(flagBytesString)) {
                // 3.校验数据包长度(取反校验+数值校验)
                if(buffer.getByte(2) == ~buffer.getByte(4) && buffer.getByte(3) == ~buffer.getByte(5)) {
                    int length = byteArray2ToInt(ByteBufUtil.getBytes(buffer, 2, 2));
                    // 长度 = 总长度 - 2(2字节标志位)
                    if(length == (total - 2)) {
                        // 4.校验和校验
                        /*int sum = 0;
                        for (int i = 2; i < total - 2; i++) {
                            System.out.println(DatatypeConverter.printHexBinary(new byte[]{buffer.getByte(i)}));
                            sum += buffer.getByte(i);
                        }
                        System.out.println(sum);
                        byte[] bs = new byte[]{buffer.getByte(total-2), buffer.getByte(total-1)};
                        System.out.println(DatatypeConverter.printHexBinary(ByteBufUtil.getBytes(buffer, total-2, 2)));
                        System.out.println(byteArray2ToInt(ByteBufUtil.getBytes(buffer, total-2, 2)));*/
                        // 5.打包(第7个字节为协议格式版本)
                        String version = "";
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
                        DataPack dataPack = new DataPack("china", "landu", version);
                        dataPack.setBuf(buffer.slice(0, total));
                        dataPackList.add(dataPack);
                    }
                }
            }
        }

        return dataPackList;
    }
}
