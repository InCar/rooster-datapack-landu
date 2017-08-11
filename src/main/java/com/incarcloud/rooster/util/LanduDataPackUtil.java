package com.incarcloud.rooster.util;

import com.incarcloud.rooster.datapack.DataPackObject;
import com.incarcloud.rooster.datapack.DataPackPosition;
import io.netty.buffer.ByteBuf;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * LANDU DataPack工具类
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

    /**
     * 将E|W/S|N表示方式转换为数值类型<br>
     *     转换规则：E->+,W->-,N->+,S->-<br>
     * 例如：E116.362946->116.362946<br>
     *      N40.079099->40.079099<br>
     *      W116.362946->-116.362946<br>
     *      S40.079099->-40.079099
     *
     * @param positionString E|W/S|N表示方式字符串
     * @return double
     */
    public static double parsePositionString(String positionString) {
        String value = positionString.substring(1);
        switch (positionString.charAt(0)) {
            case 'E':
            case 'N':
                // 东经北纬为正数
                return Double.parseDouble(value);
            case 'W':
            case 'S':
                // 西经南纬为负数
                return 0.0 - Double.parseDouble(value);
        }
        return 0;
    }

    /**
     * 读取位置数据
     *
     * @param buffer ByteBuf
     * @param dataPackObject 基对象
     * @return 位置数据对象
     * @throws UnsupportedEncodingException
     * @throws ParseException
     */
    public static DataPackPosition readPositionObject(ByteBuf buffer, DataPackObject dataPackObject) throws UnsupportedEncodingException, ParseException {
        // buffer和dataPackObject不能为null
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        if(null == dataPackObject) {
            throw new IllegalArgumentException("dataPackObject is null");
        }

        // 初始化位置数据对象
        DataPackPosition dataPackPosition = new DataPackPosition(dataPackObject);

        // 车速(km/h)
        dataPackPosition.setSpeed(Integer.parseInt(readString(buffer)));
        // 当前行程行驶距离(m)
        dataPackPosition.setTravelDistance(Integer.parseInt(readString(buffer)));

        // 位置数据格式：【经度】+【分割符】+【纬度】+【分割符】+【方向】+【分割符】+【定位时间】+【分割符】+【定位方式】
        String[] positions = splitPositionString(buffer);
        // 经度
        dataPackPosition.setLongitude(parsePositionString(positions[0]));
        // 纬度
        dataPackPosition.setLatitude(parsePositionString(positions[1]));
        // 方向
        dataPackPosition.setDirection(Float.parseFloat(positions[2]));
        // 定位时间
        dataPackPosition.setPositionTime(formatDateString(positions[3]));
        // 6.2.7 定位方式：0-无效数据，1-基站定位，2-GPS定位
        dataPackPosition.setPositioMode(Integer.parseInt(positions[4]));
        // 6.2.8 定位方式描述
        switch (dataPackPosition.getPositioMode()) {
            case 0:
                // 无效数据
                dataPackPosition.setPositioModeDesc("无效数据");
                break;
            case 1:
                // 基站定位
                dataPackPosition.setPositioModeDesc("基站定位");
                break;
            case 2:
                // GPS定位
                dataPackPosition.setPositioModeDesc("GPS定位");
                break;
        }
        return dataPackPosition;
    }

    protected LanduDataPackUtil() {
        super();
    }
}
