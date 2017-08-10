package com.incarcloud.rooster.datapack;

import com.incarcloud.rooster.util.LanduDataClassifyUtil;
import com.incarcloud.rooster.util.LanduDataPackUtil;
import com.incarcloud.rooster.util.LanduRespUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.*;

/**
 * LANDU Parser.
 *
 * @author Aaric
 * @since 2.0
 */
public class DataParserLandu implements IDataParser {

    private static Logger _logger = LoggerFactory.getLogger(DataParserLandu.class);

    /**
     * 协议分组和名称
     */
    public static final String PROTOCOL_GROUP = "china";
    public static final String PROTOCOL_NAME = "landu";
    public static final String PROTOCOL_PREFIX = PROTOCOL_GROUP + "-" + PROTOCOL_NAME + "-";

    static {
        /**
         * 声明数据包版本与解析器类关系
         */
        DataParserManager.register(PROTOCOL_PREFIX + "2.05", DataParserLandu.class);
        DataParserManager.register(PROTOCOL_PREFIX + "3.08", DataParserLandu.class);
    }




    /**
     * 数据包准许最大容量2M
     */
    private static final int DISCARDS_MAX_LENGTH = 1024 * 1024 * 2;

    /**
     * 验证数据包
     *
     * @param bytes 原始数据
     * @return
     */
    private boolean validate(byte[] bytes) {
        if(null != bytes) {
            int total = bytes.length;
            // 1.数据包长度必须大于9个字节(最小10个字节)
            if(9 < total) {
                // 2.判断数据包标志
                if(0xAA == (bytes[0] & 0xFF) && 0x55 == (bytes[1] & 0xFF)) {
                    // 3.校验数据包长度(取反校验+数值校验)
                    if(bytes[2] == ~bytes[4] && bytes[3] == ~bytes[5]) {
                        int length = (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
                        // 长度 = 总长度 - 2(2字节标志位)
                        if(length == (total - 2)) {
                            // 4.校验和校验
                            int sum = 0;
                            int sumCheck = (bytes[total-2] & 0xFF) << 8 | (bytes[total-1] & 0xFF);
                            for (int i = 2; i < total - 2; i++) {
                                sum += (bytes[i] & 0xFF);
                            }
                            // 校验
                            if(sum == sumCheck) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
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
        DataPack dataPack;
        List<DataPack> dataPackList = new ArrayList<>();

        // 长度大于2M的数据包直接抛弃(恶意数据)
        if(DISCARDS_MAX_LENGTH < buffer.readableBytes()) {
            //System.out.println("clear");
            buffer.clear();
        }

        // 遍历
        boolean skipFlag;
        int offset, max, length, sum, sumCheck;
        while(buffer.isReadable()) {
            skipFlag = true;
            offset = buffer.readerIndex();
            max = buffer.writerIndex();
            //System.out.printf("%d-%s: ", offset, ByteBufUtil.hexDump(new byte[]{buffer.getByte(offset)}));
            // 一个包最小10个字节
            if(10 < (max - offset)) {
                // 判断数据包标志
                if(0xAA == (buffer.getByte(offset) & 0xFF) && 0x55 == (buffer.getByte(offset + 1) & 0xFF)) {
                    // 获取包长度并校验
                    if(buffer.getByte(offset +2) == ~buffer.getByte(offset + 4)
                            && buffer.getByte(offset + 3) == ~buffer.getByte(offset + 5)) {
                        length = (buffer.getByte(offset + 2) & 0xFF) << 8 | (buffer.getByte(offset + 3) & 0xFF);
                        //System.out.printf(" | length: %d", length);
                        // 检验包是否完整(length + 2)
                        if(length <= (max - offset - 2)) {
                            // 检验校验和
                            sum = 0;
                            sumCheck = (buffer.getByte(offset + length) & 0xFF) << 8 | (buffer.getByte(offset + length + 1) & 0xFF);
                            // 求和
                            for (int i = offset + 2, n = offset + length; i < n; i++) {
                                sum += (buffer.getByte(i) & 0xFF);
                            }
                            // 校验
                            if(sum == sumCheck) {
                                // DataPack
                                //System.out.printf(" <DataPack> ");
                                // 版本(第7个字节为协议格式版本)
                                String version = null;
                                switch (buffer.getByte(offset + 7)) {
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
                                dataPack.setBuf(buffer.slice(offset, length + 2));
                                dataPackList.add(dataPack);

                                // 跳跃(length+2)
                                skipFlag = false;
                                buffer.skipBytes(length + 2);
                            }
                            //System.out.printf("| sum: %d, sumCheck: %d ", sum, sumCheck);
                        }
                    }
                }
            } else {
                // 没有一个包的长度，跳出循环
                break;
            }
            //System.out.println();

            // 不符合条件，向前跳跃1
            if(skipFlag) {
                buffer.skipBytes(1);
            }
        }

        // 扔掉已读数据
        buffer.discardSomeReadBytes();

        return dataPackList;
    }

    @Override
    public ByteBuf createResponse(DataPack requestPack, ERespReason reason) {
        ByteBuf responseBuf = null;
        // 回复设备数据
        if(null != requestPack && ERespReason.OK == reason) {
            // 校验数据
            byte[] originalBytes = Base64.getDecoder().decode(requestPack.getDataB64());
            // 封装返回数据(成功返回0x00)
            byte[] returnBytes = null;
            if(validate(originalBytes)) {
                if(0x16 == originalBytes[8] && 0x03 == originalBytes[9]) {
                    // 命令字：0x1603
                    // 不设置任何参数
                    returnBytes = LanduRespUtil.response0x1603Bytes(originalBytes);
                } else {
                    // 命令字：非0x1603
                    // 回复成功状态
                    returnBytes = LanduRespUtil.responseBytes(originalBytes, (byte) 0x00);
                }
            }
            if(null != returnBytes) {
                responseBuf = Unpooled.wrappedBuffer(returnBytes);
            }
        }
        return responseBuf;
    }

    @Override
    public void destroyResponse(ByteBuf responseBuf) {
        if(null != responseBuf) {
            responseBuf.release();
        }
    }

    @Override
    public List<DataPackTarget> extractBody(DataPack dataPack) {
        List<DataPackTarget> dataPackTargetList = null;
        byte[] dataPackBytes = Base64.getDecoder().decode(dataPack.getDataB64());
        if(validate(dataPackBytes)) {
            ByteBuf buffer = null;
            dataPackTargetList = new ArrayList<>();
            DataPackObject dataPackObject = new DataPackObject(dataPack);
            DataPackOverview dataPackOverview;
            DataPackPosition dataPackPosition;
            DataPackPeak dataPackPeak;
            DataPackPeak.Peak dataPeak;
            List<DataPackPeak.Peak> dataPeakList;
            LanduDataClassifyUtil.Peak tempatePeak;
            DataPackAlarm dataPackAlarm;
            DataPackAlarm.Alarm dataAlarm;
            List<DataPackAlarm.Alarm> dataAlarmList;

            try {
                // 初始化ByteBuf
                buffer = Unpooled.wrappedBuffer(dataPackBytes);

                // 跳过“标志+长度+长度校验”6个字节
                LanduDataPackUtil.readBytes(buffer, 6);

                // 数据包ID
                dataPackObject.setPackId(LanduDataPackUtil.readByte(buffer));

                // 协议格式版本
                String version;
                switch (LanduDataPackUtil.readByte(buffer)) {
                    case 0x02:
                        version = "2.05";
                        break;
                    case 0x05:
                        version = "3.08";
                        break;
                    default:
                        version = "unknown";
                }
                dataPackObject.setProtocolVersion(version);

                // 协议格式名称
                dataPackObject.setProtocolName(PROTOCOL_PREFIX + version);

                int commandId = LanduDataPackUtil.readUInt2(buffer);
                // 命令字
                switch (commandId) {
                    case 0x1601:
                        System.out.println("## 0x1601 - 3.1.1 车辆检测数据主动上传");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        //dataTarget.setTripId(buffer.readUnsignedShort());
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        // 5.检测数据时间
                        //dataTarget.setDetectionDate(DataTool.readStringZero(buffer));
                        dataPackObject.setDetectionTime(LanduDataPackUtil.readDate(buffer));

                        // 6.车辆状态
                        int carStatus = LanduDataPackUtil.readByte(buffer);
                        switch (carStatus) {
                            case 0x01:
                                // 0x01-发动机点火时
                                System.out.println("## 发动机点火时");
                                // 6.1 整车数据
                                dataPackOverview = new DataPackOverview(dataPackObject);
                                // 6.1.1 车辆状态
                                dataPackOverview.setCarStatus(carStatus);
                                // 6.1.2 启动电压(V)
                                dataPackOverview.setVoltage(Float.parseFloat(LanduDataPackUtil.readString(buffer)));

                                // 6.2 定位信息
                                dataPackPosition = new DataPackPosition(dataPackObject);
                                // 6.2.1 车速(km/h)
                                dataPackPosition.setSpeed(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                                // 6.2.2 当前行程行驶距离(m)
                                dataPackPosition.setTravelDistance(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                                String[] positions = LanduDataPackUtil.splitPositionString(buffer);
                                // 6.2.3.经度
                                dataPackPosition.setLongitude(LanduDataPackUtil.parsePositionString(positions[0]));
                                // 6.2.4 纬度
                                dataPackPosition.setLatitude(LanduDataPackUtil.parsePositionString(positions[1]));
                                // 6.2.5 方向
                                dataPackPosition.setDirection(Float.parseFloat(positions[2]));
                                // 6.2.6 定位时间
                                dataPackPosition.setPositionTime(LanduDataPackUtil.formatDateString(positions[3]));
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
                                // --add
                                dataPackTargetList.add(new DataPackTarget(dataPackPosition));

                                // --add
                                // 6.1.3 定位信息
                                dataPackOverview.setPosition(dataPackPosition);
                                dataPackTargetList.add(new DataPackTarget(dataPackOverview));
                                break;
                            case 0x02:
                                // 0x02-发动机运行中
                                System.out.println("## 发动机运行中");
                                // 1.整车数据
                                dataPackOverview = new DataPackOverview(dataPackObject);
                                // 1.1 车辆状态
                                dataPackOverview.setCarStatus(carStatus);
                                // --add
                                dataPackTargetList.add(new DataPackTarget(dataPackOverview));

                                // 2.极值数据个数
                                int count = buffer.readUnsignedShort();
                                if(0 < count) {
                                    dataPeakList = new ArrayList<>();

                                    for(int i = 0; i < count; i++){
                                        // 2.1 数据项id
                                        Integer id = buffer.readUnsignedShort();
                                        // 2.2 数据项内容
                                        String content = LanduDataPackUtil.readString(buffer);

                                        // 2.3 设置数值信息
                                        dataPeak = new DataPackPeak.Peak(id, content);
                                        // 完善数据信息
                                        tempatePeak = LanduDataClassifyUtil.PEAK_MAP.get(id);
                                        if(null != tempatePeak) {
                                            dataPeak.setPeakName(tempatePeak.getPeakName());
                                            dataPeak.setPeakUnit(tempatePeak.getPeakUnit());
                                            dataPeak.setPeakDesc(tempatePeak.getPeakDesc());
                                        }

                                        dataPeakList.add(dataPeak);
                                    }

                                    // 2.4 添加分发数据
                                    dataPackPeak = new DataPackPeak(dataPackObject);
                                    dataPackPeak.setPeakList(dataPeakList);
                                    dataPackTargetList.add(new DataPackTarget(dataPackPeak));
                                }
                                break;
                            case 0x03:
                                // 0x03-发动机熄火时
                                System.out.println("## 发动机熄火时");
                                // 整车数据
                                dataPackOverview = new DataPackOverview(dataPackObject);
                                dataPackOverview.setCarStatus(carStatus);
                                // 1.本行程数据小计
                                // 1.1 本次发动机运行时间
                                dataPackOverview.setRunTime(buffer.readUnsignedShort());
                                // 1.2 本次行驶距离
                                dataPackOverview.setCurrentMileage(buffer.readInt());
                                Integer averageFuelConsumption = buffer.readUnsignedShort();
                                // 1.3 本次平均油耗
                                dataPackOverview.setCurrentAvgOilUsed(averageFuelConsumption.floatValue()/100F);
                                //累计行驶里程
                                dataPackOverview.setMileage(buffer.readInt());
                                Integer totalAverageFuelConsumption = buffer.readUnsignedShort();
                                // 1.4 累计平均油耗
                                dataPackOverview.setAvgOilUsed(totalAverageFuelConsumption.floatValue()/100F);
                                // 1.5 车速分组统计
                                count = buffer.readUnsignedByte();
                                if(0 < count) {
                                    int speed, consumeTime, travelDistance;
                                    List<DataPackOverview.Speed> speedList = new ArrayList<>();
                                    for(int i = 0;i < count;i++){
                                        // 1.5.1 设置速度值
                                        speed = (int) buffer.readUnsignedByte();
                                        // 1.5.2 时间小计(秒)
                                        consumeTime = buffer.readUnsignedShort();
                                        // 1.5.3 距离小计(米)
                                        travelDistance = buffer.readInt();

                                        speedList.add(new DataPackOverview.Speed(speed, consumeTime, travelDistance));
                                    }
                                    dataPackOverview.setSpeedGroup(speedList);
                                }
                                // 2.驾驶习惯统计
                                // 2.1 本次急加速次数
                                dataPackOverview.setSpeedUpTimes(buffer.readUnsignedShort());
                                // 2.2 本次急减速次数
                                dataPackOverview.setSpeedDownTimes(buffer.readUnsignedShort());
                                // 2.3 本次急转向次数
                                dataPackOverview.setSharpTurnTimes(buffer.readUnsignedShort());
                                // 2.4 本次时速超速时间
                                dataPackOverview.setSpeedingTime(buffer.readInt());
                                // 2.5 最高车速
                                dataPackOverview.setMaxSpeed((int) buffer.readUnsignedByte());

                                // 3.定位信息
                                dataPackPosition = new DataPackPosition(dataPackObject);
                                // 3.1 定位信息-车速(km/h)
                                dataPackPosition.setSpeed(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                                // 3.2 定位信息-当前行程行驶距离(m)
                                dataPackPosition.setTravelDistance(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                                positions = LanduDataPackUtil.splitPositionString(buffer);
                                // 3.3 定位信息-经度
                                dataPackPosition.setLongitude(LanduDataPackUtil.parsePositionString(positions[0]));
                                // 3.4 定位信息-纬度
                                dataPackPosition.setLatitude(LanduDataPackUtil.parsePositionString(positions[1]));
                                // 3.5 定位信息-方向
                                dataPackPosition.setDirection(Float.parseFloat(positions[2]));
                                // 3.6 定位信息-定位时间
                                dataPackPosition.setPositionTime(LanduDataPackUtil.formatDateString(positions[3]));
                                // 3.7 定位信息-定位方式：0-无效数据，1-基站定位，2-GPS定位
                                dataPackPosition.setPositioMode(Integer.parseInt(positions[4]));
                                // 3.8 定位信息-定位方式描述
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
                                // --add
                                dataPackTargetList.add(new DataPackTarget(dataPackPosition));

                                // --add
                                // 2.6 定位信息
                                dataPackOverview.setPosition(dataPackPosition);
                                dataPackTargetList.add(new DataPackTarget(dataPackOverview));
                                break;
                            case 0x04:
                                // 0x04-发动机熄火后
                                System.out.println("## 发动机熄火后");
                                // 整车数据
                                dataPackOverview = new DataPackOverview(dataPackObject);
                                dataPackOverview.setCarStatus(carStatus);
                                //蓄电池电压值
                                dataPackOverview.setVoltage(Float.parseFloat(LanduDataPackUtil.readString(buffer)));
                                // --add
                                dataPackTargetList.add(new DataPackTarget(dataPackOverview));
                                break;
                            case 0x05:
                                // 0x05-车辆不能检测
                                System.out.println("## 车辆不能检测");
                                // 无数据上传
                                break;
                        }
                        break;
                    case 0x1602:
                        System.out.println("## 0x1602 - 3.1.2 上传车辆报警");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        // 5.检测数据时间
                        dataPackObject.setDetectionTime(LanduDataPackUtil.readDate(buffer));
                        // 6.报警类型
                        int alarmType = buffer.readUnsignedByte();
                        // 7.定位信息
                        dataPackPosition = new DataPackPosition(dataPackObject);
                        // 7.1 车速(km/h)
                        dataPackPosition.setSpeed(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                        // 7.2 当前行程行驶距离(m)
                        dataPackPosition.setTravelDistance(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                        String[] positions = LanduDataPackUtil.splitPositionString(buffer);
                        // 7.3.经度
                        dataPackPosition.setLongitude(LanduDataPackUtil.parsePositionString(positions[0]));
                        // 7.4 纬度
                        dataPackPosition.setLatitude(LanduDataPackUtil.parsePositionString(positions[1]));
                        // 7.5 方向
                        dataPackPosition.setDirection(Float.parseFloat(positions[2]));
                        // 7.6 定位时间
                        dataPackPosition.setPositionTime(LanduDataPackUtil.formatDateString(positions[3]));
                        // 7.7 定位方式：0-无效数据，1-基站定位，2-GPS定位
                        dataPackPosition.setPositioMode(Integer.parseInt(positions[4]));
                        // 7.8 定位方式描述
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
                        // --add
                        dataPackTargetList.add(new DataPackTarget(dataPackPosition));

                        // 8.报警数据
                        dataAlarmList = new ArrayList<>();
                        dataPackAlarm = new DataPackAlarm(dataPackObject);
                        // 8.1 判断报警类型信息
                        switch (alarmType){
                            case 0x01:
                                System.out.println("## 新故障码报警: ");
                                //故障码个数
                                int count = buffer.readUnsignedByte();
                                if(0 < count) {
                                    for(int i = 0;i < count;i ++){
                                        //故障码
                                        String code = LanduDataPackUtil.readString(buffer);
                                        //故障码属性
                                        String value = LanduDataPackUtil.readString(buffer);
                                        //故障码描述
                                        String desc = LanduDataPackUtil.readString(buffer);

                                        dataAlarm = new DataPackAlarm.Alarm("新故障码报警");
                                        dataAlarm.setAlarmCode(code);
                                        dataAlarm.setAlarmValue(value);
                                        dataAlarm.setAlarmDesc(desc);

                                        dataAlarmList.add(dataAlarm);
                                    }
                                }
                                break;
                            case 0x02:
                                System.out.println("## 碰撞报警/异常震动报警: ");
                                dataAlarm = new DataPackAlarm.Alarm("碰撞报警");
                                dataAlarm.setAlarmCode(String.valueOf(alarmType));
                                //dataAlarm.setAlarmValue();

                                dataAlarmList.add(dataAlarm);
                                break;
                            case 0x03:
                                System.out.println("## 防盗报警: ");
                                dataAlarm = new DataPackAlarm.Alarm("防盗报警");
                                dataAlarm.setAlarmCode(String.valueOf(alarmType));
                                //dataAlarm.setAlarmValue();

                                dataAlarmList.add(dataAlarm);
                                break;
                            case 0x04:
                                System.out.println("## 水温报警: ");
                                //实际水温数值
                                String waterTemperature = LanduDataPackUtil.readString(buffer);
                                dataAlarm = new DataPackAlarm.Alarm("水温报警");
                                dataAlarm.setAlarmCode(String.valueOf(alarmType));
                                dataAlarm.setAlarmValue(waterTemperature);

                                dataAlarmList.add(dataAlarm);
                                break;
                            case 0x05:
                                System.out.println("## 充电电压报警: ");
                                //充电电压值
                                String chargingVoltage = LanduDataPackUtil.readString(buffer);
                                dataAlarm = new DataPackAlarm.Alarm("充电电压报警");
                                dataAlarm.setAlarmCode(String.valueOf(alarmType));
                                dataAlarm.setAlarmValue(chargingVoltage);
                                dataAlarm.setAlarmDesc("小于 13.1 伏");

                                dataAlarmList.add(dataAlarm);
                                break;
                            case 0xF0:
                                System.out.println("## 拔下OBD报警: ");
                                //设备拔下时间戳
                                String pullOutTime = LanduDataPackUtil.readString(buffer);
                                dataAlarm = new DataPackAlarm.Alarm("拔下OBD报警");
                                dataAlarm.setAlarmCode(String.valueOf(alarmType));
                                dataAlarm.setAlarmValue(pullOutTime);

                                dataAlarmList.add(dataAlarm);
                                break;
                            default:
                                System.out.println("## 其他报警: ");
                                dataAlarm = new DataPackAlarm.Alarm("其他报警");
                                dataAlarm.setAlarmCode(String.valueOf(alarmType));
                                //dataAlarm.setAlarmValue();

                                dataAlarmList.add(dataAlarm);
                        }
                        // 8.2 添加分发数据
                        dataPackAlarm.setAlarmList(dataAlarmList);
                        dataPackAlarm.setPosition(dataPackPosition);
                        dataPackTargetList.add(new DataPackTarget(dataPackAlarm));
                        break;
                    case 0x1603:
                        System.out.println("## 0x1603 - 3.1.3 从服务器取得参数");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));

                        DataPackDevice dataPackDevice = new DataPackDevice(dataPackObject);
                        //硬件版本号
                        dataPackDevice.setHardwareVersion(LanduDataPackUtil.readString(buffer));
                        //固件版本号
                        dataPackDevice.setFirmwareVersion(LanduDataPackUtil.readString(buffer));
                        //软件版本号
                        dataPackDevice.setSoftwareVersion(LanduDataPackUtil.readString(buffer));
                        //诊断程序类型
                        dataPackDevice.setDiagnoseProgramType((int) buffer.readUnsignedByte());
                        //恢复出厂设置序号
                        dataPackDevice.setInitCode((int) buffer.readUnsignedByte());
                        //添加分发数据
                        dataPackTargetList.add(new DataPackTarget(dataPackDevice));
                        break;
                    case 0x1605:
                        System.out.println("## 0x1605 - 3.1.4 上传调试数据");
                        break;
                    case 0x1606:
                        System.out.println("## 0x1606 - 3.1.5 位置数据");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));

                        //定位信息个数
                        int count = buffer.readUnsignedShort();
                        //定位信息列表
                        for(int i = 0;i < count;i ++){
                            //定位信息
                            dataPackPosition = new DataPackPosition(dataPackObject);
                            //定位信息-车速(km/h)
                            dataPackPosition.setSpeed(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                            //定位信息-当前行程行驶距离(m)
                            dataPackPosition.setTravelDistance(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                            positions = LanduDataPackUtil.splitPositionString(buffer);
                            //定位信息-经度
                            dataPackPosition.setLongitude(LanduDataPackUtil.parsePositionString(positions[0]));
                            //定位信息-纬度
                            dataPackPosition.setLatitude(LanduDataPackUtil.parsePositionString(positions[1]));
                            //定位信息-方向
                            dataPackPosition.setDirection(Float.parseFloat(positions[2]));
                            //定位信息-定位时间
                            dataPackPosition.setPositionTime(LanduDataPackUtil.formatDateString(positions[3]));
                            //定位信息-定位方式：0-无效数据，1-基站定位，2-GPS定位
                            dataPackPosition.setPositioMode(Integer.parseInt(positions[4]));
                            //定位信息-定位方式描述
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

                            //添加分发数据
                            dataPackTargetList.add(new DataPackTarget(dataPackPosition));
                        }
                        break;
                    case 0x1607:
                        System.out.println("## 0x1607 - 3.1.6 冻结帧数据");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        // 5.检测数据时间
                        dataPackObject.setDetectionTime(LanduDataPackUtil.readDate(buffer));

                        //冻结帧个数
                        count = buffer.readUnsignedShort();
                        if(0 < count) {
                            dataPeakList = new ArrayList<>();

                            //冻结帧列表
                            for(int i = 0; i < count; i++){
                                //数据项id
                                Integer id = buffer.readUnsignedShort();
                                //数据项内容
                                String content = LanduDataPackUtil.readString(buffer);

                                // 设置数值信息
                                dataPeak = new DataPackPeak.Peak(id, content);
                                // 完善数据信息
                                tempatePeak = LanduDataClassifyUtil.PEAK_MAP.get(id);
                                if(null != tempatePeak) {
                                    dataPeak.setPeakName(tempatePeak.getPeakName());
                                    dataPeak.setPeakUnit(tempatePeak.getPeakUnit());
                                    dataPeak.setPeakDesc(tempatePeak.getPeakDesc());
                                }

                                dataPeakList.add(dataPeak);
                            }

                            //添加分发数据
                            dataPackPeak = new DataPackPeak(dataPackObject);
                            dataPackPeak.setPeakList(dataPeakList);
                            dataPackTargetList.add(new DataPackTarget(dataPackPeak));
                        }
                        break;
                    case 0x1608:
                        System.out.println("## 0x1608 - 3.1.7 怠速车况数据");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        // 5.检测数据时间
                        dataPackObject.setDetectionTime(LanduDataPackUtil.readDate(buffer));

                        //故障码个数
                        int alarmCount = buffer.readUnsignedByte();
                        if(0 < alarmCount) {
                            dataAlarmList = new ArrayList<>();

                            //故障码列表
                            for(int i = 0;i < alarmCount;i ++){
                                //故障码
                                String code = LanduDataPackUtil.readString(buffer);
                                //故障码属性
                                String value = LanduDataPackUtil.readString(buffer);
                                //故障码描述
                                String desc = LanduDataPackUtil.readString(buffer);

                                dataAlarm = new DataPackAlarm.Alarm("故障码");
                                dataAlarm.setAlarmCode(code);
                                dataAlarm.setAlarmValue(value);
                                dataAlarm.setAlarmDesc(desc);

                                dataAlarmList.add(dataAlarm);
                            }

                            //添加分发数据
                            dataPackAlarm = new DataPackAlarm(dataPackObject);
                            dataPackAlarm.setAlarmList(dataAlarmList);
                            dataPackTargetList.add(new DataPackTarget(dataPackAlarm));
                        }

                        //数据流个数
                        count = buffer.readUnsignedShort();
                        if(0 < count) {
                            dataPeakList = new ArrayList<>();

                            //数据流列表(车况信息)
                            for(int i = 0; i < count; i++){
                                //数据项id
                                Integer id = buffer.readUnsignedShort();
                                //数据项内容
                                String content = LanduDataPackUtil.readString(buffer);

                                // 设置数值信息
                                dataPeak = new DataPackPeak.Peak(id, content);
                                // 完善数据信息
                                tempatePeak = LanduDataClassifyUtil.PEAK_MAP.get(id);
                                if(null != tempatePeak) {
                                    dataPeak.setPeakName(tempatePeak.getPeakName());
                                    dataPeak.setPeakUnit(tempatePeak.getPeakUnit());
                                    dataPeak.setPeakDesc(tempatePeak.getPeakDesc());
                                }

                                dataPeakList.add(dataPeak);
                            }

                            //添加分发数据
                            dataPackPeak = new DataPackPeak(dataPackObject);
                            dataPackPeak.setPeakList(dataPeakList);
                            dataPackTargetList.add(new DataPackTarget(dataPackPeak));
                        }
                        break;
                    case 0x160A:
                        System.out.println("## 0x160A - 3.1.9 行为位置数据");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        // 5.检测数据时间
                        dataPackObject.setDetectionTime(LanduDataPackUtil.readDate(buffer));

                        //数据类型
                        int dataType = buffer.readUnsignedByte();
                        String behaviorName = null;
                        String behaviorDesc = null;
                        switch (dataType){
                            case 0x01:
                                System.out.println("## 超速记录");
                                behaviorName = "超速";
                                behaviorDesc = "超速记录";
                                break;
                            case 0x02:
                                System.out.println("## 急加速记录");
                                behaviorName = "急加速";
                                behaviorDesc = "急加速记录";
                                break;
                            case 0x03:
                                System.out.println("## 急减速记录");
                                behaviorName = "急减速";
                                behaviorDesc = "急减速记录";
                                break;
                            case 0x04:
                                System.out.println("## 急转弯记录");
                                behaviorName = "急转弯";
                                behaviorDesc = "急转弯记录";
                                break;
                            case 0x05:
                                System.out.println("## 无效");
                                break;
                        }
                        //行为位置数据
                        if(0 < dataType && 5 > dataType){
                            // 定位信息
                            dataPackPosition = new DataPackPosition(dataPackObject);
                            // 定位信息-车速(km/h)
                            dataPackPosition.setSpeed(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                            // 定位信息-当前行程行驶距离(m)
                            dataPackPosition.setTravelDistance(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                            positions = LanduDataPackUtil.splitPositionString(buffer);
                            // 定位信息-经度
                            dataPackPosition.setLongitude(LanduDataPackUtil.parsePositionString(positions[0]));
                            // 定位信息-纬度
                            dataPackPosition.setLatitude(LanduDataPackUtil.parsePositionString(positions[1]));
                            // 定位信息-方向
                            dataPackPosition.setDirection(Float.parseFloat(positions[2]));
                            // 定位信息-定位时间
                            dataPackPosition.setPositionTime(LanduDataPackUtil.formatDateString(positions[3]));
                            // 定位信息-定位方式：0-无效数据，1-基站定位，2-GPS定位
                            dataPackPosition.setPositioMode(Integer.parseInt(positions[4]));
                            // 定位信息-定位方式描述
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
                            //--add
                            dataPackTargetList.add(new DataPackTarget(dataPackPosition));

                            // 行为数据
                            DataPackBehavior dataPackBehavior = new DataPackBehavior(dataPackObject);
                            dataPackBehavior.setBehaviorId(dataType);
                            dataPackBehavior.setBehaviorName(behaviorName);
                            dataPackBehavior.setBehaviorDesc(behaviorDesc);
                            dataPackBehavior.setPosition(dataPackPosition);
                            //--add
                            dataPackTargetList.add(new DataPackTarget(dataPackBehavior));
                        }
                        break;
                    case 0x1621:
                        System.out.println("## 0x1621 - 3.2.2 取得车辆当前检测数据");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        //故障等级
                        int alarmLevel = buffer.readUnsignedByte();
                        //故障码个数
                        count = buffer.readUnsignedByte();
                        if(0 < count) {
                            dataAlarmList = new ArrayList<>();

                            //故障码列表
                            for(int i = 0;i < count;i ++){
                                //故障码
                                String code = LanduDataPackUtil.readString(buffer);
                                //故障码属性
                                String value = LanduDataPackUtil.readString(buffer);
                                //故障码描述
                                String desc = LanduDataPackUtil.readString(buffer);

                                dataAlarm = new DataPackAlarm.Alarm("故障码");
                                dataAlarm.setAlarmCode(code);
                                dataAlarm.setAlarmValue(value);
                                dataAlarm.setAlarmDesc(desc);

                                dataAlarmList.add(dataAlarm);
                            }

                            //添加分发数据
                            dataPackAlarm = new DataPackAlarm(dataPackObject);
                            dataPackAlarm.setAlarmList(dataAlarmList);
                            dataPackTargetList.add(new DataPackTarget(dataPackAlarm));
                        }
                        break;
                    case 0x1622:
                        System.out.println("## 0x1622 - 3.2.3 根据索引 ID 取得相应的检测数据");
                    case 0x1623:
                        System.out.println("## 0x1623 - 3.2.4 车辆诊断参数设定");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        //项数
                        count = buffer.readUnsignedShort();
                        if(0 < count) {
                            dataPeakList = new ArrayList<>();

                            //数据项内容
                            for(int i = 0; i < count; i++){
                                //数据项id
                                Integer id = buffer.readUnsignedShort();
                                //数据项内容
                                String content = LanduDataPackUtil.readString(buffer);

                                // 设置数值信息
                                dataPeak = new DataPackPeak.Peak(id, content);
                                // 完善数据信息
                                tempatePeak = LanduDataClassifyUtil.PEAK_MAP.get(id);
                                if(null != tempatePeak) {
                                    dataPeak.setPeakName(tempatePeak.getPeakName());
                                    dataPeak.setPeakUnit(tempatePeak.getPeakUnit());
                                    dataPeak.setPeakDesc(tempatePeak.getPeakDesc());
                                }

                                dataPeakList.add(dataPeak);
                            }

                            //添加分发数据
                            dataPackPeak = new DataPackPeak(dataPackObject);
                            dataPackPeak.setPeakList(dataPeakList);
                            dataPackTargetList.add(new DataPackTarget(dataPackPeak));
                        }
                        break;
                    case 0x1624:
                        System.out.println("## 0x1624 - 3.2.5 清空累计平均油耗");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        //错误代码
                        int resultCode = buffer.readUnsignedByte();

                        DataPackResult dataPackResult = new DataPackResult(dataPackObject);
                        dataPackResult.setResultCode(resultCode);

                        //添加分发数据
                        dataPackTargetList.add(new DataPackTarget(dataPackResult));
                        break;
                    case 0x1625:
                        System.out.println("## 0x1625 - 3.2.6 取得系统版本信息");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));

                        dataPackDevice = new DataPackDevice(dataPackObject);
                        //硬件版本号
                        dataPackDevice.setHardwareVersion(LanduDataPackUtil.readString(buffer));
                        //固件版本号
                        dataPackDevice.setFirmwareVersion(LanduDataPackUtil.readString(buffer));
                        //软件版本号
                        dataPackDevice.setSoftwareVersion(LanduDataPackUtil.readString(buffer));
                        //软件类别ID
                        dataPackDevice.setSoftwareTypeId((int)buffer.readUnsignedByte());
                        //添加分发数据
                        dataPackTargetList.add(new DataPackTarget(dataPackDevice));
                        break;
                    case 0x1626:
                        System.out.println("## 0x1626 - 3.2.7 清除车辆故障码");
                    case 0x16E0:
                        System.out.println("## 0x16E0 - 3.3.1 恢复出厂设置");
                        // 1.设备号
                        dataPackObject.setDeviceId(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataPackObject.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataPackObject.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataPackObject.setVin(LanduDataPackUtil.readString(buffer));
                        //错误代码
                        resultCode = buffer.readUnsignedByte();

                        dataPackResult = new DataPackResult(dataPackObject);
                        dataPackResult.setResultCode(resultCode);

                        //添加分发数据
                        dataPackTargetList.add(new DataPackTarget(dataPackResult));
                        break;
                    default:
                        _logger.info("未知的协议id：" + commandId);
                        break;
                }

            } catch (ParseException | UnsupportedEncodingException e) {
                e.printStackTrace();
            } finally {
                // 释放ByteBuf
                if(null != buffer) {
                    buffer.release();
                }
            }
        }

        return dataPackTargetList;
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
}
