package com.incarcloud.rooster.datapack;

import com.incarcloud.rooster.datatarget.*;
import com.incarcloud.rooster.util.DataTool;
import com.incarcloud.rooster.util.LanduDataPackUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * LANDU Parser.
 *
 * @author Aaric
 * @since 2.0
 */
public class DataParserLandu implements IDataParser {

    static {
        /**
         * 声明数据包版本与解析器类关系
         */
        /*DataParserManager.register("china-landu-2.05", DataParserManager.class);*/
        DataParserManager.register("china-landu-3.08", DataParserManager.class);
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
                                dataPack = new DataPack("china", "landu", version);
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

    /**
     * 回复数据
     *
     * @param bytes 原始数据
     * @param errorCode 错误代码
     * @return
     */
    private byte[] responseBytes(byte[] bytes, byte errorCode) {
        byte[] returnBytes = null;
        if(validate(bytes)) {
            returnBytes = new byte[13];
            // 1.数据包标志
            returnBytes[0] = bytes[0];
            returnBytes[1] = bytes[1];
            // 2.数据包长度(固定长度13-2=11)
            returnBytes[2] = 0x00;
            returnBytes[3] = (byte) (13-2);
            // 3.数据包长度校验
            returnBytes[4] = (byte) ~returnBytes[2];
            returnBytes[5] = (byte) ~returnBytes[3];
            // 4.数据包ID
            returnBytes[6] = bytes[6];
            // 5.协议格式版本
            returnBytes[7] = bytes[7];
            // 6.数据内容(命令字)
            returnBytes[8] = bytes[8];
            returnBytes[9] = bytes[9];
            returnBytes[10] = errorCode; // 0x00-成功
            // 7.校验和
            int sum = 0;
            for (int i = 2; i < returnBytes.length - 2; i++) {
                sum += (returnBytes[i] & 0xFF);
            }
            returnBytes[11] = (byte) ((sum >> 8) & 0xFF);
            returnBytes[12] = (byte) (sum & 0xFF);
        }
        return returnBytes;
    }

    @Override
    public ByteBuf createResponse(DataPack requestPack, ERespReason reason) {
        ByteBuf responseBuf = null;
        // 回复设备数据
        if(null != requestPack && ERespReason.OK == reason) {
            // 校验数据
            byte[] originalBytes = Base64.getDecoder().decode(requestPack.getDataB64());
            // 封装返回数据(成功返回0x00)
            byte[] returnBytes = responseBytes(originalBytes, (byte) 0x00);
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
            DataTarget dataTarget = new DataTarget("landu");
            DataTargetOverview dataTargetOverview;
            DataTargetPosition dataTargetPosition;

            try {
                // 初始化ByteBuf
                buffer = Unpooled.wrappedBuffer(dataPackBytes);

                // 跳过“标志+长度+长度校验”6个字节
                LanduDataPackUtil.readBytes(buffer, 6);

                // 数据包ID
                dataTarget.setPackId(LanduDataPackUtil.readByte(buffer));

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
                dataTarget.setProtocolVersion(version);

                // 命令字
                switch (LanduDataPackUtil.readUInt2(buffer)) {
                    case 0x1601:
                        System.out.println("## 0x1601 - 3.1.1 车辆检测数据主动上传");
                        // 1.设备号
                        dataTarget.setObdCode(LanduDataPackUtil.readString(buffer));
                        // 2.TripID
                        dataTarget.setTripId(LanduDataPackUtil.readDWord(buffer));
                        // 3.VID
                        dataTarget.setVid(LanduDataPackUtil.readString(buffer));
                        // 4.VIN
                        dataTarget.setVin(LanduDataPackUtil.readString(buffer));
                        // 5.检测数据时间
                        dataTarget.setDetectionDate(LanduDataPackUtil.readDate(buffer));

                        // 6.车辆状态
                        switch (LanduDataPackUtil.readByte(buffer)) {
                            case 0x01:
                                // 0x01-发动机点火时
                                System.out.println("## 发动机点火时");
                                // 6.1 整车数据
                                dataTargetOverview = new DataTargetOverview(dataTarget);
                                // 6.1.1 车辆状态
                                dataTargetOverview.setStatus(0x01);
                                // 6.1.2 启动电压(V)
                                dataTargetOverview.setVoltage(Float.parseFloat(LanduDataPackUtil.readString(buffer)));
                                // 6.1.3 车速(km/h)
                                dataTargetOverview.setSpeed(Float.parseFloat(LanduDataPackUtil.readString(buffer)));
                                // 6.1.4 当前行程行驶距离(m)
                                dataTargetOverview.setTravelDistance(Integer.parseInt(LanduDataPackUtil.readString(buffer)));
                                // --add
                                dataPackTargetList.add(new DataPackTarget(ETargetType.OVERVIEW, dataTargetOverview));
                                // 6.2 定位信息
                                dataTargetPosition = new DataTargetPosition(dataTarget);
                                String[] positions = LanduDataPackUtil.splitPositionString(buffer);
                                // 6.2.1.经度
                                dataTargetPosition.setLongitude(LanduDataPackUtil.parsePositionString(positions[0]));
                                // 6.2.2 纬度
                                dataTargetPosition.setLatitude(LanduDataPackUtil.parsePositionString(positions[1]));
                                // 6.2.2 方向
                                dataTargetPosition.setDirection(positions[2]);
                                // 6.2.3 定位时间
                                dataTargetPosition.setPositionDate(LanduDataPackUtil.formatDateString(positions[3]));
                                // 6.2.4 定位方式：0-无效数据，1-基站定位，2-GPS 定位
                                dataTargetPosition.setPositioMode(Integer.parseInt(positions[4]));
                                // --add
                                dataPackTargetList.add(new DataPackTarget(ETargetType.POSITION, dataTargetPosition));
                                break;
                            case 0x02:
                                // 0x02-发动机运行中
                                System.out.println("## 发动机运行中");
                                int count = buffer.readUnsignedShort();
                                if(count > 0){
                                    List<DataTargetPeak> dataTargetPeakList = new ArrayList<DataTargetPeak>();
                                    for(int i = 0; i < count; i++){
                                        DataTargetPeak dataTargetPeak = new DataTargetPeak();
                                        //数据项id
                                        Integer id = buffer.readUnsignedShort();
                                        //数据项内容
                                        String content = DataTool.readStringZero(buffer);

                                        dataTargetPeak.setPeakName(Integer.toHexString(id));
                                        dataTargetPeak.setPeakValue(content);

                                        dataTargetPeakList.add(dataTargetPeak);
                                    }
                                    DataTargetSet dataTargetSet = new DataTargetSet();
                                    dataTargetSet.setDataTargetPeakList(dataTargetPeakList);

                                    dataPackTargetList.add(new DataPackTarget(ETargetType.PEAKLIST, dataTargetSet));
                                }
                                break;
                            case 0x03:
                                // 0x03-发动机熄火时
                                System.out.println("## 发动机熄火时");
                                dataTargetOverview = new DataTargetOverview(dataTarget);
                                dataTargetOverview.setStatus(0x03);
                                //本行程数据小计
                                //本次发动机运行时间
                                dataTargetOverview.setEngineRunningTime(buffer.readUnsignedShort());
                                //本次行驶距离
                                dataTargetOverview.setTravelDistance(buffer.readInt());
                                Integer averageFuelConsumption = buffer.readUnsignedShort();
                                //本次平均油耗
                                dataTargetOverview.setAverageFuelConsumption(averageFuelConsumption.floatValue()/100f);
                                //累计行驶里程
                                dataTargetOverview.setTotalTravelDistance(buffer.readInt());
                                Integer totalAverageFuelConsumption = buffer.readUnsignedShort();
                                //累计平均油耗
                                dataTargetOverview.setTotalAverageFuelConsumption(totalAverageFuelConsumption.floatValue()/100f);
                                //车速分组统计
                                count = buffer.readUnsignedByte();
                                Integer[] speeds = new Integer[count];
                                Integer[] times = new Integer[count];
                                Integer[] gaps = new Integer[count];
                                for(int i = 0;i < count;i++){
                                    speeds[i] = (int) buffer.readUnsignedByte();
                                    times[i]= buffer.readUnsignedShort();
                                    gaps[i] = buffer.readInt();
                                }
                                //设置速度值
                                dataTargetOverview.setSpeedSet(speeds);
                                //时间小计
                                dataTargetOverview.setSubTotalTime(times);
                                //距离小计
                                dataTargetOverview.setSubTotalDistance(gaps);
                                //驾驶习惯统计
                                //本次急加速次数
                                dataTargetOverview.setSuddenUp(buffer.readUnsignedShort());
                                //本次急减速次数
                                dataTargetOverview.setSuddenDec(buffer.readUnsignedShort());
                                //本次急转向次数
                                dataTargetOverview.setSuddenTurn(buffer.readUnsignedShort());
                                //本次时速超速时间
                                dataTargetOverview.setSpeedingTime(buffer.readInt());
                                //最高车速
                                dataTargetOverview.setMaxSpeed((int) buffer.readUnsignedByte());
                                //车速
                                dataTargetOverview.setSpeed(Float.parseFloat(DataTool.readStringZero(buffer)));
                                //当前行程行驶距离
                                dataTargetOverview.setTravelDistance(Integer.parseInt(DataTool.readStringZero(buffer)));
                                // --add
                                dataPackTargetList.add(new DataPackTarget(ETargetType.OVERVIEW, dataTargetOverview));
                                //定位信息
                                dataTargetPosition = new DataTargetPosition(dataTarget);
                                positions = DataTool.readStringZero(buffer).split(",");
                                // 6.2.1.经度
                                String lngStr = positions[0];
                                Double lng = 0d;
                                if(lngStr != null && !"-".equals(lngStr)){
                                    lng = Double.parseDouble(lngStr);
                                }
                                dataTargetPosition.setLongitude(lng);
                                // 6.2.2 纬度
                                String latStr = positions[1];
                                Double lat = 0d;
                                if(latStr != null && !"-".equals(latStr)){
                                    lat = Double.parseDouble(latStr);
                                }
                                dataTargetPosition.setLatitude(lat);
                                // 6.2.2 方向
                                dataTargetPosition.setDirection(positions[2]);
                                // 6.2.3 定位时间
                                dataTargetPosition.setPositionDate(LanduDataPackUtil.formatDateString(positions[3]));
                                // 6.2.4 定位方式：0-无效数据，1-基站定位，2-GPS 定位
                                dataTargetPosition.setPositioMode(Integer.parseInt(positions[4]));
                                // --add
                                dataPackTargetList.add(new DataPackTarget(ETargetType.POSITION, dataTargetPosition));
                                break;
                            case 0x04:
                                // 0x04-发动机熄火后
                                System.out.println("## 发动机熄火后");
                                dataTargetOverview = new DataTargetOverview(dataTarget);
                                dataTargetOverview.setStatus(0x04);
                                //蓄电池电压值
                                dataTargetOverview.setVoltage(Float.parseFloat(DataTool.readStringZero(buffer)));
                                // --add
                                dataPackTargetList.add(new DataPackTarget(ETargetType.OVERVIEW, dataTargetOverview));
                                break;
                            case 0x05:
                                // 0x05-车辆不能检测
                                System.out.println("## 车辆不能检测");
                                break;
                        }
                        break;
                    case 0x1602:
                        System.out.println("## 0x1602 - 3.1.2 上传车辆报警");
                        break;
                    case 0x1603:
                        System.out.println("## 0x1603 - 3.1.3 从服务器取得参数");
                        break;
                    case 0x1605:
                        System.out.println("## 0x1605 - 3.1.4 上传调试数据");
                        break;
                    case 0x1606:
                        System.out.println("## 0x1606 - 3.1.5 位置数据");
                        break;
                    case 0x1607:
                        System.out.println("## 0x1607 - 3.1.6 冻结帧数据");
                        break;
                    case 0x1608:
                        System.out.println("## 0x1608 - 3.1.7 怠速车况数据");
                        break;
                    case 0x160A:
                        System.out.println("## 0x160A - 3.1.9 行为位置数据");
                        break;
                    case 0x1621:
                        System.out.println("## 0x1621 - 3.2.2 取得车辆当前检测数据");
                        break;
                    case 0x1622:
                        System.out.println("## 0x1622 - 3.2.3 根据索引 ID 取得相应的检测数据");
                        break;
                    case 0x1623:
                        System.out.println("## 0x1623 - 3.2.4 车辆诊断参数设定");
                        break;
                    case 0x1624:
                        System.out.println("## 0x1624 - 3.2.5 清空累计平均油耗");
                        break;
                    case 0x1625:
                        System.out.println("## 0x1625 - 3.2.6 取得系统版本信息");
                        break;
                    case 0x1626:
                        System.out.println("## 0x1626 - 3.2.7 清除车辆故障码");
                        break;
                    case 0x16E0:
                        System.out.println("## 0x16E0 - 3.3.1 恢复出厂设置");
                        break;
                }

            } catch (ParseException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
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
}
