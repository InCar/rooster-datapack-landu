package com.incarcloud.rooster.datapack;/**
 * Created by fanbeibei on 2017/8/22.
 */

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Fan Beibei
 * @Description: 蓝度设备总线协议（UBI）
 * landu的ubi格式与普通landu格式一样不同的是命令字只有1603和1620
 * @date 2017/8/22 10:32
 */
public class DataParserLanduUbi implements IDataParser {

    /**
     * 协议分组和名称
     */
    public static final String PROTOCOL_GROUP = "china";
    public static final String PROTOCOL_NAME = "landu-ubi";
    public static final String PROTOCOL_PREFIX = PROTOCOL_GROUP + "-" + PROTOCOL_NAME + "-";

    static {
        /**
         * 声明数据包版本与解析器类关系
         */
        DataParserManager.register(PROTOCOL_PREFIX + "2.5", DataParserLanduUbi.class);
        DataParserManager.register(PROTOCOL_PREFIX + "3.5", DataParserLanduUbi.class);
    }

    /**
     * 数据包准许最大容量2M
     */
    private static final int DISCARDS_MAX_LENGTH = 1024 * 1024 * 2;


    /**
     * <p>抽取出完整有效的数据包,并从buffer丢弃掉已经解析或无用的字节</p>
     *
     * ## LANDU数据包格式 ##
     * 0,1: 数据包标志(AA55  2个字节)
     * 2,3: 数据包长度，包括从【数据包长度】起至【校验和】止的所有字节数量，不是整个包的长度(2个字节)
     * 4,5: 数据包长度校验(数据包长度取反   2个字节)
     * 6: 数据包ID  (1个字节)
     * 7: 保留字节(协议格式版本, v2.05-0x02, v3.08-0x05     1个字节)
     * ...: 数据内容(其长度为【数据包长度】– 4 – 2   不定长度)
     * 最后2字节: 校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
     *
     * landu的ubi格式与普通landu格式一样不同的是命令字只有1603和1620，所以这里只要校验landu格式即可
     *
     * @param buffer
     *            二进制数据包
     * @return
     */
    @Override
    public List<DataPack> extract(ByteBuf buffer) {
        // 长度大于2M的数据包直接抛弃(恶意数据)
        if(DISCARDS_MAX_LENGTH < buffer.readableBytes()) {
            //System.out.println("clear");
            buffer.clear();
            return null;
        }


        DataPack dataPack;
        List<DataPack>  dataPackList = new ArrayList<>();

        int readIndex/*当前读指针*/, writeIndex/*当前写指针*/, packLenVal/*数据包长度字段值*/, packCheck/*数据包实际校验和*/, packCheckVal/*数据包校验和字段值*/;
        while(buffer.isReadable()) {
            readIndex = buffer.readerIndex();
            writeIndex = buffer.writerIndex();

            if(writeIndex - readIndex < 10){// 一个包最小10个字节
                break;
            }

            // 判断数据包标志,检查头两个字节是否为 AA 55,不为AA 55不停往下跳过一字节直到头两个字节为 AA 55为止
            if( 0xAA != (buffer.getByte(readIndex) & 0xFF) ||  0x55 != (buffer.getByte(readIndex + 1) & 0xFF)) {
                buffer.skipBytes(1);
                continue;
            }

            // 获取校验数据包长度(第3、4个字节是数据包长度，第5、6个字节是第3、4个字节取反)
            if(buffer.getByte(readIndex +2) != ~buffer.getByte(readIndex + 4)
                    || buffer.getByte(readIndex + 3) != ~buffer.getByte(readIndex + 5)) {//长度校验不通过，说明这里读到的是不完整的包
                buffer.skipBytes(2);//这里前面包头检查通过，所以跳过已校验的 AA 55 两个字节
                continue;
            }

            //获取数据包长度字段，数据包长度字段是从【数据包长度】起至【校验和】止的所有字节数量，不是整个包的长度
            packLenVal = (buffer.getByte(readIndex + 2) & 0xFF) << 8 | (buffer.getByte(readIndex + 3) & 0xFF);
            if(packLenVal > (writeIndex - readIndex - 2)) {//buffer中可读字节数小于数据包的长度
                //包长度不够，有以下两种情况
                // 1、可能是tcp拆包引起的半个包情况，直接跳出循环结束此次解析,等缓存区积累够了再解析
                // 2、包体里面含有 AA 55而且AA 55 后面的4个字节能通过数据包长度校验 导致解析出错,这种情况有可能会误伤后面的正常包，
                // 但这种情况概率极小，所以这里直接跳出循环结束此次解析，等缓存区积累够了再当成“正常包”解析，会在第二步解析不到数据直接丢弃
                break;
            }


            //校验和(最后两字节)验证
            packCheckVal = (buffer.getByte(readIndex + packLenVal) & 0xFF) << 8 | (buffer.getByte(readIndex + packLenVal + 1) & 0xFF);
            //计算实际校验和
            packCheck=0;
            for (int i = readIndex + 2, n = readIndex + packLenVal; i < n; i++) {
                packCheck += (buffer.getByte(i) & 0xFF);
            }
            if(packCheck != packCheckVal){
                buffer.skipBytes(2);//这里前面包头检查通过，所以跳过已校验的 AA 55 两个字节
                continue;
            }

            // 版本(第7个字节为协议格式版本)
            String version = null;
            switch (buffer.getByte(readIndex + 7)) {
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
            dataPack = new DataPack(PROTOCOL_GROUP, PROTOCOL_NAME, version);
            dataPack.setBuf(buffer.slice(readIndex, packLenVal + 2));
            dataPackList.add(dataPack);
            buffer.skipBytes(packLenVal + 2);
        }

        // 扔掉已读数据
        buffer.discardSomeReadBytes();


        if(dataPackList.size() > 0){
            return dataPackList;
        }

        return null;
    }

    @Override
    public ByteBuf createResponse(DataPack requestPack, ERespReason reason) {
        return null;
    }

    @Override
    public void destroyResponse(ByteBuf responseBuf) {
        if(null != responseBuf) {
            responseBuf.release();
        }
    }

    @Override
    public List<DataPackTarget> extractBody(DataPack dataPack) {
        return null;
    }

    @Override
    public Map<String, Object> getMetaData(ByteBuf buffer) {
        return null;
    }
}
