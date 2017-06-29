package com.incarcloud.rooster.util;

import com.incarcloud.rooster.datapack.DataPackUtil;
import io.netty.buffer.ByteBuf;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    /**
     * 格式化时间
     */
    public final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    /**
     * 读取一个时间类型数据<br>
     *     时间字符串格式必须是
     *
     * @param buffer
     * @return
     */
    public static Date readDate(ByteBuf buffer) throws UnsupportedEncodingException, ParseException {
        String dataString = readString(buffer);
        if(null != dataString && !"".equals(dataString.trim())) {
            return dateFormat.parse(dataString);
        }
        return  null;
    }

    protected LanduDataPackUtil() {
        super();
    }
}
