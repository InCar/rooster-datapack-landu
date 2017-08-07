package com.incarcloud.rooster.datapack;

import com.incarcloud.rooster.util.DataTool;
import io.netty.buffer.ByteBuf;

import static io.netty.buffer.Unpooled.buffer;

/**
 * 从服务器取得参数 0x1603
 * Created by zxZhang on 2015/12/7.
 */
public class AcceptServerParameter extends LanDuMsgHead {
    public static final int BUFFER_SIZE = 1024;
    //数据头
    private int commandId;//命令字，short 0x1603=5635
    private String obdCode;//远程诊断仪串号（设备号）
    private Long tripID;// DWORD
    //车辆信息
    private String vid;
    private String vin;
    //模块信息
    private String hardwareVersion;//硬件版本号
    private String firmwareVersion;//固件版本号
    private String softwareVersion;//软件版本号
    private int diagnosisType;//诊断程序类型 0xFF
    //执行动作初值
    private int initCode;//回复出厂设置序号

    /**
     * 解码
     * @param data
     * @return
     */
    public AcceptServerParameter decoded(byte[] data){
        AcceptServerParameter acceptServerParameter = new AcceptServerParameter();
        ByteBuf bb = buffer(BUFFER_SIZE);
        bb.writeBytes(data);
        //包头
        acceptServerParameter.setPackageMark(bb.readUnsignedShort());
        acceptServerParameter.setPackageLength(bb.readUnsignedShort());
        acceptServerParameter.setCheckPackageLength(bb.readUnsignedShort());
        acceptServerParameter.setPackageID(bb.readUnsignedByte());
        acceptServerParameter.setVersion(bb.readUnsignedByte());

        //数据内容
        acceptServerParameter.setCommandId(bb.readUnsignedShort());
        acceptServerParameter.setObdCode(DataTool.readStringZero(bb));
        acceptServerParameter.setTripID(bb.readUnsignedInt());

        acceptServerParameter.setVid(DataTool.readStringZero(bb));
        acceptServerParameter.setVin(DataTool.readStringZero(bb));

        acceptServerParameter.setHardwareVersion(DataTool.readStringZero(bb));
        acceptServerParameter.setFirmwareVersion(DataTool.readStringZero(bb));
        acceptServerParameter.setSoftwareVersion(DataTool.readStringZero(bb));
        acceptServerParameter.setDiagnosisType(bb.readUnsignedByte());

        acceptServerParameter.setInitCode(bb.readUnsignedByte());

        acceptServerParameter.setCheckSum(bb.readUnsignedShort());
        return acceptServerParameter;
    }

    public int getCommandId() {
        return commandId;
    }

    public void setCommandId(int commandId) {
        this.commandId = commandId;
    }

    public String getObdCode() {
        return obdCode;
    }

    public void setObdCode(String obdCode) {
        this.obdCode = obdCode;
    }

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public Long getTripID() {
        return tripID;
    }

    public void setTripID(Long tripID) {
        this.tripID = tripID;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public int getDiagnosisType() {
        return diagnosisType;
    }

    public void setDiagnosisType(int diagnosisType) {
        this.diagnosisType = diagnosisType;
    }

    public int getInitCode() {
        return initCode;
    }

    public void setInitCode(int initCode) {
        this.initCode = initCode;
    }

}
