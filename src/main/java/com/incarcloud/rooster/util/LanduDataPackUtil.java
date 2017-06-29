package com.incarcloud.rooster.util;

import com.incarcloud.rooster.datapack.DataPackUtil;
import io.netty.buffer.ByteBuf;

/**
 * lANDU DataPack工具类
 *
 * @author Aaric, created on 2017-06-29T13:34.
 * @since 1.0-SNAPSHOT
 */
public class LanduDataPackUtil extends DataPackUtil {

    /**
     * 读取一个WORD类型数据<br>
     *     2 个字节，高位字节在前，低位字节在后
     *
     * @param buffer ByteBuf
     * @return integer
     */
    public static int readWord(ByteBuf buffer) {
        return readUInt2(buffer);
    }

    /**
     * 读取一个BYTE类型数据<br>
     *     1 个字节
     *
     * @param buffer ByteBuf
     * @return integer
     */
    public static int readByte(ByteBuf buffer) {
        return readUInt1(buffer);
    }

    /**
     * 读取一个SHORT类型数据<br>
     *     2 个字节有符号数，高字节在前，低字节在后
     *
     * @param buffer ByteBuf
     * @return integer
     */
    public static int readShort(ByteBuf buffer) {
        return readInt2(buffer);
    }

    /**
     * 读取一个LONG类型数据<br>
     *     4 个字节有符号数，高位字节在前，低位字节在后
     *
     * @param buffer ByteBuf
     * @return integer
     */
    public static int readLong(ByteBuf buffer) {
        return readInt4(buffer);
    }

    /**
     * 读取一个DWORD类型数据<br>
     *     4 个字节无符号数，高位字节在前，低位字节在后
     *
     * @param buffer ByteBuf
     * @return integer
     */
    public static long readDWord(ByteBuf buffer) {
        return readUInt4(buffer);
    }

    protected LanduDataPackUtil() {
        super();
    }
}
