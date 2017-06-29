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
     *     时间字符串格式必须是yyyy-MM-dd HH:mm:ss
     *
     * @param buffer ByteBuf
     * @return Date
     */
    public static Date readDate(ByteBuf buffer) throws UnsupportedEncodingException, ParseException {
        return  formatDateString(readString(buffer));
    }

    /**
     * 将字符串时间转时间对象<br>
     *     时间字符串格式必须是yyyy-MM-dd HH:mm:ss
     *
     * @param dataString 时间字符串
     * @return Date
     */
    public static Date formatDateString(String dataString) throws ParseException {
        if(null != dataString && !"".equals(dataString.trim())) {
            return dateFormat.parse(dataString);
        }
        return null;
    }

    /**
     * 格式化位置字符串<br>
     *     示例：E116.362946,N40.079099,0,2014-09-04 16:19:28,2
     *
     * @param buffer ByteBuf
     * @return string array
     */
    public static String[] splitPositionString(ByteBuf buffer) throws UnsupportedEncodingException {
        return splitPositionString(readString(buffer));
    }

    /**
     * 格式化位置字符串<br>
     *     示例：E116.362946,N40.079099,0,2014-09-04 16:19:28,2
     *
     * @param positionString 位置字符串
     * @return string array
     */
    public static String[] splitPositionString(String positionString) {
        if(null == positionString && !positionString.contains(",")) {
            throw new IllegalArgumentException("positionString is illegal string");
        }
        return positionString.split(",");
    }

    protected LanduDataPackUtil() {
        super();
    }
}
