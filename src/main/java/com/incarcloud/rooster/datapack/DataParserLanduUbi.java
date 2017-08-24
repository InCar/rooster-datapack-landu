package com.incarcloud.rooster.datapack;/**
 * Created by fanbeibei on 2017/8/22.
 */

import com.incarcloud.rooster.util.LanduDataPackUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    public static final String PROTOCOL_NAME = "landu$ubi";
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
     * <p>
     * ## LANDU数据包格式 ##
     * 0,1: 数据包标志(AA55  2个字节)
     * 2,3: 数据包长度，包括从【数据包长度】起至【校验和】止的所有字节数量，不是整个包的长度(2个字节)
     * 4,5: 数据包长度校验(数据包长度取反   2个字节)
     * 6: 数据包ID  (1个字节)
     * 7: 保留字节(协议格式版本, v2.05-0x02, v3.08-0x05     1个字节)
     * ...: 数据内容(其长度为【数据包长度】– 4 – 2   不定长度)
     * 最后2字节: 校验和(【数据包长度】开始至全部【数据内容】结束止的所有字节产累加之和)
     * <p>
     * landu的ubi格式与普通landu格式一样不同的是命令字只有1603和1620，所以这里只要校验landu格式即可
     *
     * @param buffer 二进制数据包
     * @return
     */
    @Override
    public List<DataPack> extract(ByteBuf buffer) {
        // 长度大于2M的数据包直接抛弃(恶意数据)
        if (DISCARDS_MAX_LENGTH < buffer.readableBytes()) {
            //System.out.println("clear");
            buffer.clear();
            return null;
        }


        DataPack dataPack;
        List<DataPack> dataPackList = new ArrayList<>();

        int readIndex/*当前读指针*/, writeIndex/*当前写指针*/, packLenVal/*数据包长度字段值*/, packCheck/*数据包实际校验和*/, packCheckVal/*数据包校验和字段值*/;
        while (buffer.isReadable()) {
            readIndex = buffer.readerIndex();
            writeIndex = buffer.writerIndex();

            if (writeIndex - readIndex < 10) {// 一个包最小10个字节
                break;
            }

            // 判断数据包标志,检查头两个字节是否为 AA 55,不为AA 55不停往下跳过一字节直到头两个字节为 AA 55为止
            if (0xAA != (buffer.getByte(readIndex) & 0xFF) || 0x55 != (buffer.getByte(readIndex + 1) & 0xFF)) {
                buffer.skipBytes(1);
                continue;
            }

            // 获取校验数据包长度(第3、4个字节是数据包长度，第5、6个字节是第3、4个字节取反)
            if (buffer.getByte(readIndex + 2) != ~buffer.getByte(readIndex + 4)
                    || buffer.getByte(readIndex + 3) != ~buffer.getByte(readIndex + 5)) {//长度校验不通过，说明这里读到的是不完整的包
                buffer.skipBytes(2);//这里前面包头检查通过，所以跳过已校验的 AA 55 两个字节
                continue;
            }

            //获取数据包长度字段，数据包长度字段是从【数据包长度】起至【校验和】止的所有字节数量，不是整个包的长度
            packLenVal = (buffer.getByte(readIndex + 2) & 0xFF) << 8 | (buffer.getByte(readIndex + 3) & 0xFF);
            if (packLenVal > (writeIndex - readIndex - 2)) {//buffer中可读字节数小于数据包的长度
                //包长度不够，有以下两种情况
                // 1、可能是tcp拆包引起的半个包情况，直接跳出循环结束此次解析,等缓存区积累够了再解析
                // 2、包体里面含有 AA 55而且AA 55 后面的4个字节能通过数据包长度校验 导致解析出错,这种情况有可能会误伤后面的正常包，
                // 但这种情况概率极小，所以这里直接跳出循环结束此次解析，等缓存区积累够了再当成“正常包”解析，会在第二步解析不到数据直接丢弃
                break;
            }


            //校验和(最后两字节)验证
            packCheckVal = (buffer.getByte(readIndex + packLenVal) & 0xFF) << 8 | (buffer.getByte(readIndex + packLenVal + 1) & 0xFF);
            //计算实际校验和
            packCheck = 0;
            for (int i = readIndex + 2, n = readIndex + packLenVal; i < n; i++) {
                packCheck += (buffer.getByte(i) & 0xFF);
            }
            if (packCheck != packCheckVal) {
                buffer.skipBytes(2);//这里前面包头检查通过，所以跳过已校验的 AA 55 两个字节
                continue;
            }

            // 版本(第7个字节为协议格式版本)
            String version = null;
//            System.out.println("*********"+ByteBufUtil.hexDump(new byte[]{buffer.getByte(readIndex + 7)}));
            switch (buffer.getByte(readIndex + 7)) {//TODO 版本
                case 0x02:
                    version = "2.05";
                    break;
                case 0x05:
                    version = "3.08";
                    break;
                case 0x30:
                    version = "0x30";
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


        if (dataPackList.size() > 0) {
            return dataPackList;
        }

        return null;
    }

    @Override
    public ByteBuf createResponse(DataPack requestPack, ERespReason reason) {
        if (null == reason || ERespReason.OK != reason) {
            return null;
        }

        byte[] bytes = requestPack.getDataBytes();

        String defaultGBK = "GBK";
        DateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ByteBuf buffer = Unpooled.buffer(1024);

        // 1.数据包标志
        buffer.writeBytes(new byte[]{bytes[0], bytes[1]});
        // 2.数据包长度(预留空间)
        buffer.writeShort(0x0000);
        // 3.数据包长度校验(预留空间)
        buffer.writeShort(0xFFFF);
        // 4.数据包ID
        buffer.writeByte(bytes[6]);
        // 5.协议格式版本
        buffer.writeByte(bytes[7]);
        // 6.数据内容(命令字)
        buffer.writeBytes(new byte[]{bytes[8], bytes[9]});

        try {
            if (0x16 == bytes[8] && 0x03 == bytes[9]) {
            /* 命令字：0x1603 */
                // 7.回复内容
                // 7.1 当前时刻时间戮（【STRING】YYYY-MM-DD hh:mm:ss）
                //buffer.writeBytes("2017-08-04 15:50:00".getBytes(defaultCharsetGBK));
                buffer.writeBytes(defaultDateFormat.format(Calendar.getInstance().getTime()).getBytes(defaultGBK));
                buffer.writeByte(0x00); //end
                // 7.2 执行动作值（【动作参数数量】+【恢复出厂设置序号】+【是否执行清码动作】）
                buffer.writeByte(0x02); // 仅能取值 0x00 或 0x02,其它值非法
                buffer.writeByte(0x00);
                buffer.writeByte(0xFF);
                // 7.3 车辆信息（【车辆信息参数数量】+【VID】+【品牌】+【系列】+【年款】+【排量】）
                buffer.writeByte(0x00); // 仅可取值 0x00 或 0x05，其它值非法
                // 7.4 上传数据网络配置（【网络配置数量】+【网络配置 1】+...+【网络配置 n】）
                buffer.writeByte(0x00);
            /*buffer.writeByte(0x05);
            for (int i = 0; i < 5; i++) {
                buffer.writeBytes(ip.getBytes(defaultCharsetGBK));
                buffer.writeByte(0x00); //end
                buffer.writeByte((port >> 8) & 0xFF);
                buffer.writeByte(port & 0xFF);
            }*/
                // 7.5 车速分段统计设置（【分段数量】+【第 1 段最高车速】+…+【第 n 段最高车速】）
                buffer.writeByte(0x04); // 该值不得大于 10
                buffer.writeByte(1 & 0xFF);
                buffer.writeByte(45 & 0xFF);
                buffer.writeByte(90 & 0xFF);
                buffer.writeByte(255 & 0xFF);
                // 7.6 定位数据设置（【定位参数设置参数数量】+【定位间隔距离】+【定位间隔时间】+【距离与时间关系】）
                buffer.writeByte(0x00); // 仅可取值 0x00 或 0x03
                // 7.7 报警设置（【报警设置参数数量】+【超速最小车速】+【超速报警的最小持续时间】+【报警水温值】+【充电电压报警值】）
                buffer.writeByte(0x04); // 仅可取 0x00 或 0x04, 其它值非法
                buffer.writeByte(120 & 0xFF);
                buffer.writeByte((6 >> 8) & 0xFF); // 6
                buffer.writeByte(6 & 0xFF);
                buffer.writeByte((110 >> 8) & 0xFF); // 110
                buffer.writeByte(110 & 0xFF);
                buffer.writeByte(132 & 0xFF);
                // 7.8 熄火后数据设置（【熄火后数据数量】+【熄火后关闭时间点】+【关机临界电压值】+【熄火后电压设定】）
                buffer.writeByte(0x03); // 该值可取 0x00 或 0x03, 其它值非法
                buffer.writeByte((720 >> 8) & 0xFF); // 720
                buffer.writeByte(720 & 0xFF);
                buffer.writeByte(85 & 0xFF);
                buffer.writeByte((2 >> 8) & 0xFF); // 2
                buffer.writeByte(2 & 0xFF);
                buffer.writeByte(118 & 0xFF);
                buffer.writeByte(121 & 0xFF);
                // 7.9 运行中数据设置（【数据 ID 数量】+【【数据间隔时间】+【【数据 ID】…+】】）
                buffer.writeByte(0x02); // 参考协议示例，设置2个
                buffer.writeByte((300 >> 8) & 0xFF); // 300
                buffer.writeByte(300 & 0xFF);
                buffer.writeByte((511 >> 8) & 0xFF); // 511
                buffer.writeByte(511 & 0xFF);
                buffer.writeByte((255 >> 8) & 0xFF); // 255
                buffer.writeByte(255 & 0xFF);
                // 7.10 软件升级
                buffer.writeBytes("0.0.0".getBytes(defaultGBK));
                buffer.writeByte(0x00); //end

            } else {
            /* 命令字：非0x1603 */
                // 8.回复成功状态
                buffer.writeByte((byte) 0x00);
            }

            // 9.设置包长度和校验信息
            int length = buffer.readableBytes();
            buffer.setByte(2, (byte) ((length >> 8) & 0xFF));
            buffer.setByte(3, (byte) (length & 0xFF));
            buffer.setByte(4, (byte) ~buffer.getByte(2));
            buffer.setByte(5, (byte) ~buffer.getByte(3));

            // 10.校验和
            int sum = 0;
            for (int i = 2; i < buffer.readableBytes(); i++) {
                sum += (buffer.getByte(i) & 0xFF);
            }
            buffer.writeByte((byte) ((sum >> 8) & 0xFF));
            buffer.writeByte((byte) (sum & 0xFF));


            return buffer;
        } catch (UnsupportedEncodingException e) {

        }

        return null;
    }

    @Override
    public void destroyResponse(ByteBuf responseBuf) {
        if (null != responseBuf) {
            responseBuf.release();
        }
    }

    @Override
    public List<DataPackTarget> extractBody(DataPack dataPack) {
        byte[] data = dataPack.getDataBytes();
        if (!validateDataPackBytes(data)) {//包不合法
            return null;
        }


        ByteBuf dataBuf = Unpooled.wrappedBuffer(data);
        List<DataPackTarget> dataPackTargetList = new ArrayList<>();


        try {
            DataPackObject dataPackObject = new DataPackObject(dataPack);
            // 跳过“标志+长度+长度校验”6个字节
            dataBuf.skipBytes(6);
            // 数据包ID
            dataPackObject.setPackId(LanduDataPackUtil.readByte(dataBuf));

            //跳过版本读命令字
            dataBuf.skipBytes(1);
            int commandId = LanduDataPackUtil.readWord(dataBuf);//命令字
            switch (commandId) {
                case 0x1603:
                    System.out.println("## 0x1603 - 3.1.3 从服务器取得参数");
                    // 1.设备号
                    dataPackObject.setDeviceId(LanduDataPackUtil.readString(dataBuf));
                    // 2.TripID
                    dataPackObject.setTripId(LanduDataPackUtil.readDWord(dataBuf));
                    // 3.VID
                    dataPackObject.setVid(LanduDataPackUtil.readString(dataBuf));
                    // 4.VIN
                    dataPackObject.setVin(LanduDataPackUtil.readString(dataBuf));

                    // 5.上报设备信息
                    DataPackDevice dataPackDevice = new DataPackDevice(dataPackObject);
                    // 5.1 硬件版本号
                    dataPackDevice.setHardwareVersion(LanduDataPackUtil.readString(dataBuf));
                    // 5.2 固件版本号
                    dataPackDevice.setFirmwareVersion(LanduDataPackUtil.readString(dataBuf));
                    // 5.3 软件版本号
                    dataPackDevice.setSoftwareVersion(LanduDataPackUtil.readString(dataBuf));
                    // 5.4 诊断程序类型
                    dataPackDevice.setDiagnoseProgramType(LanduDataPackUtil.readByte(dataBuf));
                    // 5.5 恢复出厂设置序号
                    dataPackDevice.setInitCode(LanduDataPackUtil.readByte(dataBuf));
                    // --add
                    dataPackTargetList.add(new DataPackTarget(dataPackDevice));
                    break;

                case 0x1620:
                    // 1.设备号
                    dataPackObject.setDeviceId(LanduDataPackUtil.readString(dataBuf));
                    // 2.TripID
                    dataPackObject.setTripId(LanduDataPackUtil.readDWord(dataBuf));
                    // 3.VID
                    dataPackObject.setVid(LanduDataPackUtil.readString(dataBuf));
                    // 4.VIN
                    dataPackObject.setVin(LanduDataPackUtil.readString(dataBuf));
                    // 5.检测数据时间
                    dataPackObject.setDetectionTime(LanduDataPackUtil.readDate(dataBuf));


                    //6.0数据内容
                    ByteBuf contentBuf = dataBuf.slice(dataBuf.readerIndex(), dataBuf.writerIndex() - dataBuf.readerIndex() - 2);

                    contentBuf.markReaderIndex();
                    //6.1总数据项数
                    int totalItems = LanduDataPackUtil.readWord(contentBuf);
                    //6.2总字节数
                    int totalBytes = LanduDataPackUtil.readWord(contentBuf);
                    contentBuf.resetReaderIndex();

                    //6.3数据项列表
                    ByteBuf itemListBuf = contentBuf.slice(4, contentBuf.writerIndex() - 4);
                    int itemId/*数据项ID*/, itemLen/*数据项长度*/;
                    ByteBuf itemDataBuf;//数据值
                    while (itemListBuf.isReadable()) {//读取数据项
                        itemId = LanduDataPackUtil.readWord(itemListBuf);
                        itemLen = LanduDataPackUtil.readByte(itemListBuf);
                        itemDataBuf = itemListBuf.slice(itemListBuf.readerIndex(), itemLen);
                        itemListBuf.skipBytes(itemLen);

                        //TODO ADAS数据
                        switch (itemId) {
                            case 0x0A01://
                                int code = (int) LanduDataPackUtil.readUInt4(itemDataBuf);//前两字节为0
                                byte b0 = itemDataBuf.readByte();
                                byte b1 = itemDataBuf.readByte();
                                byte b2 = itemDataBuf.readByte();
                                byte b3 = itemDataBuf.readByte();
                                byte b4 = itemDataBuf.readByte();
                                byte b5 = itemDataBuf.readByte();
                                byte b6 = itemDataBuf.readByte();
                                byte b7 = itemDataBuf.readByte();

                                switch (code) {
                                    case 0x0700:
                                        //时间标识
                                        int timeIndicator = (b0 & 0b00011000) >>> 3;


                                        //•	Display sound type  显示声音类型
                                        int soundType = b0 & 0b00000111;


                                        //错误码
                                        if(0x0 == (b3 & 0b00000001)){//最后一位为0表示有错误
                                            int errorCode = (b3 & 0b11111110) >>> 1;
                                        }


                                        //Lane Departure Warning left and right 左右偏离车道报警
                                        if((b4 & 0b00000001) == 1){//最后一位表示是否开启车道报警
                                            boolean leftLDW = ((b4 & 0b00000010) >>> 1) == 1 ? true : false;
                                            boolean rightLDW = ((b4 & 0b00000100) >>> 2) == 1 ? true : false;
                                        }



                                        //Low Speed detection off  低速度检测


                                        //Headway in seconds 行驶速度
                                        boolean zeroSpeed = ((b1 & 0b00100000) >>> 5) == 1 ? true:false;
                                        boolean headWay = ((b2 & 0b11111110) >>> 1) == 1 ? true:false;


                                        //FCW前方碰撞预警
                                        boolean fcw = ((b4 & 0b00001000) >>> 3)  == 1 ? true:false;


                                        //Pedestrian detection and warning   行人碰撞预警
                                        boolean pedsDZ = ((b5 & 0b00000100) >>> 2) == 1 ? true:false;
                                        boolean pedsFCW = ((b5 & 0b00000010) >>> 1) == 1 ? true:false;



                                        //Hi/Low beam decision 强弱光束


                                        //HMW level 超速报警级别
                                        int HMWLevel = b7 & 0b00000011;


                                        //Failsafe events: Low visibility, Maintenance故障安全事件:低可见性，维护
                                        boolean failSafe = ((b4 & 0b10000000) >>> 7 ) == 1 ? true : false;


                                        //TSR Warning Level交通标志识别警告级别
                                        boolean tsrOn = ((b5 & 0b10000000) >>> 7 ) == 1 ? true : false;
                                        if(tsrOn){
                                            int tsrLevel = b6 & 0b00000111;
                                        }

                                        //Tamper Alert  警告篡改
                                        boolean tamperAlert = ((b5 & 0b00100000) >>> 5) == 1 ? true : false;
                                        break;



                                    case 0x0720:
                                        //Sign Type
                                        int signType = b0;
                                        //Supplementary Sign Type
                                        int supplementarySignType = b1;
                                        //Sign Position X
                                        int signPosX = b2;//Range: 0…122
                                        //Sign Position Y
                                        int signPosY = b3 & 0b01111111;//Range: -32… 31
                                        //Sign Position Z
                                        int signPosZ =  b4 & 0b00111111;//Range: -16… 16
                                        //Filter Type
                                        int filterType = b5;

                                        break;
                                    case 0x0721:
                                        break;
                                    case 0x0722:
                                        break;
                                    case 0x0723:
                                        break;
                                    case 0x0724:
                                        break;
                                    case 0x0725:
                                        break;
                                    case 0x0726:
                                        break;



                                    case 0x0727:
                                        int signTypeD1 = b0;
                                        int supplementarySignTypeD1 = b1;
                                        int signTypeD2 = b2;
                                        int supplementarySignTypeD2 = b3;
                                        int signTypeD3 = b4;
                                        int supplementarySignTypeD3 = b5;
                                        int signTypeD4 = b6;
                                        int supplementarySignTypeD4 = b7;


                                        break;



                                    case 0x0760:
                                        //	Left blink 左灯闪烁
                                        int leftBlink = (b0 & 0b00000010) >>> 1;//1 if left turn signal is on, 0 if off.

                                        //	Right Blink
                                        int rightBlink = (b0 & 0b00000100) >>> 2;//1 if right turn signal is on, 0 if off.


                                        //	Speed
                                        int speedAvailable = (b1 & 0b10000000) >>> 7 ;//1 if Speed available
                                        int speed = b2;//Unit: km/h

                                        //	Brakes 刹车
                                        int brakes = b0 & 0b00000001;//1 if right turn signal is on, 0 if off

                                        //	High beam 强光
                                        int highBeamAvailable = (b1 & 0b00100000) >>> 5;//1 if High Beam available
                                        int highBeam = (b0 & 0b00100000) >>> 5;//1 if High Beam on, 0 if off
                                        int lowBeamAvailable = (b1 & 0b00100000) >>> 4;// 1 if Low Beam available
                                        int lowBeam = (b0 & 0b00010000) >>> 4;//1 if Low Beam on, 0 if off.


                                        //	Wipers  雨刮
                                        int wipersAvailable = (b1 & 0b00001000) >>> 3;//1 if Wipers available
                                        int wipers = (b0 & 0b00001000) >>> 3;//1 when a Wiper passes the windshield, 0 if a wiper is static

                                        break;
                                }


                                break;
                        }


                        // TODO 补充的
                        int val;
                        switch (itemId) {
                            case 0x0000: //蓄电池电压
                                val = LanduDataPackUtil.readByte(itemDataBuf) / 10;
                                System.out.println(val);
                                break;
                            case 0x0004: //燃油系统1状态
                                val = LanduDataPackUtil.readByte(itemDataBuf);
                                System.out.println(val);

                                break;
                            case 0x0007: //发动机冷却液温度
                                val = LanduDataPackUtil.readByte(itemDataBuf);
                                System.out.println(val);
                                break;
                        }


                        //TODO 诊断数据 (文档无说明)


                        //TODO 车身数据


                        //TODO 定位数据


                        //TODO J1939数据(文档无说明)


                        //TOOD 其它数据
                        switch (itemId) {
                            case 0x0E00: //行程ID
                                break;
                            case 0x0E01://点火时间
                                break;
                            case 0x0E02://熄火时间
                                break;
                            case 0x0E03://急加速次数
                                break;
                            case 0x0E04://急减速次数
                                break;
                            case 0x0E05://急转弯次数
                                break;
                            case 0x0E06://行驶里程
                                break;

                        }

                    }

                    break;
                default:
                    ;
            }

            if (dataPackTargetList.size() > 0) {
                return dataPackTargetList;
            }

        } catch (UnsupportedEncodingException | ParseException e) {

        }

        return null;
    }

    @Override
    public Map<String, Object> getMetaData(ByteBuf buffer) {
        Map<String, Object> metaDataMap = new HashMap<>();

        buffer.markReaderIndex();

        // 跳过"标志+长度+校验+数据包ID"
        LanduDataPackUtil.readBytes(buffer, 7);

        // 0.协议版本
        String version = null;
        switch (LanduDataPackUtil.readByte(buffer)) {
            case 0x02:
                version = "2.05";
                break;
            case 0x05:
                version = "3.08";
                break;
            case 0x30:
                version = "0x30";
                break;
            default:
                version = "unknown";
        }
        metaDataMap.put("protocol", PROTOCOL_PREFIX + version);

        // 跳过"命令字"
        LanduDataPackUtil.readBytes(buffer, 2);

        try {
            // 1.设备号
            metaDataMap.put("deviceId", LanduDataPackUtil.readString(buffer));

            // 2.TripID
            //buffer.readUnsignedShort();
            LanduDataPackUtil.readDWord(buffer);

            // 3.VID
            LanduDataPackUtil.readString(buffer);

            // 4.VIN
            metaDataMap.put("vin", LanduDataPackUtil.readString(buffer));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 重置readerIndex为0
        buffer.resetReaderIndex();

        return metaDataMap;
    }


    /**
     * 验证DataPack对象携带数据是否合法
     *
     * @param bytes
     * @return
     */
    protected boolean validateDataPackBytes(byte[] bytes) {
        if (null == bytes || bytes.length < 10) {
            return false;
        }

        if (0xAA != (bytes[0] & 0xFF) || 0x55 != (bytes[1] & 0xFF)) {//判断数据包标志,检查头两个字节是否为 AA 55
            return false;
        }


        if (bytes[2] != ~bytes[4] || bytes[3] != ~bytes[5]) {//校验数据包长度(取反校验+数值校验)
            return false;
        }

        //验证数据包长度字段
        int packLen = (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
        if (packLen != (bytes.length - 2)) {
            return false;
        }


        //校验和(最后两字节)验证
        int packCheckVal = (bytes[bytes.length - 2] & 0xFF) << 8 | (bytes[bytes.length - 1] & 0xFF);
        //计算实际校验和
        int packCheck = 0;
        for (int i = 2, n = bytes.length - 2; i < n; i++) {
            packCheck += (bytes[i] & 0xFF);
        }
        if (packCheck != packCheckVal) {
            return false;
        }

        return true;
    }
}
