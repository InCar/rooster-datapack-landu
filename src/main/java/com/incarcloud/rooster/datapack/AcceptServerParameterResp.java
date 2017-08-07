package com.incarcloud.rooster.datapack;

import com.incarcloud.rooster.util.DataTool;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static io.netty.buffer.Unpooled.buffer;

/**
 * 从服务器取得参数回复格式 0x1603
 * Created by zxZhang on 2015/12/7.
 */
public class AcceptServerParameterResp extends LanDuMsgHead {
    private Logger _logger = LoggerFactory.getLogger(AcceptServerParameterResp.class);
    public static final int BUFFER_SIZE = 1024;

    private String obdCode;

    private int commandId;//命令字，short 0x1603=5635
    private String currentTime;//YYYY-MM-DD hh:mm:ss
    //执行动作值
    private int actionParameterNum;//动作参数数量 只可是0x00x或0x02
    private int initCode;//回复出厂设置序号
    private int isClearAction;//是否执行清码动作 0xF0:执行,否则不执行
    // 车辆信息
    private int vehicleParameterNum;//车辆信息参数数量 仅可0x00或 0x05；
    private String vid;
    private int brand;//品牌 0xFF默认
    private int series;//系列 0xFF未确定
    private int yearStyle;//年款 0xFF未确定

    private String displacementValue;//排量数值 无结束符，保留一位小数
    private String typeComposition;//类型组成 “L”|“T”
    //上传数据网络配置
    private int networkConfigNum;//网络配置数量
    private String[] ips;//域名IP
    private int[] ports;//端口号
    //车速分段统计设置
    private int segmentNum;//分段数量<=10
    private byte[] maxSpeeds;//最高车速 递增
    //定位数据设置
    private int locationParameterNum;//定位参数数量 0x00 0x03
    private int locationGap;//定位间隔距离 75
    private int locationTime;//定位间隔距离 9
    private int gapAndTime;//距离与时间关系 0x00与关系 0x01或关系
    //报警设置
    private int warnNum;//报警设置参数数量 0x00和0x04
    private int overSpeedMinSpeed;//超速最小速度 120
    private int overSpeedMinTime;//超速报警最小持续时间  6
    private int warnWaterTemperature;//报警水温值
    private int warnChargeVoltage;//充电电压报警值 13.2
    //熄火后数据设置
    private int misFireDataNum;//熄火后数据数量 0x00 0x03
    private int misFireCloseTime;//熄火后关闭时间点 0xFFFF
    private int shutCriticalVoltage;//关机临界电压值 保留一位小数，临界电压值*10

    private int misFireVoltageDataTotal;//熄火后电压数据总数 1-10间
    private byte[] misFireBatteryVoltage;//熄火后电池电压阀值
    //运行中数据设置
    private int dataIDNum;//数据ID数量
    private int[] spaceTime;//间隔时间
    private int[] dataID;//数据ID
    //软件升级ID
    private String updateID;//不超过18字节

    /**
     * 编码
     * @return
     */
    public byte[] encoded(/*AcceptServerParameter reqParams, */AcceptServerParameterResp respParam, byte version) {
        ByteBuf bb = buffer(BUFFER_SIZE);
        int countByte = 0;//消息长度
        int addByte = 0;//增加的字节
        //消息头
        bb.writeShort(0xAA55);//Short((short) 0xAA55);
        bb.markWriterIndex();//标记ByteBuf的writerIndex
        bb.writeShort(0x3030);//填充00，预留packageLength空间
        bb.writeShort(0x3030);//填充00，预留checkPackageLength空间
        //bb.writeByte(reqParam.getPackageID());
        bb.writeByte(0x00);
        //bb.writeByte(reqParam.getVersion());//协议版本格式
        bb.writeByte(version);//协议版本格式
        countByte += 2 + 2 + 2 + 1 + 1;
        //消息内容
        //bb.writeShort(reqParam.getCommandId());//命令字
        bb.writeShort(0x1603);//命令字
        String currentTime = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        DataTool.writeStringZero(bb, currentTime, true);
        addByte += 1;
        countByte += 2 + currentTime.length();
        int actionParameterNum = respParam.getActionParameterNum();
        bb.writeByte(actionParameterNum);
        /*if(actionParameterNum ==0x02){
            bb.writeByte(reqParam.getInitCode());
            bb.writeByte(0xF0);
            countByte += 1+1;
        }*/
        countByte += 1;

        int vehicleParameterNum = respParam.getVehicleParameterNum();
        bb.writeByte(vehicleParameterNum);
        /*if(vehicleParameterNum ==0x05){
            DataTool.writeStringZero(bb, reqParam.getVid(), true);
            bb.writeByte(0xFF);
            bb.writeByte(0xFF);
            bb.writeByte(0xFF);
            String displacement = "1.4" + "L";
            DataTool.writeStringZero(bb, displacement, true);
            countByte += reqParam.getVid().length() + 1 + 1 + 1 + displacement.length();
            addByte += 1 + 1;
        }*/
        countByte +=1;

        int networkConfigNum = respParam.getNetworkConfigNum();
        bb.writeByte(networkConfigNum);
        for(int i = 0;i < networkConfigNum;i ++){
            if(this.getIps()[i].length() <=48){
                DataTool.writeStringZero(bb, this.getIps()[i], true);
            }else {
                _logger.info("_________>>>ip超长");
            }
            bb.writeShort(this.getPorts()[i]);
            countByte +=this.getIps()[i].length()+2;
            addByte +=1;
        }
        countByte +=1;

        int segmentNum = respParam.getSegmentNum();
        if(segmentNum <= 10){
            bb.writeByte(segmentNum);
            for (int i = 0; i < segmentNum;i++){
                if(i <= segmentNum - 2 && this.getMaxSpeeds()[i]<this.getMaxSpeeds()[i+1]){
                    bb.writeByte(this.getMaxSpeeds()[i]);
                    countByte += 1;
                }else if(i == this.getSegmentNum()-1 ){
                    bb.writeByte(this.getMaxSpeeds()[i]);
                    countByte += 1;
                }else {
                    _logger.info("_________>>>第"+i+"分段最高车速不递增");
                }
            }
            countByte +=1;
        }else {
            _logger.info("_________>>>车速分段统计设置分段数量过多");
        }

        int locationParameterNum = respParam.getLocationParameterNum();
        bb.writeByte(locationParameterNum);
        if(locationParameterNum==0x03){
            bb.writeShort(this.getLocationGap()==0? 75: this.getLocationGap());
            bb.writeShort(this.getLocationTime() ==0? 9:this.getLocationTime());
            bb.writeByte(this.getGapAndTime());
            countByte += 2+2+1;
        }else if(this.getLocationParameterNum()==0x00){
        }else {
            _logger.info("_________>>>定位数据设置非法");
        }
        countByte +=1;

        int warnNum = respParam.getWarnNum();
        bb.writeByte(warnNum);
        if(warnNum==0x04){
            bb.writeByte(this.getOverSpeedMinSpeed() == 0 ? 120 : this.getOverSpeedMinSpeed());
            bb.writeShort(this.getOverSpeedMinTime() == 0 ? 6 : this.getOverSpeedMinTime());
            bb.writeShort(this.getWarnWaterTemperature() == 0 ? 110 : this.getWarnWaterTemperature());
            bb.writeByte(this.getWarnChargeVoltage()==0 ? 132: this.getWarnChargeVoltage());
            countByte += 1+2+2+1;
        }else if(this.getWarnNum()==0x00){
        }else {
            _logger.info("_________>>>报警数据设置非法");
        }
        countByte +=1;

        int misFireDataNum = respParam.getMisFireDataNum();
        bb.writeByte(misFireDataNum);
        if(misFireDataNum==0x03){
            bb.writeShort(this.getMisFireCloseTime());
            bb.writeByte(this.getShutCriticalVoltage());
            countByte += 2+1;
            if(this.getMisFireVoltageDataTotal()>=1 && this.getMisFireVoltageDataTotal()<=10){
                bb.writeShort(this.getMisFireVoltageDataTotal());
                bb.writeBytes(this.getMisFireBatteryVoltage());//数组
                countByte += 2+this.getMisFireBatteryVoltage().length;
            }
        }else if(this.getLocationParameterNum()==0x00){
        }else {
            _logger.info("_________>>>熄火后数据设置非法");
        }
        countByte +=1;

        int dataIdNum = respParam.getDataIDNum();
        bb.writeByte(dataIdNum);
        if(dataIdNum!=0){
            for(int i=0;i<this.getDataIDNum();i++){
                bb.writeShort(this.getSpaceTime()[i]);
                bb.writeShort(this.getDataID()[i]);
                countByte+= 1+1;
            }
        }
        countByte +=1;

        String updateId = respParam.getUpdateID();
        if(updateId.length()<=18){
            DataTool.writeStringZero(bb, updateId,true);
            countByte += updateId.length();
            addByte +=1;
        }else {
            _logger.info("_________>>>软件升级ID超长");
            DataTool.writeStringZero(bb, updateId.substring(0, 18), true);
            countByte +=18;
            addByte +=1;
        }

//        countByte += 2;
        int index = bb.writerIndex();
        bb.resetWriterIndex();
        bb.writeShort(countByte + addByte);
        bb.writeShort(~(countByte + addByte));
        bb.writerIndex(index);
        bb.writeShort(LanDuDataTool.getLanduCheckSum(LanDuDataTool.getBytesFromByteBuf(bb)));//checkSum
        bb.readerIndex(0);
        _logger.info("------>>>统计字节个数:" + (countByte + addByte));
        return DataTool.getBytesFromByteBuf(bb);
    }

    public int getCommandId() {
        return commandId;
    }

    public void setCommandId(int commandId) {
        this.commandId = commandId;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public int getActionParameterNum() {
        return actionParameterNum;
    }

    public void setActionParameterNum(int actionParameterNum) {
        this.actionParameterNum = actionParameterNum;
    }

    public int getInitCode() {
        return initCode;
    }

    public void setInitCode(int initCode) {
        this.initCode = initCode;
    }

    public int getIsClearAction() {
        return isClearAction;
    }

    public void setIsClearAction(int isClearAction) {
        this.isClearAction = isClearAction;
    }

    public int getVehicleParameterNum() {
        return vehicleParameterNum;
    }

    public void setVehicleParameterNum(int vehicleParameterNum) {
        this.vehicleParameterNum = vehicleParameterNum;
    }

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public int getBrand() {
        return brand;
    }

    public void setBrand(int brand) {
        this.brand = brand;
    }

    public int getSeries() {
        return series;
    }

    public void setSeries(int series) {
        this.series = series;
    }

    public int getYearStyle() {
        return yearStyle;
    }

    public void setYearStyle(int yearStyle) {
        this.yearStyle = yearStyle;
    }

    public String getDisplacementValue() {
        return displacementValue;
    }

    public void setDisplacementValue(String displacementValue) {
        this.displacementValue = displacementValue;
    }

    public String getTypeComposition() {
        return typeComposition;
    }

    public void setTypeComposition(String typeComposition) {
        this.typeComposition = typeComposition;
    }

    public int getNetworkConfigNum() {
        return networkConfigNum;
    }

    public void setNetworkConfigNum(int networkConfigNum) {
        this.networkConfigNum = networkConfigNum;
    }

    public String[] getIps() {
        return ips;
    }

    public void setIps(String[] ips) {
        this.ips = ips;
    }

    public int[] getPorts() {
        return ports;
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public int getSegmentNum() {
        return segmentNum;
    }

    public void setSegmentNum(int segmentNum) {
        this.segmentNum = segmentNum;
    }

    public byte[] getMaxSpeeds() {
        return maxSpeeds;
    }

    public void setMaxSpeeds(byte[] maxSpeeds) {
        this.maxSpeeds = maxSpeeds;
    }

    public int getLocationParameterNum() {
        return locationParameterNum;
    }

    public void setLocationParameterNum(int locationParameterNum) {
        this.locationParameterNum = locationParameterNum;
    }

    public int getLocationGap() {
        return locationGap;
    }

    public void setLocationGap(int locationGap) {
        this.locationGap = locationGap;
    }

    public int getLocationTime() {
        return locationTime;
    }

    public void setLocationTime(int locationTime) {
        this.locationTime = locationTime;
    }

    public int getGapAndTime() {
        return gapAndTime;
    }

    public void setGapAndTime(int gapAndTime) {
        this.gapAndTime = gapAndTime;
    }

    public int getWarnNum() {
        return warnNum;
    }

    public void setWarnNum(int warnNum) {
        this.warnNum = warnNum;
    }

    public int getOverSpeedMinSpeed() {
        return overSpeedMinSpeed;
    }

    public void setOverSpeedMinSpeed(int overSpeedMinSpeed) {
        this.overSpeedMinSpeed = overSpeedMinSpeed;
    }

    public int getOverSpeedMinTime() {
        return overSpeedMinTime;
    }

    public void setOverSpeedMinTime(int overSpeedMinTime) {
        this.overSpeedMinTime = overSpeedMinTime;
    }

    public int getWarnWaterTemperature() {
        return warnWaterTemperature;
    }

    public void setWarnWaterTemperature(int warnWaterTemperature) {
        this.warnWaterTemperature = warnWaterTemperature;
    }

    public int getWarnChargeVoltage() {
        return warnChargeVoltage;
    }

    public void setWarnChargeVoltage(int warnChargeVoltage) {
        this.warnChargeVoltage = warnChargeVoltage;
    }

    public int getMisFireDataNum() {
        return misFireDataNum;
    }

    public void setMisFireDataNum(int misFireDataNum) {
        this.misFireDataNum = misFireDataNum;
    }

    public int getMisFireCloseTime() {
        return misFireCloseTime;
    }

    public void setMisFireCloseTime(int misFireCloseTime) {
        this.misFireCloseTime = misFireCloseTime;
    }

    public int getShutCriticalVoltage() {
        return shutCriticalVoltage;
    }

    public void setShutCriticalVoltage(int shutCriticalVoltage) {
        this.shutCriticalVoltage = shutCriticalVoltage;
    }

    public int getMisFireVoltageDataTotal() {
        return misFireVoltageDataTotal;
    }

    public void setMisFireVoltageDataTotal(int misFireVoltageDataTotal) {
        this.misFireVoltageDataTotal = misFireVoltageDataTotal;
    }

    public byte[] getMisFireBatteryVoltage() {
        return misFireBatteryVoltage;
    }

    public void setMisFireBatteryVoltage(byte[] misFireBatteryVoltage) {
        this.misFireBatteryVoltage = misFireBatteryVoltage;
    }

    public int getDataIDNum() {
        return dataIDNum;
    }

    public void setDataIDNum(int dataIDNum) {
        this.dataIDNum = dataIDNum;
    }

    public int[] getSpaceTime() {
        return spaceTime;
    }

    public void setSpaceTime(int[] spaceTime) {
        this.spaceTime = spaceTime;
    }

    public int[] getDataID() {
        return dataID;
    }

    public void setDataID(int[] dataID) {
        this.dataID = dataID;
    }

    public String getUpdateID() {
        return updateID;
    }

    public void setUpdateID(String updateID) {
        this.updateID = updateID;
    }



    public void print() {
        _logger.info("LanDuMsg:__________________________");
        _logger.info("packageMark_" + this.getPackageMark()
                + " packageLength_" + this.getPackageLength()
                + " checkPackageLength_" + this.getCheckPackageLength()
                + " packageID_" + this.getPackageID()
                + " version_" + this.getVersion()
                + " checkSum_" + this.getCheckSum());
        _logger.info("LanDuMsgContent:__________________________");
        _logger.info("orderWord_" + this.getCommandId()
                        + " tripID_" + this.getCurrentTime()
                        + " ActionParameterNum_" + this.getActionParameterNum()
                        + " InitCode_" + this.getInitCode()
                        + " IsClearAction_" + this.getIsClearAction()
                        + " VehicleParameterNum_" + this.getVehicleParameterNum()
                        + " VID_" + this.getVid()
                        + " Brand_" + this.getBrand()
                        + " Series_" + this.getSeries()
                        + " YearStyle_" + this.getYearStyle()
                        + " DisplacementValue_" + this.getDisplacementValue()
                        + " TypeComposition_" + this.getTypeComposition()
                        + " NetworkConfigNum_" + this.getNetworkConfigNum()
        );
        for(int i=0;i<this.getIps().length;i++){
            _logger.info(" Ips__"+i+"_*"+ this.getIps()[i]);
        }
        for(int i=0;i<this.getPorts().length;i++){
            _logger.info(" Ports__"+i+"_*"+ this.getPorts()[i]);
        }
        _logger.info(" SegmentNum_" + this.getSegmentNum());
        for(int i=0;i<this.getMaxSpeeds().length;i++){
            _logger.info(" MaxSpeeds_"+i+"_*"+ this.getMaxSpeeds()[i]);
        }
        _logger.info(" LocationParameterNum_" + this.getLocationParameterNum()
                        + " LocationGap_" + this.getLocationGap()
                        + " LocationTime_" + this.getLocationTime()
                        + " GapAndTime_" + this.getGapAndTime()
                        + " WarnNum_" + this.getWarnNum()
                        + " OverSpeedMinSpeed_" + this.getOverSpeedMinSpeed()
                        + " OverSpeedMinTime_" + this.getOverSpeedMinTime()
                        + " WarnWaterTemperature_" + this.getWarnWaterTemperature()
                        + " WarnChargeVoltage_" + this.getWarnChargeVoltage()
                        + " MisFireDataNum_" + this.getMisFireDataNum()
                        + " MisFireCloseTime_" + this.getMisFireCloseTime()
                        + " ShutCriticalVoltage_" + this.getShutCriticalVoltage()
                        + " MisFireVoltageDataTotal_" + this.getMisFireVoltageDataTotal()
        );
        for(int i=0;i<this.getMisFireBatteryVoltage().length;i++){
            _logger.info(" MisFireBatteryVoltage_"+i+"_*"+ this.getMisFireBatteryVoltage()[i]);
        }
        _logger.info(" DataIDNum_" + this.getDataIDNum());
        for(int i=0;i<this.getSpaceTime().length;i++){
            _logger.info(" SpaceTime__"+i+"_*"+ this.getSpaceTime()[i]);
        }
        for(int i=0;i<this.getDataID().length;i++){
            _logger.info(" DataID__"+i+"_*"+ this.getDataID()[i]);
        }
        _logger.info(" UpdateID_" + this.getUpdateID());
    }
}
