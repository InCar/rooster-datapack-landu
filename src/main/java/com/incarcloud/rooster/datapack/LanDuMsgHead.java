package com.incarcloud.rooster.datapack;

import com.incarcloud.rooster.util.DataTool;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Created by zxZhang on 2015/12/2.
 */
public class LanDuMsgHead {

    protected Logger _logger = LoggerFactory.getLogger(getClass());
    private static final int BUFFER_SIZE =1024;

    private int packageMark;//数据包标志 0xAA55
    private int packageLength;//数据包长度
    private int checkPackageLength;//数据包长度校验,即数据包长度取反
    private int packageID;//数据包ID
    private int version;//协议格式版本 0x05
    private int checkSum;//校验和

    public LanDuMsgHead decoded(byte[] data){
        LanDuMsgHead lanDuMsgHead = new LanDuMsgHead();
        ByteBuf bb = buffer(BUFFER_SIZE);
        bb.writeBytes(data);
        try{
            lanDuMsgHead.setPackageMark(bb.readShort());
            lanDuMsgHead.setPackageLength(bb.readShort());
            lanDuMsgHead.setCheckPackageLength(bb.readShort());
            lanDuMsgHead.setPackageID(bb.readByte());
            lanDuMsgHead.setVersion(bb.readByte());
            lanDuMsgHead.setCheckSum(bb.readShort());
        }catch (Exception e){
            _logger.error("封装LanDuMsgHead" + "");
            e.printStackTrace();
        }
        return new LanDuMsgHead();
    }

    public byte[] encoded(){
        ByteBuf bb = buffer(BUFFER_SIZE);
        try{
            bb.writeShort(0xAA55);
            bb.writeShort(8);
            bb.writeShort(-8);
            bb.writeByte(this.getPackageID());
            bb.writeByte(0x05);
            bb.writeShort(6);
        } catch (Exception e){
            _logger.error("封装LanDdMsgHead消息头二进制数组失败。");
            e.printStackTrace();
        }
        return DataTool.getBytesFromByteBuf(bb);
    }

    public LanDuMsgHead(){
        super();
    }

    public Logger get_logger() {
        return _logger;
    }

    public void set_logger(Logger _logger) {
        this._logger = _logger;
    }

    public int getPackageMark() {
        return packageMark;
    }

    public void setPackageMark(int packageMark) {
        this.packageMark = packageMark;
    }

    public int getPackageLength() {
        return packageLength;
    }

    public void setPackageLength(int packageLength) {
        this.packageLength = packageLength;
    }

    public int getCheckPackageLength() {
        return checkPackageLength;
    }

    public void setCheckPackageLength(int checkPackageLength) {
        this.checkPackageLength = checkPackageLength;
    }

    public int getPackageID() {
        return packageID;
    }

    public void setPackageID(int packageID) {
        this.packageID = packageID;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(int checkSum) {
        this.checkSum = checkSum;
    }
}
