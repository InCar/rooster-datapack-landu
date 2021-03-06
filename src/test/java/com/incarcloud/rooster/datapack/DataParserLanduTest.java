package com.incarcloud.rooster.datapack;

import com.incarcloud.rooster.util.LanduDataPackUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.Map;

/**
 * Test DataParserLANDU class
 *
 * @author Aaric
 * @since 2.0
 */
public class DataParserLanduTest {

    private static Logger s_logger = LoggerFactory.getLogger(DataParserLanduTest.class);

    private ByteBuf buffer;

    @Before
    public void begin() {
        // 测试数据
        byte[] data = {
                //0----------0x1601
                (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
                0x00, 0x6a, //2字节-数据包长度
                (byte) 0xff, (byte) 0x95, //2字节-数据包长度校验(数据包长度取反)
                0x00, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x01, 0x49, 0x4e, 0x43, 0x41, 0x52, 0x30, 0x30, 0x30, 0x30, 0x30, 0x39, 0x00, 0x00, 0x00, 0x00, 0x52, 0x31, 0x33, 0x00, 0x00, 0x32, 0x30, 0x31, 0x34, 0x2d, 0x30, 0x39, 0x2d, 0x30, 0x34, 0x20, 0x32, 0x33, 0x3a, 0x31, 0x35, 0x3a, 0x32, 0x33, 0x00, 0x01, 0x37, 0x2e, 0x38, 0x00, 0x30, 0x00, 0x30, 0x00, 0x57, 0x30, 0x30, 0x30, 0x2e, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x2c, 0x53, 0x30, 0x30, 0x2e, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x2c, 0x30, 0x2c, 0x31, 0x39, 0x37, 0x30, 0x2d, 0x30, 0x31, 0x2d, 0x30, 0x31, 0x20, 0x30, 0x30, 0x3a, 0x30, 0x30, 0x3a, 0x30, 0x34, 0x2c, 0x30, 0x00,
                0x13, 0x01, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
                //1----------0x1602
                (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
                0x00, 0x6d, //2字节-数据包长度
                (byte) 0xff, (byte) 0x92, //2字节-数据包长度校验(数据包长度取反)
                0x00, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x02, 0x49, 0x4e, 0x43, 0x41, 0x52, 0x30, 0x30, 0x30, 0x30, 0x30, 0x39, 0x00, 0x00, 0x00, 0x01, (byte) 0x81, 0x31, 0x33, 0x00, 0x00, 0x32, 0x30, 0x31, 0x34, 0x2d, 0x31, 0x31, 0x2d, 0x32, 0x34, 0x20, 0x31, 0x30, 0x3a, 0x32, 0x31, 0x3a, 0x31, 0x34, 0x00, 0x05, 0x30, 0x00, 0x34, 0x32, 0x38, 0x34, 0x00, 0x45, 0x31, 0x31, 0x36, 0x2e, 0x33, 0x31, 0x38, 0x32, 0x39, 0x39, 0x2c, 0x4e, 0x34, 0x30, 0x2e, 0x30, 0x37, 0x35, 0x34, 0x30, 0x31, 0x2c, 0x30, 0x2c, 0x32, 0x30, 0x31, 0x34, 0x2d, 0x31, 0x31, 0x2d, 0x32, 0x34, 0x20, 0x30, 0x32, 0x3a, 0x32, 0x31, 0x3a, 0x30, 0x38, 0x2c, 0x32, 0x00, 0x30, 0x2e, 0x37, 0x00,
                0x13, (byte) 0xf1, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
                //2----------0x1603
                (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
                0x00, 0x50, //2字节-数据包长度
                (byte) 0xff, (byte) 0xaf, //2字节-数据包长度校验(数据包长度取反)
                0x04, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x03, 0x49, 0x4e, 0x43, 0x41, 0x52, 0x31, 0x30, 0x30, 0x30, 0x31, 0x31, 0x36, 0x38, 0x36, 0x34, 0x33, 0x00, 0x00, 0x00, 0x0e, 0x6f, 0x31, 0x33, 0x32, 0x00, 0x4c, 0x46, 0x56, 0x32, 0x41, 0x32, 0x38, 0x56, 0x32, 0x45, 0x35, 0x30, 0x30, 0x39, 0x37, 0x31, 0x30, 0x00, 0x56, 0x31, 0x2e, 0x36, 0x31, 0x2e, 0x30, 0x30, 0x00, 0x56, 0x31, 0x2e, 0x30, 0x2e, 0x30, 0x00, 0x56, 0x33, 0x2e, 0x31, 0x36, 0x2e, 0x35, 0x37, 0x00, (byte) 0xff, 0x00,
                0x10, 0x6a, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
                //3----------0x1606
                (byte) 0xAA, 0x55, //2字节-数据包标志(AA55)
                0x01, (byte) 0xF6, //2字节-数据包长度
                (byte) 0xFE, 0x09, //2字节-数据包长度校验(数据包长度取反)
                0x1C, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x06, 0x31, 0x32, 0x37, 0x33, 0x37, 0x36, 0x34, 0x36, 0x00, 0x00, 0x00, 0x00, 0x6E, 0x31, 0x34, 0x35, 0x30, 0x39, 0x00, 0x31, 0x43, 0x33, 0x48, 0x38, 0x42, 0x33, 0x47, 0x33, 0x36, 0x59, 0x31, 0x30, 0x39, 0x36, 0x39, 0x35, 0x00, 0x00, 0x08, 0x35, 0x32, 0x00, 0x33, 0x33, 0x39, 0x34, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x38, 0x37, 0x31, 0x31, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x38, 0x30, 0x31, 0x31, 0x2C, 0x32, 0x34, 0x36, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x34, 0x38, 0x3A, 0x35, 0x36, 0x2C, 0x32, 0x00, 0x33, 0x38, 0x00, 0x33, 0x35, 0x32, 0x30, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x37, 0x35, 0x36, 0x39, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x37, 0x35, 0x32, 0x31, 0x2C, 0x32, 0x35, 0x32, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x34, 0x39, 0x3A, 0x30, 0x35, 0x2C, 0x32, 0x00, 0x36, 0x00, 0x33, 0x35, 0x38, 0x31, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x36, 0x39, 0x33, 0x37, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x37, 0x33, 0x30, 0x35, 0x2C, 0x30, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x34, 0x39, 0x3A, 0x32, 0x32, 0x2C, 0x32, 0x00, 0x31, 0x30, 0x00, 0x33, 0x35, 0x38, 0x33, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x36, 0x39, 0x33, 0x30, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x37, 0x33, 0x30, 0x34, 0x2C, 0x32, 0x34, 0x36, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x34, 0x39, 0x3A, 0x32, 0x33, 0x2C, 0x32, 0x00, 0x33, 0x35, 0x00, 0x33, 0x36, 0x37, 0x31, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x36, 0x35, 0x32, 0x39, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x36, 0x38, 0x36, 0x34, 0x2C, 0x31, 0x36, 0x36, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x34, 0x39, 0x3A, 0x33, 0x35, 0x2C, 0x32, 0x00, 0x32, 0x36, 0x00, 0x33, 0x37, 0x34, 0x38, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x36, 0x37, 0x30, 0x34, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x36, 0x32, 0x39, 0x39, 0x2C, 0x31, 0x35, 0x38, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x34, 0x39, 0x3A, 0x34, 0x36, 0x2C, 0x32, 0x00, 0x34, 0x32, 0x00, 0x33, 0x38, 0x33, 0x35, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x36, 0x39, 0x35, 0x36, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x35, 0x36, 0x32, 0x37, 0x2C, 0x31, 0x35, 0x35, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x34, 0x39, 0x3A, 0x35, 0x35, 0x2C, 0x32, 0x00, 0x35, 0x31, 0x00, 0x33, 0x39, 0x34, 0x39, 0x00, 0x45, 0x31, 0x31, 0x33, 0x2E, 0x32, 0x33, 0x37, 0x34, 0x35, 0x32, 0x2C, 0x4E, 0x32, 0x33, 0x2E, 0x30, 0x39, 0x34, 0x36, 0x32, 0x36, 0x2C, 0x31, 0x35, 0x35, 0x2C, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x35, 0x2D, 0x32, 0x37, 0x20, 0x30, 0x30, 0x3A, 0x35, 0x30, 0x3A, 0x30, 0x34, 0x2C, 0x32, 0x00,
                0x5F, 0x0E, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
                //4----------0x1607
                (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
                0x00, (byte) 0xc4, //2字节-数据包长度
                (byte) 0xff, 0x3b, //2字节-数据包长度校验(数据包长度取反)
                0x1d, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x07, 0x49, 0x4e, 0x43, 0x41, 0x52, 0x31, 0x30, 0x30, 0x30, 0x31, 0x33, 0x34, 0x31, 0x33, 0x38, 0x39, 0x00, 0x00, 0x00, 0x00, 0x0d, 0x33, 0x37, 0x00, 0x56, 0x46, 0x31, 0x56, 0x59, 0x52, 0x4a, 0x54, 0x31, 0x45, 0x43, 0x35, 0x33, 0x34, 0x30, 0x37, 0x34, 0x00, 0x32, 0x30, 0x31, 0x34, 0x2d, 0x31, 0x32, 0x2d, 0x32, 0x30, 0x20, 0x31, 0x34, 0x3a, 0x35, 0x32, 0x3a, 0x33, 0x33, 0x00, 0x00, 0x12, 0x00, 0x03, 0x50, 0x31, 0x32, 0x31, 0x32, 0x00, 0x00, 0x04, 0x4f, 0x4c, 0x00, 0x00, 0x05, 0x2d, 0x2d, 0x2d, 0x00, 0x00, 0x06, 0x31, 0x30, 0x30, 0x2e, 0x30, 0x00, 0x00, 0x07, 0x34, 0x36, 0x00, 0x00, 0x08, 0x30, 0x2e, 0x30, 0x00, 0x00, 0x0a, 0x30, 0x2e, 0x30, 0x00, 0x00, 0x12, 0x31, 0x32, 0x38, 0x38, 0x00, 0x00, 0x13, 0x30, 0x00, 0x00, 0x15, 0x31, 0x37, 0x00, 0x00, 0x16, 0x37, 0x2e, 0x38, 0x36, 0x00, 0x00, 0x17, 0x35, 0x2e, 0x39, 0x00, 0x00, 0x19, 0x4f, 0x32, 0x53, 0x31, 0x32, 0x20, 0x7c, 0x20, 0x4f, 0x32, 0x53, 0x31, 0x31, 0x00, 0x00, 0x1a, 0x30, 0x2e, 0x34, 0x33, 0x30, 0x00, 0x00, 0x1b, 0x30, 0x2e, 0x30, 0x00, 0x00, 0x1c, 0x30, 0x2e, 0x32, 0x38, 0x30, 0x00, 0x00, 0x1d, 0x39, 0x39, 0x2e, 0x32, 0x00, 0x00, 0x7d, 0x33, 0x00,
                0x1e, 0x2f, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
                //5----------0x1608
                (byte) 0xAA, 0x55, //2字节-数据包标志(AA55)
                0x01, 0x71,        //2字节-数据包长度
                (byte) 0xFE, (byte) 0x8E, //2字节-数据包长度校验(数据包长度取反)
                0x46, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x08, 0x31, 0x32, 0x30, 0x37, 0x30, 0x33, 0x36, 0x39, 0x00, 0x00, 0x00, 0x03, (byte) 0xE0, 0x31, 0x34, 0x34, 0x37, 0x32, 0x00, 0x00, 0x32, 0x30, 0x31, 0x37, 0x2D, 0x30, 0x38, 0x2D, 0x31, 0x30, 0x20, 0x31, 0x33, 0x3A, 0x31, 0x39, 0x3A, 0x32, 0x37, 0x00, 0x05, 0x50, 0x30, 0x32, 0x30, 0x31, 0x00, (byte) 0xB4, (byte) 0xE6, (byte) 0xD6, (byte) 0xFC, (byte) 0xB9, (byte) 0xCA, (byte) 0xD5, (byte) 0xCF, (byte) 0xC2, (byte) 0xEB, 0x00, (byte) 0xC5, (byte) 0xE7, (byte) 0xD3, (byte) 0xCD, (byte) 0xD7, (byte) 0xEC, (byte) 0xB5, (byte) 0xE7, (byte) 0xC2, (byte) 0xB7, 0x2F, (byte) 0xB6, (byte) 0xCF, (byte) 0xC2, (byte) 0xB7, (byte) 0xB5, (byte) 0xDA, 0x31, (byte) 0xB8, (byte) 0xD7, 0x00, 0x50, 0x30, 0x32, 0x30, 0x33, 0x00, (byte) 0xB4, (byte) 0xE6, (byte) 0xD6, (byte) 0xFC, (byte) 0xB9, (byte) 0xCA, (byte) 0xD5, (byte) 0xCF, (byte) 0xC2, (byte) 0xEB, 0x00, (byte) 0xC5, (byte) 0xE7, (byte) 0xD3, (byte) 0xCD, (byte) 0xD7, (byte) 0xEC, (byte) 0xB5, (byte) 0xE7, (byte) 0xC2, (byte) 0xB7, 0x2F, (byte) 0xB6, (byte) 0xCF, (byte) 0xC2, (byte) 0xB7, (byte) 0xB5, (byte) 0xDA, 0x33, (byte) 0xB8, (byte) 0xD7, 0x00, 0x50, 0x30, 0x32, 0x30, 0x34, 0x00, (byte) 0xB4, (byte) 0xE6, (byte) 0xD6, (byte) 0xFC, (byte) 0xB9, (byte) 0xCA, (byte) 0xD5, (byte) 0xCF, (byte) 0xC2, (byte) 0xEB, 0x00, (byte) 0xC5, (byte) 0xE7, (byte) 0xD3, (byte) 0xCD, (byte) 0xD7, (byte) 0xEC, (byte) 0xB5, (byte) 0xE7, (byte) 0xC2, (byte) 0xB7, 0x2F, (byte) 0xB6, (byte) 0xCF, (byte) 0xC2, (byte) 0xB7, (byte) 0xB5, (byte) 0xDA, 0x34, (byte) 0xB8, (byte) 0xD7, 0x00, 0x50, 0x30, 0x32, 0x30, 0x32, 0x00, (byte) 0xB4, (byte) 0xE6, (byte) 0xD6, (byte) 0xFC, (byte) 0xB9, (byte) 0xCA, (byte) 0xD5, (byte) 0xCF, (byte) 0xC2, (byte) 0xEB, 0x00, (byte) 0xC5, (byte) 0xE7, (byte) 0xD3, (byte) 0xCD, (byte) 0xD7, (byte) 0xEC, (byte) 0xB5, (byte) 0xE7, (byte) 0xC2, (byte) 0xB7, 0x2F, (byte) 0xB6, (byte) 0xCF, (byte) 0xC2, (byte) 0xB7, (byte) 0xB5, (byte) 0xDA, 0x32, (byte) 0xB8, (byte) 0xD7, 0x00, 0x50, 0x30, 0x31, 0x37, 0x32, 0x00, (byte) 0xB4, (byte) 0xE6, (byte) 0xD6, (byte) 0xFC, (byte) 0xB9, (byte) 0xCA, (byte) 0xD5, (byte) 0xCF, (byte) 0xC2, (byte) 0xEB, 0x00, (byte) 0xCF, (byte) 0xB5, (byte) 0xCD, (byte) 0xB3, 0x28, (byte) 0xBB, (byte) 0xEC, (byte) 0xBA, (byte) 0xCF, (byte) 0xB1, (byte) 0xC8, 0x29, (byte) 0xCC, (byte) 0xAB, (byte) 0xC5, (byte) 0xA8, 0x20, 0x28, (byte) 0xB5, (byte) 0xDA, 0x31, (byte) 0xC5, (byte) 0xC5, 0x29, 0x00, 0x00, 0x14, 0x00, 0x00, 0x31, 0x34, 0x2E, 0x31, 0x37, 0x00, 0x00, 0x01, 0x31, 0x00, 0x00, 0x02, (byte) 0xB9, (byte) 0xD8, 0x00, 0x00, 0x04, 0x43, 0x4C, 0x00, 0x00, 0x05, 0x2D, 0x2D, 0x2D, 0x00, 0x00, 0x06, 0x32, 0x2E, 0x34, 0x00, 0x00, 0x07, 0x38, 0x38, 0x00, 0x00, 0x08, 0x32, 0x39, 0x2E, 0x37, 0x00, 0x00, 0x0A, 0x2D, 0x32, 0x33, 0x2E, 0x34, 0x00, 0x00, 0x11, 0x34, 0x32, 0x00, 0x00, 0x12, 0x39, 0x31, 0x33, 0x00, 0x00, 0x13, 0x30, 0x00, 0x00, 0x14, 0x31, 0x30, 0x00, 0x00, 0x15, 0x36, 0x32, 0x00, 0x00, 0x17, 0x30, 0x2E, 0x30, 0x00, 0x00, 0x19, 0x4F, 0x32, 0x53, 0x31, 0x31, 0x00, 0x00, 0x1A, 0x30, 0x2E, 0x30, 0x33, 0x30, 0x00, 0x00, 0x1B, 0x32, 0x39, 0x2E, 0x37, 0x00, 0x00, 0x1C, 0x30, 0x2E, 0x30, 0x30, 0x30, 0x00, 0x00, 0x4A, 0x2D, 0x2D, 0x2D, 0x00,
                (byte) 0x90, 0x6E, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
                //6----------0x160a
                (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
                0x00, (byte) 0x82, //2字节-数据包长度
                (byte) 0xff, 0x7d, //2字节-数据包长度校验(数据包长度取反)
                0x05, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x0a, 0x49, 0x4e, 0x43, 0x41,
                0x52, 0x31, 0x30, 0x30, 0x30, 0x31, 0x31, 0x36, 0x38, 0x36, 0x34, 0x33, 0x00, 0x00, 0x00, 0x0e, 0x6e, 0x31, 0x33, 0x32, 0x00, 0x4c, 0x46, 0x56, 0x32, 0x41, 0x32, 0x38, 0x56, 0x32, 0x45, 0x35, 0x30, 0x30, 0x39, 0x37, 0x31, 0x30, 0x00, 0x32, 0x30, 0x31, 0x36, 0x2d, 0x30, 0x34, 0x2d, 0x30, 0x35, 0x20, 0x31, 0x33, 0x3a, 0x31, 0x36, 0x3a, 0x31, 0x35, 0x00, 0x02, 0x34, 0x34, 0x00, 0x31, 0x33, 0x33, 0x36, 0x31, 0x00, 0x45, 0x31, 0x31, 0x36, 0x2e, 0x35, 0x32, 0x34, 0x34, 0x36, 0x34, 0x2c, 0x4e, 0x33, 0x39, 0x2e, 0x39, 0x30, 0x38, 0x35, 0x36, 0x30, 0x2c, 0x30, 0x2c, 0x32, 0x30, 0x31, 0x36, 0x2d, 0x30, 0x34, 0x2d, 0x30, 0x35, 0x20, 0x30, 0x35, 0x3a, 0x31, 0x36, 0x3a, 0x31, 0x36, 0x2c, 0x32, 0x00,
                0x19, 0x13, //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
                //7----------0x1621
                (byte) 0xaa, 0x55, //2字节-数据包标志(AA55)
                0x00, 0x31, //2字节-数据包长度
                (byte) 0xff, (byte) 0xce, //2字节-数据包长度校验(数据包长度取反)
                0x00, //1字节-数据包ID
                0x05, //1字节-保留字节(协议格式版本, v2.05-0x02, v3.08-0x05)
                0x16, 0x21, 0x49, 0x4e, 0x43, 0x41, 0x52, 0x30, 0x30, 0x30, 0x30, 0x30, 0x38, 0x00, 0x00, 0x00, 0x01, 0x02, 0x31, 0x32, 0x00, 0x4c, 0x53, 0x47, 0x54, 0x42, 0x35, 0x34, 0x4d, 0x35, 0x42, 0x59, 0x30, 0x30, 0x34, 0x30, 0x33, 0x37, 0x00, 0x01, 0x00,
                0x09, 0x66 //2字节-校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
        };
        buffer = Unpooled.wrappedBuffer(data);
    }

    @After
    public void end() {
        //Assert.assertEquals(0, buffer.refCnt());
        buffer.release();
    }

//    @Test
    public void testExtract() throws Exception {
        IDataParser parser = new DataParserLandu();
        List<DataPack> dataPackList = parser.extract(buffer);
        Assert.assertEquals(8, dataPackList.size());

        for(DataPack dataPack: dataPackList) {
            dataPack.freeBuf();
        }
    }

//    @Test
    public void testCreateResponse() {
        IDataParser parser = new DataParserLandu();
        ByteBuf responseBuf = parser.createResponse(parser.extract(buffer).get(0), ERespReason.OK);
        //System.out.println(DataParserLandu.validate(ByteBufUtil.getBytes(responseBuf, 0, responseBuf.readableBytes())));
        Assert.assertEquals("AA55000BFFF40005160100021A", DatatypeConverter.printHexBinary(ByteBufUtil.getBytes(responseBuf, 0, responseBuf.readableBytes())));
    }

//    @Test
    public void testCreateResponse0x1603() throws Exception {
        IDataParser parser = new DataParserLandu();
        ByteBuf respBuffer = parser.createResponse(parser.extract(buffer).get(2), ERespReason.OK);
        //System.out.println(DataParserLandu.validate(ByteBufUtil.getBytes(responseBuf, 0, responseBuf.readableBytes())));
        // 跳过数据包头
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
    }

    @Test
    public void testExtractBody() {
        IDataParser parser = new DataParserLandu();
        List<DataPack> dataPackList = parser.extract(buffer);
        for (int i = 0; i < 8; i++) {
            List<DataPackTarget> dataPackTargetList = parser.extractBody(dataPackList.get(i));
            //System.out.println(dataPackTargetList);
            Assert.assertNotEquals(0, dataPackTargetList);
        }
    }

//    @Test
    public void testGetMetaData() throws Exception {
        IDataParser parser = new DataParserLandu();
        Map<String, Object> metaDataMap = parser.getMetaData(buffer);
        Assert.assertEquals(3, metaDataMap.size());
    }
}