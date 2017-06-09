package com.incarcloud.rooster.datapack.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.io.UnsupportedEncodingException;

/**
 * ByteBuf工具类
 *
 * @author Aaric, created on 2017-06-06T16:45.
 * @since 1.0-SNAPSHOT
 */
public final class DataPackUtil {

    /**
     * 根据offset获得1byte有符号整型<br>
     *
     * @param buffer ByteBuf
     * @return int
     */
    public static int readInt1(ByteBuf buffer) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readInt1(buffer, offset);
    }

    /**
     * 根据offset获得1byte有符号整型<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @return int
     */
    public static int readInt1(ByteBuf buffer, int offset) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        // 丢弃1个字节
        buffer.skipBytes(1);
        // 返回数值
        return buffer.getByte(offset);
    }

    /**
     * 根据offset获得1byte无符号整型<br>
     *
     * @param buffer ByteBuf
     * @return int
     */
    public static int readUInt1(ByteBuf buffer) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readUInt1(buffer, offset);
    }

    /**
     * 根据offset获得1byte无符号整型<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @return int
     */
    public static int readUInt1(ByteBuf buffer, int offset) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        // 丢弃1个字节
        buffer.skipBytes(1);
        // 返回数值
        return buffer.getByte(offset) & 0xFF;
    }

    /**
     * 根据offset获得2byte有符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @return int
     */
    public static int readInt2(ByteBuf buffer) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readInt2(buffer, offset);
    }

    /**
     * 根据offset获得2byte有符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @return int
     */
    public static int readInt2(ByteBuf buffer, int offset) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        // 丢弃4个字节
        buffer.skipBytes(2);
        // 返回数值
        return buffer.getByte(offset) << 8
                | (buffer.getByte(offset + 1) & 0xFF);
    }

    /**
     * 根据offset获得2byte无符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @return int
     */
    public static int readUInt2(ByteBuf buffer) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readUInt2(buffer, offset);
    }

    /**
     * 根据offset获得2byte无符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @return int
     */
    public static int readUInt2(ByteBuf buffer, int offset) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        // 丢弃4个字节
        buffer.skipBytes(2);
        // 返回数值
        return (buffer.getByte(offset) & 0xFF) << 8
                | (buffer.getByte(offset + 1) & 0xFF);
    }

    /**
     * 根据offset获得4byte有符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @return int
     */
    public static int readInt4(ByteBuf buffer) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readInt4(buffer, offset);
    }

    /**
     * 根据offset获得4byte有符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @return int
     */
    public static int readInt4(ByteBuf buffer, int offset) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        // 丢弃4个字节
        buffer.skipBytes(4);
        // 返回数值
        return buffer.getByte(offset) << 24
                | (buffer.getByte(offset + 1) & 0xFF) << 16
                | (buffer.getByte(offset + 2) & 0xFF) << 8
                | (buffer.getByte(offset + 3) & 0xFF);
    }

    /**
     * 根据offset获得4byte无符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @return int
     */
    public static int readUInt4(ByteBuf buffer) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readUInt4(buffer, offset);
    }

    /**
     * 根据offset获得4byte无符号整型(高位在前)<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @return int
     */
    public static int readUInt4(ByteBuf buffer, int offset) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        // 丢弃4个字节
        buffer.skipBytes(4);
        // 返回数值
        return (buffer.getByte(offset) & 0xFF) << 24
                | (buffer.getByte(offset + 1) & 0xFF) << 16
                | (buffer.getByte(offset + 2) & 0xFF) << 8
                | (buffer.getByte(offset + 3) & 0xFF);
    }

    /**
     * 根据offset获得以0x00结束的字符串<br>
     *
     * @param buffer ByteBuf
     * @return string
     */
    public static String readString(ByteBuf buffer) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readString(buffer, offset);
    }

    /**
     * 根据offset获得以0x00结束的字符串<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @return string
     */
    public static String readString(ByteBuf buffer, int offset) {
        return readString(buffer, offset, (byte) 0x00);
    }

    /**
     * 根据offset获得以separator结束的字符串<br>
     *
     * @param buffer ByteBuf
     * @param separator 分隔符
     * @return string
     */
    public static String readString(ByteBuf buffer, byte separator) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        int offset = buffer.readerIndex();
        return readString(buffer, offset, separator);
    }

    /**
     * 根据offset获得以separator结束的字符串<br>
     *
     * @param buffer ByteBuf
     * @param offset 偏移
     * @param separator 分隔符
     * @return string
     */
    public static String readString(ByteBuf buffer, int offset, byte separator) {
        if(null == buffer) {
            throw new IllegalArgumentException("buffer is null");
        }
        // 扫描
        while (separator != buffer.getByte(buffer.readerIndex())) {
            buffer.skipBytes(1);
        }
        // 读取长度
        int length = buffer.readerIndex() - offset;
        // 丢弃0x00
        buffer.skipBytes(1);
        // 返回字符串
        String string = null;
        try {
            // LANDU使用的是GBK字符串
            string = new String(ByteBufUtil.getBytes(buffer, offset, length), "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return string;
    }

    /**
     * WORD：2个字节，高位字节在前，低位字节在后
     *
     * @param buffer ByteBuf
     * @return
     */
    public static int readWord(ByteBuf buffer) {
        return readUInt2(buffer);
    }

    /**
     * DWORD：4个字节无符号数，高位字节在前，低位字节在后
     *
     * @param buffer ByteBuf
     * @return
     */
    public static int readDWord(ByteBuf buffer) {
        return readUInt4(buffer);
    }

    /**
     * BYTE：1个字节
     *
     * @param buffer ByteBuf
     * @return
     */
    public static int readByte(ByteBuf buffer) {
        return readUInt1(buffer);
    }

    /**
     * SHORT：2个字节有符号数，高字节在前，低字节在后
     *
     * @param buffer ByteBuf
     * @return
     */
    public static int readShort(ByteBuf buffer) {
        return readInt2(buffer);
    }

    /**
     * LONG：4个字节有符号数，高位字节在前，低位字节在后
     *
     * @param buffer ByteBuf
     * @return
     */
    public static int readLong(ByteBuf buffer) {
        return readInt4(buffer);
    }

    /**
     * 根据offset获得以','结束的字符串<br>
     *
     * @param buffer ByteBuf
     * @return string
     */
    public static String readStringEmic(ByteBuf buffer) {
        return readString(buffer, (byte) ',');
    }

    /**
     * 推送消息远程诊断仪上传数据
     *
     * @param content 信息内容<br>
     *                <ol>
     *                <li>0x1621-【信息内容】::=【命令字】</li>
     *                <li>0x1622-【信息内容】::=【命令字】+【项数】+【【ID】+……】</li>
     *                <li>0x1623-【信息内容】::=【命令字】+【项数】+【【【ID】+【参数内容】】+……】</li>
     *                <li>0x1624-【信息内容】::=【命令字】</li>
     *                <li>0x1625-【信息内容】::=【命令字】</li>
     *                <li>0x1626-【信息内容】::=【命令字】</li>
     *                <li>0x16E0-【信息内容】::=【命令字】</li>
     *                </ol>
     * @return bytes
     */
    public static byte[] commandBytes(byte[] content) {
        if(null == content || 0 == content.length) {
            throw new IllegalArgumentException("content is null or the length is 0.");
        }
        // 【消息内容】::=【信息头】+【信息内容】
        // 【信息头】::=”LD” 的十六进制表示方式
        // 【信息内容】::=数据包格式中【数据内容】
        byte[] bytes = new byte[2+content.length];
        bytes[0] = 0x4C;
        bytes[1] = 0x44;
        for (int i = 2; i < bytes.length; i++) {
            bytes[i] = content[i-2];
        }
        return bytes;
    }

    private DataPackUtil(){}
}
