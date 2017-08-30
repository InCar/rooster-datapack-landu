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
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&commandId: "+commandId);
            switch (commandId) {
                case 0x1603:
                    //上报设备数据
                    DataPackDevice dataPackDevice;


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
                    dataPackDevice = new DataPackDevice(dataPackObject);
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


/****------------------------------------------------------------------1602 --------------------------------------------------------------------****/
                case 0x1620://除 上报设备数据 外，其余数据均在1602中
                    //报警数据
                    DataPackAlarm dataPackAlarm = new DataPackAlarm(dataPackObject);//报警数据
                    dataPackAlarm.setAlarmList(new LinkedList<>());
                    //车辆信号灯
                    DataPackCarSignals carSignals =null;

                    DataPackSignInfo signInfo = null;

                    DataPackSignType signTypeObj = null;

                    DataPackAlarm.Alarm alarm = null;
                    //位置数据
                    DataPackPosition position = null;
                    //整车数据
                    DataPackOverview overview = null;
                    //车身数据
                    DataPackCondition dataPackCondition = null;


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

                        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&itemId:"+itemId+",itemLen:"+itemLen);//TODO

/*-------------------------  ADAS数据------------------------------------------------------*/
                        switch (itemId) {
                            case 0x0A01://ADAS数据现在只有 0x0A01
                                int code = (int) LanduDataPackUtil.readUInt4(itemDataBuf);//前两字节为0,剩下8字节是数据区
                                byte b0 = itemDataBuf.readByte();
                                byte b1 = itemDataBuf.readByte();
                                byte b2 = itemDataBuf.readByte();
                                byte b3 = itemDataBuf.readByte();
                                byte b4 = itemDataBuf.readByte();
                                byte b5 = itemDataBuf.readByte();
                                byte b6 = itemDataBuf.readByte();
                                byte b7 = itemDataBuf.readByte();


                                System.out.println("&&&&&&&&&&&&&&&&& adas code:"+code);
                                switch (code) {
                                    case 0x0700://显示和报警数据
                                        //时间标识
                                        int timeIndicator = (b0 & 0b00011000) >>> 3;

                                        //•	Display sound type  显示声音类型
                                        int soundType = b0 & 0b00000111;


                                        //错误码
                                        if (0x0 == (b3 & 0b00000001)) {//最后一位为0表示有错误
                                            int errorCode = (b3 & 0b11111110) >>> 1;
                                        }


                                        //Lane Departure Warning left and right 左右偏离车道报警
                                        if ((b4 & 0b00000001) == 1) {//最后一位表示是否开启车道报警


                                            boolean leftLDW = ((b4 & 0b00000010) >>> 1) == 1 ? true : false;
                                            alarm = new DataPackAlarm.Alarm();
                                            alarm.setAlarmName("LEFTLDW");
                                            alarm.setAlarmValue("1");
                                            alarm.setAlarmDesc("左边偏离车道");

                                            dataPackAlarm.getAlarmList().add(alarm);

                                            boolean rightLDW = ((b4 & 0b00000100) >>> 2) == 1 ? true : false;
                                            alarm = new DataPackAlarm.Alarm();
                                            alarm.setAlarmName("RIGHTLDW");
                                            alarm.setAlarmValue("1");
                                            alarm.setAlarmDesc("右边偏离车道");

                                            dataPackAlarm.getAlarmList().add(alarm);
                                        }


                                        //Low Speed detection off  低速度检测


                                        //Headway in seconds 行驶速度
                                        boolean zeroSpeed = ((b1 & 0b00100000) >>> 5) == 1 ? true : false;
                                        boolean headWay = ((b2 & 0b11111110) >>> 1) == 1 ? true : false;


                                        //FCW前方碰撞预警
                                        boolean fcw = ((b4 & 0b00001000) >>> 3) == 1 ? true : false;
                                        if (fcw) {
                                            alarm = new DataPackAlarm.Alarm();
                                            alarm.setAlarmName("FCW");
                                            alarm.setAlarmValue("1");
                                            alarm.setAlarmDesc("前方碰撞预警");

                                            dataPackAlarm.getAlarmList().add(alarm);
                                        }


                                        //Pedestrian detection and warning   行人碰撞预警
                                        boolean pedsDZ = ((b5 & 0b00000100) >>> 2) == 1 ? true : false;
                                        alarm = new DataPackAlarm.Alarm();
                                        alarm.setAlarmName("PedsDZ");
                                        alarm.setAlarmValue("1");
                                        alarm.setAlarmDesc("前方危险区域预警");
                                        dataPackAlarm.getAlarmList().add(alarm);

                                        boolean pedsFCW = ((b5 & 0b00000010) >>> 1) == 1 ? true : false;
                                        alarm = new DataPackAlarm.Alarm();
                                        alarm.setAlarmName("PedsFCW");
                                        alarm.setAlarmValue("1");
                                        alarm.setAlarmDesc("行人前方碰撞预警");
                                        dataPackAlarm.getAlarmList().add(alarm);


                                        //Hi/Low beam decision 强弱光束


                                        //HMW level 超速报警级别
                                        int HMWLevel = b7 & 0b00000011;
                                        alarm = new DataPackAlarm.Alarm();
                                        alarm.setAlarmName("HMW");
                                        alarm.setAlarmValue(HMWLevel + "");
                                        alarm.setAlarmDesc("超速报警");
                                        dataPackAlarm.getAlarmList().add(alarm);


                                        //Failsafe events: Low visibility, Maintenance故障安全事件:低可见性，维护
                                        boolean failSafe = ((b4 & 0b10000000) >>> 7) == 1 ? true : false;


                                        //TSR Warning Level交通标志识别警告级别
                                        boolean tsrOn = ((b5 & 0b10000000) >>> 7) == 1 ? true : false;
                                        if (tsrOn) {
                                            int tsrLevel = b6 & 0b00000111;

                                            alarm = new DataPackAlarm.Alarm();
                                            alarm.setAlarmName("TSR");
                                            alarm.setAlarmValue(tsrLevel + "");
                                            alarm.setAlarmDesc("交通标志识别警告");
                                            dataPackAlarm.getAlarmList().add(alarm);
                                        }

                                        //Tamper Alert  警告篡改
                                        boolean tamperAlert = ((b5 & 0b00100000) >>> 5) == 1 ? true : false;
                                        break;

                                    case 0x0760://车辆信号
                                        //	Left Signal 左灯闪烁
                                        int leftSignal = (b0 & 0b00000010) >>> 1;//1 if left turn signal is on, 0 if off.

                                        //	Right Signal
                                        int rightSignal = (b0 & 0b00000100) >>> 2;//1 if right turn signal is on, 0 if off.


                                        //	Speed
                                        int speedAvailable = (b1 & 0b10000000) >>> 7;//1 if Speed available
                                        int speed = b2;//Unit: km/h

                                        //	Brakes 刹车
                                        int brakes = b0 & 0b00000001;//1 if right turn signal is on, 0 if off

                                        //	High beam 远光
                                        int highBeamAvailable = (b1 & 0b00100000) >>> 5;//1 if High Beam available
                                        int highBeam = (b0 & 0b00100000) >>> 5;//1 if High Beam on, 0 if off
                                        int lowBeamAvailable = (b1 & 0b00100000) >>> 4;// 1 if Low Beam available
                                        int lowBeam = (b0 & 0b00010000) >>> 4;//1 if Low Beam on, 0 if off.


                                        //	Wipers  雨刮
                                        int wipersAvailable = (b1 & 0b00001000) >>> 3;//1 if Wipers available
                                        int wipers = (b0 & 0b00001000) >>> 3;//1 when a Wiper passes the windshield, 0 if a wiper is static

                                        carSignals = new DataPackCarSignals(dataPackObject);

                                        carSignals.setRightSignal(rightSignal);
                                        carSignals.setLeftSignal(leftSignal);

                                        carSignals.setSpeendAvailable(speedAvailable);
                                        carSignals.setSpeend(speed);

                                        carSignals.setBrakeSignal(brakes);

                                        carSignals.setHightBeamAvailable(highBeamAvailable);
                                        carSignals.setHightBeam(highBeam);
                                        carSignals.setLowBeamAvailable(lowBeamAvailable);
                                        carSignals.setLowBeam(lowBeam);
                                        carSignals.setWipersAvailable(wipersAvailable);
                                        carSignals.setWipers(wipers);
                                        break;


                                    case 0x0720://TSR message - Sign Type and Position
                                    case 0x0721:
                                    case 0x0722:
                                    case 0x0723:
                                    case 0x0724:
                                    case 0x0725:
                                    case 0x0726:



                                        //Sign Type
                                        int signType = b0;
                                        //Supplementary Sign Type
                                        int supplementarySignType = b1;
                                        //Sign Position X
                                        int signPosX = b2;//Range: 0…122


                                        /**
                                         * 7位补码和6位补码转8位补码，正数前加0、负数前加1
                                         * 同理8位补码转6位补码(不超过6位数值范围)，正数减去前面0，负数减去前面1
                                         */
                                        //Sign Position Y，Range: -32… 31
                                        int signPosY = ((byte) (((byte) (b3 & 0b01111111)) << 1)) >> 1;//左移一位再有符号右移一位
                                        //Sign Position Z，-16… 16
                                        int signPosZ = ((byte) (((byte) (b4 & 0b01111111)) << 2)) >> 2;//左移2位再有符号右移2位
                                        //Filter Type
                                        int filterType = b5;


                                        signInfo = new DataPackSignInfo(dataPackObject);
                                        signInfo.setSignType(signType);
                                        signInfo.setSupplementarySignType(supplementarySignType);
                                        signInfo.setSignPosX(signPosX);
                                        signInfo.setSignPosY(signPosY);
                                        signInfo.setSignPosZ(signPosZ);
                                        signInfo.setFilterType(filterType);
                                        break;


                                    case 0x0727://Sign Type

                                        int signTypeD1 = b0;
                                        int supplementarySignTypeD1 = b1;
                                        int signTypeD2 = b2;
                                        int supplementarySignTypeD2 = b3;
                                        int signTypeD3 = b4;
                                        int supplementarySignTypeD3 = b5;
                                        int signTypeD4 = b6;
                                        int supplementarySignTypeD4 = b7;

                                        signTypeObj = new DataPackSignType(dataPackObject);
                                        signTypeObj.setSignTypeD1(signTypeD1);
                                        signTypeObj.setSupplementarySignTypeD1(supplementarySignTypeD1);
                                        signTypeObj.setSignTypeD2(signTypeD2);
                                        signTypeObj.setSupplementarySignTypeD2(supplementarySignTypeD2);
                                        signTypeObj.setSignTypeD3(signTypeD3);
                                        signTypeObj.setSupplementarySignTypeD3(supplementarySignTypeD3);
                                        signTypeObj.setSignTypeD4(signTypeD4);
                                        signTypeObj.setSupplementarySignTypeD4(supplementarySignTypeD4);

                                        break;

                                }


                                break;
                        }
/*----------------------------------------- end ADAS数据------------------------------------------------------*/


                        //TODO 诊断数据 (文档无说明)


/*-------------------------------------------------- 车身数据---------------------------------------------------------------------*/
                        if (null == dataPackCondition && itemId >= 0x0400 && itemId <= 0x04FF) {
                            dataPackCondition = new DataPackCondition(dataPackObject);
                            dataPackCondition.setConditionList(new LinkedList<>());
                        }
                        switch (itemId) {
                            case 0x0400://钥匙状态 + 点火开关
                                byte b = itemDataBuf.readByte();

                                int keyCdtVal = (b & 0b11110000) >>> 4;
                                int fireSwithCdtVal = b & 0b00001111;
                                Map<String,Integer> valMap0 = new HashMap<>();
                                valMap0.put("key",keyCdtVal);
                                valMap0.put("fireSwith",fireSwithCdtVal);

                                DataPackCondition.CarCondition<Map<String,Integer>> keyFireSwithCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_FIRESWITCH,
                                        valMap0,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_FIRESWITCH),new byte[]{b});
                                dataPackCondition.getConditionList().add(keyFireSwithCdt);
                                break;
                            case 0x0401://档位状态
                                byte b1 = itemDataBuf.readByte();

                                int level1Gear = (b1 & 0b11110000) >>> 4;
                                int level2Gear = b1 & 0b00001111;

                                Map<String,Integer> valMap1 = new HashMap<>();
                                valMap1.put("level1Gear",level1Gear);
                                valMap1.put("level2Gear",level2Gear);


                                DataPackCondition.CarCondition<Map<String,Integer>> gearCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_GEARSTATUS,
                                        valMap1,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_GEARSTATUS),new byte[]{b1});
                                dataPackCondition.getConditionList().add(gearCdt);
                                break;
                            case 0x0402://防盗监控
                                byte b2 = itemDataBuf.readByte();
                                DataPackCondition.CarCondition<Integer> antiTheftCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_ANTI_THEFT_MONITOR,
                                        b2 & 0b00001111,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_ANTI_THEFT_MONITOR),new byte[]{b2});
                                dataPackCondition.getConditionList().add(antiTheftCdt);
                                break;
                            case 0x0403://档位状态（吉利金刚）
                                byte  b3 = itemDataBuf.readByte();
                                DataPackCondition.CarCondition<Integer> geelyJgCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_GEARSTATUS_GEELY_JG,
                                        b3 & 0xFF,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_GEARSTATUS_GEELY_JG),new byte[]{b3});
                                dataPackCondition.getConditionList().add(geelyJgCdt);
                                break;
                            case 0x0410://车锁
                                byte [] b10 = new byte[2];
                                itemDataBuf.readBytes(b10);

                                Map<String,Integer> valMap10 = new HashMap<>();

                                int carLock = b10[0] & 0b00000011;//车锁（0-未锁，1-已上锁）
                                int leftFrontDoorLock = (b10[0] & 0b00001100) >>> 2;//左前门锁
                                int rightFrontDoorLock = (b10[0] & 0b00110000) >>> 4;//右前门锁
                                int leftRearDoorLock =  (b10[0] & 0b11000000) >>> 6;//左后门锁

                                int rightRearDoorLock = b10[1] & 0b00000011;//右后门锁

                                valMap10.put("carLock",carLock);
                                valMap10.put("leftFrontDoorLock",leftFrontDoorLock);
                                valMap10.put("rightFrontDoorLock",rightFrontDoorLock);
                                valMap10.put("leftRearDoorLock",leftRearDoorLock);
                                valMap10.put("rightRearDoorLock",rightRearDoorLock);


                                DataPackCondition.CarCondition<Map<String,Integer>> lockCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_LOCK,
                                        valMap10,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_LOCK),b10);
                                dataPackCondition.getConditionList().add(lockCdt);
                                break;
                            case 0x0411://车门
                                byte [] b11 = new byte[2];
                                itemDataBuf.readBytes(b11);

                                Map<String,Integer> valMap11 = new HashMap<>();

                                int carDoor = b11[0] & 0b00000011;//车门总体状态（0-关，1-开）
                                int leftFrontDoor = (b11[0] & 0b00001100) >>> 2;//左前门
                                int rightFrontDoor = (b11[0] & 0b00110000) >>> 4;//右前门
                                int leftRearDoor =  (b11[0] & 0b11000000) >>> 6;//左后门

                                int rightRearDoor = b11[1] & 0b00000011;//右后门
                                int trunkDoor = (b11[1] & 0b00001100)>>>2;//后备箱/尾门
                                int engineHood = (b11[1] & 0b00110000)>>>4;//引擎盖
                                int fuelTankCap = (b11[1] & 0b11000000)>>>6;//油箱盖

                                valMap11.put("carDoor",carDoor);
                                valMap11.put("leftFrontDoor",leftFrontDoor);
                                valMap11.put("rightFrontDoor",rightFrontDoor);
                                valMap11.put("leftRearDoor",leftRearDoor);
                                valMap11.put("rightRearDoor",rightRearDoor);
                                valMap11.put("trunkDoor",trunkDoor);
                                valMap11.put("engineHood",engineHood);
                                valMap11.put("fuelTankCap",fuelTankCap);

                                DataPackCondition.CarCondition<Map<String,Integer>> doorCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_DOOR,
                                        valMap11,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_DOOR),b11);
                                dataPackCondition.getConditionList().add(doorCdt);
                                break;
                            case 0x0412://车灯
                                byte[] b12 = new byte[3];
                                itemDataBuf.readBytes(b12);

                                Map<String,Integer> valMap12 = new HashMap<>();
                                int carLight  = b12[0] & 0b00000011;//车灯总体状态（0-关，1-开）
                                int warmLight = (b12[0] & 0b00001100) >>> 2;//示警灯/双闪（0-关，1-开）
                                int turnLight = (b12[0] & 0b00110000) >>> 4;//转向灯
                                int dayRunLight =  (b12[0] & 0b11000000) >>> 6;//日间行车灯

                                int widthLight = b12[1] & 0b00000011;//示宽灯
                                int nearLight = (b12[1] & 0b00001100)>>>2;//近光灯
                                int farLight = (b12[1] & 0b00110000)>>>4;//远光灯
                                int frontFogLight = (b12[1] & 0b11000000)>>>6;//前雾灯

                                int backFogLight = b12[2] & 0b00000011;//后雾灯
                                int brakeLight = (b12[2] & 0b00001100)>>>2;//刹车灯

                                valMap12.put("carLight",carLight);
                                valMap12.put("warmLight",warmLight);
                                valMap12.put("turnLight",turnLight);
                                valMap12.put("dayRunLight",dayRunLight);
                                valMap12.put("widthLight",widthLight);
                                valMap12.put("nearLight",nearLight);
                                valMap12.put("farLight",farLight);
                                valMap12.put("frontFogLight",frontFogLight);
                                valMap12.put("backFogLight",backFogLight);
                                valMap12.put("brakeLight",brakeLight);


                                DataPackCondition.CarCondition<Map<String,Integer>> lightCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_LIGHT,
                                        valMap12,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_LIGHT),b12);
                                dataPackCondition.getConditionList().add(lightCdt);
                                break;
                            case 0x0413://故障灯
                                byte b13 = itemDataBuf.readByte();
                                Map<String,Integer> valMap13 = new HashMap<>();
                                int tpmsTyreLight  = b13 & 0b00000011;//TPMS轮胎灯
                                int esStopLight = (b13 & 0b00001100) >>> 2;//ES制动灯

                                valMap13.put("tpmsTyreLight",tpmsTyreLight);
                                valMap13.put("esStopLight",esStopLight);


                                DataPackCondition.CarCondition<Map<String,Integer>> trobleLightCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_TROUBLE_LIGHT,
                                        valMap13,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_TROUBLE_LIGHT), new byte[]{b13} );
                                dataPackCondition.getConditionList().add(trobleLightCdt);

                                break;
                            case 0x0414://车窗状态
                                byte [] b14 = new byte[2];
                                itemDataBuf.readBytes(b14);

                                Map<String,Integer> valMap14 = new HashMap<>();

                                int leftFrontWindow = b14[0] & 0b00000011;//左前窗（0-关窗，1-开窗，2-升窗）
                                int rightFrontWindow = (b14[0] & 0b00001100) >>> 2;//左前窗
                                int leftRearWindow = (b14[0] & 0b00110000) >>> 4;//右前窗
                                int rightRearWindow =  (b14[0] & 0b11000000) >>> 6;//左后窗

                                int topWidow = b14[1] & 0b00000011;//天窗
                                int backWidow = (b14[1] & 0b00001100)>>>2;//尾窗

                                valMap14.put("leftFrontWindow",leftFrontWindow);
                                valMap14.put("rightFrontWindow",rightFrontWindow);
                                valMap14.put("leftRearWindow",leftRearWindow);
                                valMap14.put("rightRearWindow",rightRearWindow);
                                valMap14.put("topWidow",topWidow);
                                valMap14.put("backWidow",backWidow);

                                DataPackCondition.CarCondition<Map<String,Integer>> windowCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_WINDOW,
                                        valMap14,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_WINDOW),b14);
                                dataPackCondition.getConditionList().add(windowCdt);
                                break;
                            case 0x0415://安全带
                                byte [] b15 = new byte[2];
                                itemDataBuf.readBytes(b15);

                                Map<String,Integer> valMap15 = new HashMap<>();

                                int driverSafetyBbelt = b15[0] & 0b00000011;//主驾驶安全带（0-未系，1-已系）
                                int viceDriverSafetyBbelt = (b15[0] & 0b00001100) >>> 2;//副驾驶安全带
                                int leftRearSafetyBbelt = (b15[0] & 0b00110000) >>> 4;//后排左安全带
                                int midRearSafetyBbelt =  (b15[0] & 0b11000000) >>> 6;//后排中安全带

                                int rightRearSafetyBbelt = b15[1] & 0b00000011;//后排右安全带

                                valMap15.put("driverSafetyBbelt",driverSafetyBbelt);
                                valMap15.put("viceDriverSafetyBbelt",viceDriverSafetyBbelt);
                                valMap15.put("leftRearSafetyBbelt",leftRearSafetyBbelt);
                                valMap15.put("midRearSafetyBbelt",midRearSafetyBbelt);
                                valMap15.put("rightRearSafetyBbelt",rightRearSafetyBbelt);

                                DataPackCondition.CarCondition<Map<String,Integer>> safetyBeltCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_SAFETY_BELT,
                                        valMap15,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_SAFETY_BELT),b15);
                                dataPackCondition.getConditionList().add(safetyBeltCdt);
                                break;
                            case 0x0416://空调
                                byte b16 = itemDataBuf.readByte();
                                itemDataBuf.readBytes(b16);

                                Map<String,Integer> valMap16 = new HashMap<>();
                                int airConditionerRunStatus = b16 & 0b00000011;//运行状态 （0-关，1-开）
                                int airConditionerAirFanStatus = (b16 & 0b00001100) >>> 2 ; //风扇状态


                                valMap16.put("airConditionerRunStatus",airConditionerRunStatus);
                                valMap16.put("airConditionerAirFanStatus",airConditionerAirFanStatus);

                                DataPackCondition.CarCondition<Map<String,Integer>> airConditionerCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_AIR_CONDITIONER,
                                        valMap16,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_AIR_CONDITIONER), new byte[]{b16});
                                dataPackCondition.getConditionList().add(airConditionerCdt);

                                break;


                            case 0x0420://其它
                                byte [] b20 = new byte[2];
                                itemDataBuf.readBytes(b20);

                                Map<String,Integer> valMap20 = new HashMap<>();

                                int carPlayer = b20[0] & 0b00000011;//车载影音（0-关，1-开）
                                int seatHeat = (b20[0] & 0b00001100) >>> 2;//座椅加热（0-关，1-开）
                                int rearviewMirror = (b20[0] & 0b00110000) >>> 4;//后视镜子（0-关，1-开）
                                int reverseMonitor =  (b20[0] & 0b11000000) >>> 6;//可视倒车屏（0-关，1-开）

                                int remoteControlSound = b20[1] & 0b00000011;//遥控器鸣笛（0-静，1-响）
                                int driverType = (b20[1] & 0b00001100) >>> 2;//驱动类型（0-手动，1-自动）
                                int cruiseControlBtn = (b20[1] & 0b00110000) >>> 4;//定速巡航按钮（0-关，1-开）
                                int clutchStatus =  (b20[1] & 0b11000000) >>> 6;//离合器（0-离，1-合）

                                valMap20.put("carPlayer",carPlayer);
                                valMap20.put("seatHeat",seatHeat);
                                valMap20.put("rearviewMirror",rearviewMirror);
                                valMap20.put("reverseMonitor",reverseMonitor);
                                valMap20.put("remoteControlSound",remoteControlSound);
                                valMap20.put("driverType",driverType);
                                valMap20.put("cruiseControlBtn",cruiseControlBtn);
                                valMap20.put("clutchStatus",clutchStatus);

                                DataPackCondition.CarCondition<Map<String,Integer>> otherCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_OTHER,
                                        valMap20,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_OTHER),b20);
                                dataPackCondition.getConditionList().add(otherCdt);
                                break;
                            case 0x0440://剩余油量
                                byte [] b40 = new byte[2];
                                itemDataBuf.readBytes(b40);


                                int fuelCapacityUnit = (b40[1] & 0b10000000) >>> 7;//单位标识位，0-百分比模式，单位0.1%；1- 0.1L
                                int fuelCapacity = (b40[1] & 0b01111111) << 8  | (b40[0] & 0xFF);//油量值

                                Map<String,Integer> valMap40 = new HashMap<>();
                                valMap40.put("fuelCapacityUnit",fuelCapacityUnit);
                                valMap40.put("fuelCapacity",fuelCapacity);

                                DataPackCondition.CarCondition<Map<String,Integer>> fuelCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_OIL_REMAIN,
                                        valMap40,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_OIL_REMAIN),b40);
                                dataPackCondition.getConditionList().add(fuelCdt);
                                break;
                            case 0x0441://仪表里程 单位 ：公里
                                byte [] b41 = new byte[2];
                                itemDataBuf.readBytes(b41);
                                int odometerNum = (b41[0] & 0xFF) << 8 | (b41[1] & 0xFF);
                                DataPackCondition.CarCondition<Integer> odometerNumCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_ODOMETER_NUM,
                                        odometerNum,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_ODOMETER_NUM),b41);
                                dataPackCondition.getConditionList().add(odometerNumCdt);
                                break;
                            case 0x0442://VIN码
                                byte[] b42 = new byte[18];
                                itemDataBuf.readBytes(b42);
                                String vin = new String(b42,0,17,"UTF-8");
                                System.out.println("%%%%%%%%%%vin="+vin);

                                DataPackCondition.CarCondition<String> vinCdt = new DataPackCondition.CarCondition<String>(DataPackCondition.CONDITIONNAME_VIN,
                                        vin,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_VIN),b42);
                                dataPackCondition.getConditionList().add(vinCdt);
                                break;
                            case 0x0443://转速  单位 ：RPM（转/分）
                                byte[] b43 = new byte[2];
                                itemDataBuf.readBytes(b43);
                                int rotateSpeed = (b43[0] &  0xFF) << 8 | (b43[1] & 0xFF);

                                DataPackCondition.CarCondition<Integer> rotateSpeedCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_ROTATE_SPEED,
                                        rotateSpeed,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_ROTATE_SPEED),b43);
                                dataPackCondition.getConditionList().add(rotateSpeedCdt);
                                break;
                            case 0x0444://车速 单位 ：10m/h （10米/小时）
                                byte [] b44 = new byte[2];
                                itemDataBuf.readBytes(b44);
                                int speed = (b44[0] &  0xFF) << 8 | (b44[1] & 0xFF);

                                DataPackCondition.CarCondition<Integer> speedCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_SPEED,
                                        speed,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_SPEED),b44);
                                dataPackCondition.getConditionList().add(speedCdt);
                                break;
                            case 0x0445://续航里程   单位 ：公里
                                byte [] b45 = new byte[2];
                                itemDataBuf.readBytes(b45);
                                int nedc = (b45[0] &  0xFF) << 8 | (b45[1] & 0xFF);

                                DataPackCondition.CarCondition<Integer> nedcCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_NEDC,
                                        nedc,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_NEDC),b45);
                                dataPackCondition.getConditionList().add(nedcCdt);
                                break;
                            case 0x0446://平均油耗
                                byte [] b46 = new byte[2];
                                itemDataBuf.readBytes(b46);


                                int aveFuelConsumptionUnit = (b46[1] & 0b10000000) >>> 7;//单位标识位，0-升/时（L/H）；1-升/十公里（L/10KM）
                                int aveFuelConsumption = (b46[1] & 0b01111111) << 8  | (b46[0] & 0xFF);//油量值

                                Map<String,Integer> valMap46 = new HashMap<>();
                                valMap46.put("aveFuelConsumptionUnit",aveFuelConsumptionUnit);
                                valMap46.put("fuelCapacity",aveFuelConsumption);

                                DataPackCondition.CarCondition<Map<String,Integer>> aveFuelConsumptionCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_AVE_FUEL_CONSUMPTION,
                                        valMap46,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_AVE_FUEL_CONSUMPTION),b46);
                                dataPackCondition.getConditionList().add(aveFuelConsumptionCdt);
                                break;
                            case 0x0447://瞬时油耗
                                byte [] b47 = new byte[2];
                                itemDataBuf.readBytes(b47);


                                int unit47 = (b47[1] & 0b10000000) >>> 7;//单位标识位，0-升/时（L/H）；1-升/十公里（L/10KM）
                                int instantFuelConsumption = (b47[1] & 0b01111111) << 8  | (b47[0] & 0xFF);//油量值

                                Map<String,Integer> valMap47 = new HashMap<>();
                                valMap47.put("unit",unit47);
                                valMap47.put("fuelCapacity",instantFuelConsumption);

                                DataPackCondition.CarCondition<Map<String,Integer>> instantFuelConsumptionCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_INSTANT_FUEL_CONSUMPTION,
                                        valMap47,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_INSTANT_FUEL_CONSUMPTION),b47);
                                dataPackCondition.getConditionList().add(instantFuelConsumptionCdt);
                                break;
                            case 0x0448://行驶时间  单位 ：分
                                byte [] b48 = new byte[2];
                                itemDataBuf.readBytes(b48);
                                int runTime = (b48[0] &  0xFF) << 8 | (b48[1] & 0xFF);

                                DataPackCondition.CarCondition<Integer> runTimeCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_RUN_TIME,
                                        runTime,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_RUN_TIME),b48);
                                dataPackCondition.getConditionList().add(runTimeCdt);
                                break;
                            case 0x044A://方向盘转角 单位 ：0.1度（-5400，+ 5400）
                                byte [] b4A = new byte[2];
                                itemDataBuf.readBytes(b4A);
                                int wheelCorner = ((b4A[0] & 0xFF) << 8 | (b4A[1] & 0xFF)) << 16 >>16 ;//先转为int，再左移16位后带符号右移16位

                                DataPackCondition.CarCondition<Integer> wheelCornerCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_WHEEL_CORNER,
                                        wheelCorner,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_WHEEL_CORNER),b4A);
                                dataPackCondition.getConditionList().add(wheelCornerCdt);
                                break;
                            case 0x044B:// 轮速   单位 ：10m/h（10米/小时）
                                byte[] b4B = new byte[8];
                                itemDataBuf.readBytes(b4B);
                                int leftFrontTyreSpeed = (b4B[0] & 0xFF) << 8 | (b4B[1] & 0xFF);
                                int rightFrontTyreSpeed = (b4B[2] & 0xFF) << 8 | (b4B[3] & 0xFF);
                                int leftRearTyreSpeed = (b4B[4] & 0xFF) << 8 | (b4B[5] & 0xFF);
                                int rightRearTyreSpeed = (b4B[6] & 0xFF) << 8 | (b4B[7] & 0xFF);

                                Map<String,Integer> valMap4B = new HashMap<>();
                                valMap4B.put("leftFrontTyreSpeed",leftFrontTyreSpeed);
                                valMap4B.put("rightFrontTyreSpeed",rightFrontTyreSpeed);
                                valMap4B.put("leftRearTyreSpeed",leftRearTyreSpeed);
                                valMap4B.put("rightRearTyreSpeed",rightRearTyreSpeed);


                                DataPackCondition.CarCondition<Map<String,Integer>> typeSpeedCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_TYRE_SPEED,
                                        valMap4B,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_TYRE_SPEED),b4B);
                                dataPackCondition.getConditionList().add(typeSpeedCdt);
                                break;
                            case 0x044D://TODO??? 胎压   单位 ：0.1bar
                                byte [] b4D = new  byte[4];
                                int leftFrontTyrePressure =  b4D[0];
                                int rightFrontTyrePressure =  b4D[1];
                                int leftRearTyrePressure =  b4D[2];
                                int rightRearTyrePressure =  b4D[3];

                                Map<String,Integer> valMap4D = new HashMap<>();
                                valMap4D.put("leftFrontTyrePressure",leftFrontTyrePressure);
                                valMap4D.put("rightFrontTyrePressure",rightFrontTyrePressure);
                                valMap4D.put("leftRearTyrePressure",leftRearTyrePressure);
                                valMap4D.put("rightRearTyrePressure",rightRearTyrePressure);


                                DataPackCondition.CarCondition<Map<String,Integer>> typePressureCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_TYRE_PRESSURE,
                                        valMap4D,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_TYRE_PRESSURE),b4D);
                                dataPackCondition.getConditionList().add(typePressureCdt);
                                break;


                            case 0x044E://动力电流  单位 ：0.1A
                                byte [] b4E = new byte[2];
                                itemDataBuf.readBytes(b4E);
                                int powerElectricity = (b4E[0] &  0xFF) << 8 | (b4E[1] & 0xFF);

                                DataPackCondition.CarCondition<Integer> powerElectricityVoltageCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_POWER_ELECTRICITY,
                                        powerElectricity,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_POWER_ELECTRICITY),b4E);
                                dataPackCondition.getConditionList().add(powerElectricityVoltageCdt);

                                break;

                            case 0x044F://电池电压  单位 ：0.1V
                                byte [] b4F = new byte[2];
                                itemDataBuf.readBytes(b4F);
                                int batteryVoltage = (b4F[0] &  0xFF) << 8 | (b4F[1] & 0xFF);

                                DataPackCondition.CarCondition<Integer> batteryVoltageCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_BATTERY_VOLTAGE,
                                        batteryVoltage,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_BATTERY_VOLTAGE),b4F);
                                dataPackCondition.getConditionList().add(batteryVoltageCdt);
                                break;
                            case 0x0470://剩余电量  百分比（单位：1）
                                byte b70 = itemDataBuf.readByte();
                                int electricRemian = b70;
                                DataPackCondition.CarCondition<Integer> electricRemianCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_ELECTRIC_REMIAN,
                                        electricRemian,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_ELECTRIC_REMIAN),new byte[]{b70});
                                dataPackCondition.getConditionList().add(electricRemianCdt);
                                break;
                            case 0x0471://油门位置  百分比（单位：1）
                                byte b71 = itemDataBuf.readByte();
                                int gaunPosition = b71;
                                DataPackCondition.CarCondition<Integer> gaunPositionCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_GAUN_POSITION,
                                        gaunPosition,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_GAUN_POSITION),new byte[]{b71});
                                dataPackCondition.getConditionList().add(gaunPositionCdt);
                                break;
                            case 0x0472://尾门开度  百分比（单位：1）
                                byte b72 = itemDataBuf.readByte();
                                int tailOpen = b72;
                                DataPackCondition.CarCondition<Integer> tailOpenCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_TAIL_OPEN,
                                        tailOpen,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_TAIL_OPEN),new byte[]{b72});
                                dataPackCondition.getConditionList().add(tailOpenCdt);
                                break;
                            case 0x0473://刹车踏板力度  百分比（单位：1）
                                byte [] b73 = new byte[2];
                                itemDataBuf.readBytes(b73);
                                int brakeForce1 = b73[0];
                                int brakeForce2 = b73[1];
                                Map<String,Integer> valMap73 = new HashMap<>(2);
                                valMap73.put("brakeForce1",brakeForce1);
                                valMap73.put("brakeForce2",brakeForce2);

                                DataPackCondition.CarCondition<Map<String,Integer>> brakeForceCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_BRAKE_FORCE,
                                        valMap73,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_BRAKE_FORCE),b73);
                                dataPackCondition.getConditionList().add(brakeForceCdt);
                                break;
                            case 0x0474://节气门开度 百分比（单位：1）
                                byte b74 = itemDataBuf.readByte();
                                int throttleValve = b74;
                                DataPackCondition.CarCondition<Integer> throttleValveCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_THROTTLE_VALVE,
                                        throttleValve,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_THROTTLE_VALVE),new byte[]{b74});
                                dataPackCondition.getConditionList().add(throttleValveCdt);
                                break;

                            case 0x0480://刹车踏板状态   0-松开，1-踩下，2-点刹，3-急刹
                                byte b80 = itemDataBuf.readByte();
                                int brake1Status = (b80 & 0b11110000) >>> 4;
                                int brake2Status = b80 & 0b00001111;
                                Map<String,Integer> valMap80 = new HashMap<>(2);
                                valMap80.put("brake1Status",brake1Status);
                                valMap80.put("brake2Status",brake2Status);

                                DataPackCondition.CarCondition<Map<String,Integer>> brakeStatusCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_BRAKE_STATUS,
                                        valMap80,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_BRAKE_STATUS),new byte []{b80});
                                dataPackCondition.getConditionList().add(brakeStatusCdt);
                                break;
                            case 0x0481://助刹状态 0-松开， 1-脚刹踩下、手刹拉起、电子按下
                                byte b81 = itemDataBuf.readByte();
                                int viceBrakeStatus = b81 & 0b00001111;
                                DataPackCondition.CarCondition<Integer> viceBrakeStatusCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_VICE_BRAKE_STATUS,
                                        viceBrakeStatus,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_VICE_BRAKE_STATUS),new byte []{b81});
                                dataPackCondition.getConditionList().add(viceBrakeStatusCdt);
                                break;
                            case 0x0482://油门状态  0-松开，1-踩下
                                byte b82 = itemDataBuf.readByte();
                                int acceleratorStatus = b82 & 0b00001111;
                                DataPackCondition.CarCondition<Integer> acceleratorStatusCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_ACCELERATOR_STATUS,
                                        acceleratorStatus,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_ACCELERATOR_STATUS),new byte []{b82});
                                dataPackCondition.getConditionList().add(acceleratorStatusCdt);
                                break;
                            case 0x0483://雨刮器状态
                                byte b83 = itemDataBuf.readByte();
                                int frontWindscreenWiper =  (b83 & 0b1111000) >>> 4;
                                int rearWindscreenWiper = b83 & 0b00001111;

                                Map<String,Integer> valMap83 = new HashMap<>(2);
                                valMap83.put("frontWindscreenWiper",frontWindscreenWiper);
                                valMap83.put("rearWindscreenWiper",rearWindscreenWiper);

                                DataPackCondition.CarCondition<Map<String,Integer>> windscreenWiperCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_WINDSCREEN_WIPER,
                                        valMap83,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_WINDSCREEN_WIPER),new byte []{b83});
                                dataPackCondition.getConditionList().add(windscreenWiperCdt);
                                break;
                            case 0x0484://充电枪状态
                                byte b84 = itemDataBuf.readByte();
                                int evCharger = b84;

                                DataPackCondition.CarCondition<Integer> evChargerCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_EV_CHARGER,
                                        evCharger,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_EV_CHARGER),new byte []{b84});
                                dataPackCondition.getConditionList().add(evChargerCdt);
                                break;
                            case 0x0485://充电状态 0-未充电，1-正在充电，2-充电完成
                                byte b85 = itemDataBuf.readByte();
                                int chargerStatus = b85 & 0b00001111;

                                DataPackCondition.CarCondition<Integer> chargerStatusCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_CHARGER_STATUS,
                                        chargerStatus,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_CHARGER_STATUS),new byte []{b85});
                                dataPackCondition.getConditionList().add(chargerStatusCdt);
                                break;
                            case 0x0486://行驶状态（方向）  0-驻车，1-后退，2-停止，3-前进
                                byte b86 = itemDataBuf.readByte();
                                int runStatus = b86 & 0b00001111;

                                DataPackCondition.CarCondition<Integer> runStatusCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_RUN_STATUS,
                                        runStatus,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_RUN_STATUS),new byte []{b86});
                                dataPackCondition.getConditionList().add(runStatusCdt);
                                break;
                            case 0x0487://升窗档次  档次 （百分比）
                                byte [] b87 = new byte[6];
                                itemDataBuf.readBytes(b87);

                                int leftFrontWindowLiftLevel =  b87[0];
                                int rightFrontWindowLiftLevel =  b87[1];
                                int leftRearWindowLiftLevel =  b87[2];
                                int rightRearWindowLiftLevel =  b87[3];
                                int topWindowLiftLevel =  b87[4];
                                int tailWindowLiftLevel =  b87[5];

                                Map<String,Integer> valMap87 = new HashMap<>(6);
                                valMap87.put("leftFrontWindowLiftLevel",leftFrontWindowLiftLevel);
                                valMap87.put("rightFrontWindowLiftLevel",rightFrontWindowLiftLevel);
                                valMap87.put("leftRearWindowLiftLevel",leftRearWindowLiftLevel);
                                valMap87.put("rightRearWindowLiftLevel",rightRearWindowLiftLevel);
                                valMap87.put("topWindowLiftLevel",topWindowLiftLevel);
                                valMap87.put("tailWindowLiftLevel",tailWindowLiftLevel);

                                DataPackCondition.CarCondition<Map<String,Integer>> windowLiftLevelCdt = new DataPackCondition.CarCondition<Map<String,Integer>>(DataPackCondition.CONDITIONNAME_WINDOW_LIFT_LEVEL,
                                        valMap87,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_WINDOW_LIFT_LEVEL),b87);
                                dataPackCondition.getConditionList().add(windowLiftLevelCdt);
                                break;
                            case 0x0488://就绪状态  0-未就绪，1-就绪
                                byte b88 = itemDataBuf.readByte();
                                int readyStatus = b88;

                                DataPackCondition.CarCondition<Integer> readyStatusCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_READY_STATUS,
                                        readyStatus,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_READY_STATUS),new byte []{b88});
                                dataPackCondition.getConditionList().add(readyStatusCdt);
                                break;
                            case 0x0489://保养状态 0-不需保养，1-需要保养
                                byte b89 = itemDataBuf.readByte();
                                int upKeepStatus = b89;

                                DataPackCondition.CarCondition<Integer> upKeepStatusCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_UPKEEP_STATUS,
                                        upKeepStatus,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_UPKEEP_STATUS),new byte []{b89});
                                dataPackCondition.getConditionList().add(upKeepStatusCdt);
                                break;
                            case 0x048D://空调风扇档次 1-1档，2-2档，3-3档，4-4档，5-5档，6-6档，7-7档，8-8档
                                byte b8D = itemDataBuf.readByte();
                                int airConditionerFanLevel = b8D & 0x0F;

                                DataPackCondition.CarCondition<Integer> airConditionerFanLevelCdt = new DataPackCondition.CarCondition<Integer>(DataPackCondition.CONDITIONNAME_AIR_CONDITIONER_FAN_LEVEL,
                                        airConditionerFanLevel,DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_AIR_CONDITIONER_FAN_LEVEL),new byte []{b8D});
                                dataPackCondition.getConditionList().add(airConditionerFanLevelCdt);
                                break;

                            case 0x04A0:// 自定义透传数据
                                int len = itemDataBuf.readableBytes();
                                byte [] bA0 = new byte[len -1];
                                itemDataBuf.readBytes(bA0);
                                System.out.println("自定义透传数据" + ByteBufUtil.hexDump(bA0));

                                DataPackCondition.CarCondition<String> costomDataCdt = new DataPackCondition.CarCondition<String>(DataPackCondition.CONDITIONNAME_COSTOM_DATA,
                                        Base64.getEncoder().encodeToString(bA0),DataPackCondition.getConditionDesc(DataPackCondition.CONDITIONNAME_COSTOM_DATA),bA0);
                                dataPackCondition.getConditionList().add(costomDataCdt);
                                break;

                        }

/*--------------------------------------------------end 车身数据---------------------------------------------------------------------*/


/*--------------------------------------------- 定位数据---------------------------------------------------------*/
                        if (null == position && (itemId >= 0x0602 || itemId <= 0x0609)) {
                            position = new DataPackPosition(dataPackObject);
                            position.setPositioMode(DataPackPosition.POSITION_MODE_GPS);
                            dataPackAlarm.setPosition(position);
                        }

                        switch (itemId) {
                            case 0x0602://定位状态
                                int status = LanduDataPackUtil.readByte(itemDataBuf);//0-无效，1-有效
                                if (0 == status) {
                                    position.setPositioMode(DataPackPosition.POSITION_MODE_INVALID);
                                }

                                break;
                            case 0x0603://有效星数
                                int validStar = LanduDataPackUtil.readByte(itemDataBuf);
                                position.setValidStar(validStar);
                                break;
                            case 0x0604://方向
                                int direct = LanduDataPackUtil.readWord(itemDataBuf);
                                position.setDirection((float) direct);
                                break;
                            case 0x0605://经度
                                int lan = LanduDataPackUtil.readInt4(itemDataBuf);
                                position.setLatitude(lan * 0.000001);
                                break;
                            case 0x0606://纬度
                                int lon = LanduDataPackUtil.readInt4(itemDataBuf);
                                position.setLongitude(lon * 0.000001);
                                break;
                            case 0x0607://海拔
                                int altitude = LanduDataPackUtil.readWord(itemDataBuf);
                                position.setAltitude(altitude);
                                break;
                            case 0x0608://GPS速度
                                int v = LanduDataPackUtil.readByte(itemDataBuf);
                                position.setSpeed((float) v);
                                break;
                            case 0x0609://定位时间
                                long time = LanduDataPackUtil.readUInt7(itemDataBuf);
                                position.setPositionTime(new Date(time));
                                break;
                        }


/*---------------------------------------------end 定位数据---------------------------------------------------------*/

                        //TODO J1939数据(文档无说明)


/*-------------------------------------------------------- TOOD 其它数据--------------------------------------------------------------------*/
                        if (null == overview && (itemId >= 0x0E00 && itemId <= 0x0E06)) {
                            overview = new DataPackOverview(dataPackObject);
                        }
                        switch (itemId) {
                            case 0x0E00: //行程ID
                                long tripId = LanduDataPackUtil.readUInt4(itemDataBuf);
                                dataPackObject.setTripId(tripId);
                                break;
                            case 0x0E01://点火时间
                                String fireTime = LanduDataPackUtil.readString(itemDataBuf);
                                //设置车状态为点火，再设置数据采集时间为点火时间
                                overview.setCarStatus(0x01);

                                try {
                                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    overview.setDetectionTime(df.parse(fireTime));
                                } catch (ParseException e) {
                                }
                                break;
                            case 0x0E02://熄火时间
                                String unFireTime = LanduDataPackUtil.readString(itemDataBuf);
                                //设置车状态为熄火，再设置数据采集时间为熄火时间
                                overview.setCarStatus(0x03);

                                try {
                                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    overview.setDetectionTime(df.parse(unFireTime));
                                } catch (ParseException e) {
                                }
                                break;
                            case 0x0E03://急加速次数
                                int speedUpTimes = LanduDataPackUtil.readWord(itemDataBuf);
                                overview.setSpeedUpTimes(speedUpTimes);
                                break;
                            case 0x0E04://急减速次数
                                int speendDownTimes = LanduDataPackUtil.readWord(itemDataBuf);
                                overview.setSpeedDownTimes(speendDownTimes);
                                break;
                            case 0x0E05://急转弯次数
                                int sharpTurnTimes = LanduDataPackUtil.readWord(itemDataBuf);
                                overview.setSharpTurnTimes(sharpTurnTimes);
                                break;
                            case 0x0E06://行驶里程
                                int mileage = (int) LanduDataPackUtil.readDWord(itemDataBuf);
                                overview.setMileage(mileage);
                                break;
                        }


                        // TODO 补充的
                        if (null == overview && (itemId == 0x0000 || itemId == 0x0004 || itemId == 0x0007)) {
                            overview = new DataPackOverview(dataPackObject);
                        }

                        switch (itemId) {
                            case 0x0000: //蓄电池电压
                                float voltage = LanduDataPackUtil.readByte(itemDataBuf) / 10.0F;
                                overview.setVoltage(voltage);
                                break;
                            case 0x0004: //燃油系统1状态
                                float avgOilUsed = LanduDataPackUtil.readByte(itemDataBuf);
                                overview.setAvgOilUsed(avgOilUsed);
                                break;
                            case 0x0007: //TODO 发动机冷却液温度
                                int temperature = LanduDataPackUtil.readByte(itemDataBuf);
                                System.out.println(temperature);
                                break;
                        }


/*-------------------------------------------------------- end 其它数据--------------------------------------------------------------------*/

                    }//end while

                    if(null != signTypeObj){
                        dataPackTargetList.add(new DataPackTarget(signTypeObj));
                    }

                    if(null != signInfo){
                        dataPackTargetList.add(new DataPackTarget(signInfo));
                    }

                    if (null != dataPackCondition) {
                        dataPackTargetList.add(new DataPackTarget(dataPackCondition));
                    }
                    if (null != carSignals) {
                        dataPackTargetList.add(new DataPackTarget(carSignals));
                    }

                    if (null != position) {
                        dataPackTargetList.add(new DataPackTarget(position));
                    }

                    if (null != overview) {
                        dataPackTargetList.add(new DataPackTarget(overview));
                    }

                    if (null != dataPackAlarm.getAlarmList() && dataPackAlarm.getAlarmList().size() > 0) {
                        dataPackTargetList.add(new DataPackTarget(dataPackAlarm));
                    }

                    break;
/****------------------------------------------------------------------ end 1602 --------------------------------------------------------------------****/
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
