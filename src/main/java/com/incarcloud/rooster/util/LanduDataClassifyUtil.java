package com.incarcloud.rooster.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LANDU 分类工具类
 *
 * @author Aaric, created on 2017-07-26T09:42.
 * @since 2.0
 */
public class LanduDataClassifyUtil {

    /**
     * 极值对象
     */
    public static class Peak {

        /**
         * 极值ID
         */
        private Integer peakId;
        /**
         * 极值名称
         */
        private String peakName;
        /**
         * 极值单位
         */
        private String peakUnit;
        /**
         * 极值描述
         */
        private String peakDesc;

        public Peak(Integer peakId, String peakUnit, String peakName, String peakDesc) {
            this.peakId = peakId;
            this.peakName = peakName;
            this.peakUnit = peakUnit;
            this.peakDesc = peakDesc;
        }

        public Integer getPeakId() {
            return peakId;
        }

        public void setPeakId(Integer peakId) {
            this.peakId = peakId;
        }

        public String getPeakName() {
            return peakName;
        }

        public void setPeakName(String peakName) {
            this.peakName = peakName;
        }

        public String getPeakUnit() {
            return peakUnit;
        }

        public void setPeakUnit(String peakUnit) {
            this.peakUnit = peakUnit;
        }

        public String getPeakDesc() {
            return peakDesc;
        }

        public void setPeakDesc(String peakDesc) {
            this.peakDesc = peakDesc;
        }
    }

    /**
     * 极值数据表
     */
    public static Map<Integer, Peak> PEAK_MAP = new LinkedHashMap<>();

    static {
        /**
         * 构建极值数据表
         */
        PEAK_MAP.put(0x0000, new Peak(0x0000, "伏特", "蓄电池电压", "%.1f，（00~18）"));
        PEAK_MAP.put(0x0001, new Peak(0x0001, "", "存储在电子控制单元中的故障码个数", "%d，（0~127）"));
        PEAK_MAP.put(0x0002, new Peak(0x0002, "", "故障灯状态", "%s，（关 开）"));
        PEAK_MAP.put(0x0003, new Peak(0x0003, "", "对应所存储的冻结帧的故障码", "%s%04X，（P C B U）"));
        PEAK_MAP.put(0x0004, new Peak(0x0004, "", "燃油系统 1 状态", "%s，（OL CL OL-Drive）"));
        PEAK_MAP.put(0x0005, new Peak(0x0005, "", "燃油系统 2 状态", "%s，（OL CL OL-Drive）"));
        PEAK_MAP.put(0x0006, new Peak(0x0006, "%", "计算负荷值", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x0007, new Peak(0x0007, "℃", "发动机冷却液温度", "%d，（-40~215）"));
        PEAK_MAP.put(0x0008, new Peak(0x0008, "%", "第 1 列的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x0009, new Peak(0x0009, "%", "第 3 列的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x000A, new Peak(0x000A, "%", "第 1 列的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x000B, new Peak(0x000B, "%", "第 3 列的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x000C, new Peak(0x000C, "%", "第 2 列的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x000D, new Peak(0x000D, "%", "第 4 列的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x000E, new Peak(0x000E, "%", "第 2 列的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x000F, new Peak(0x000F, "%", "第 4 列的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x0010, new Peak(0x0010, "千帕", "油轨压力", "%d，（0~765）"));
        PEAK_MAP.put(0x0011, new Peak(0x0011, "千帕", "进气歧管绝对压力", "%d，（0~255）"));
        PEAK_MAP.put(0x0012, new Peak(0x0012, "转/分", "发动机转速", "%.0f，（0~16383.75）"));
        PEAK_MAP.put(0x0013, new Peak(0x0013, "km/h", "车速", "%d，（0~255）"));
        PEAK_MAP.put(0x0014, new Peak(0x0014, "°", "1 号汽缸点火正时提前角", "%.0f，（-64~63.5）"));
        PEAK_MAP.put(0x0015, new Peak(0x0015, "℃", "进气温度", "%d，（-40~215）"));
        PEAK_MAP.put(0x0016, new Peak(0x0016, "克/秒", "空气流量传感器的空气流量", "%.2f，（0~655.35）"));
        PEAK_MAP.put(0x0017, new Peak(0x0017, "%", "绝对节气门位置", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x0018, new Peak(0x0018, "", "二次空气状态指令", "%s，（UPS DNS OFF）"));
        PEAK_MAP.put(0x0019, new Peak(0x0019, "", "氧传感器的位置", "%s，（O2S11 O2S12 O2S13）"));
        PEAK_MAP.put(0x001A, new Peak(0x001A, "伏特", "第 1 列氧传感器 1 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x001B, new Peak(0x001B, "%", "第 1 列传感器 1 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x001C, new Peak(0x001C, "伏特", "第 1 列氧传感器 2 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x001D, new Peak(0x001D, "%", "第 1 列传感器 2 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x001E, new Peak(0x001E, "伏特", "第 1 列氧传感器 3 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x001F, new Peak(0x001F, "%", "第 1 列传感器 3 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0020, new Peak(0x0020, "伏特", "第 1 列氧传感器 4 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0021, new Peak(0x0021, "%", "第 1 列传感器 4 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0022, new Peak(0x0022, "伏特", "第 2 列氧传感器 1 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0023, new Peak(0x0023, "%", "第 2 列传感器 1 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0024, new Peak(0x0024, "伏特", "第 2 列传感器 2 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0025, new Peak(0x0025, "%", "第 2 列传感器 2 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0026, new Peak(0x0026, "伏特", "第 2 列传感器 3 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0027, new Peak(0x0027, "%", "第 2 列传感器 3 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0028, new Peak(0x0028, "伏特", "第 2 列传感器 4 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0029, new Peak(0x0029, "%", "第 2 列传感器 4 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x002A, new Peak(0x002A, "", "第 1 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x002B, new Peak(0x002B, "伏特", "第 1 列氧传感器 1 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x002C, new Peak(0x002C, "", "第 1 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x002D, new Peak(0x002D, "伏特", "第 1 列氧传感器 2 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x002E, new Peak(0x002E, "", "第 1 列氧传感器 3 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x002F, new Peak(0x002F, "伏特", "第 1 列氧传感器 3 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0030, new Peak(0x0030, "", "第 1 列氧传感器 4 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0031, new Peak(0x0031, "伏特", "第 1 列氧传感器 4 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0032, new Peak(0x0032, "", "第 2 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0033, new Peak(0x0033, "伏特", "第 2 列氧传感器 1 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0034, new Peak(0x0034, "", "第 2 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0035, new Peak(0x0035, "伏特", "第 2 列氧传感器 2 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0036, new Peak(0x0036, "", "第 2 列氧传感器 3 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0037, new Peak(0x0037, "伏特", "第 2 列氧传感器 3 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0038, new Peak(0x0038, "", "第 2 列氧传感器 4 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0039, new Peak(0x0039, "伏特", "第 2 列氧传感器 4 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x003A, new Peak(0x003A, "", "第 1 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x003B, new Peak(0x003B, "毫安", "第 1 列氧传感器 1 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x003C, new Peak(0x003C, "", "第 1 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x003D, new Peak(0x003D, "毫安", "第 1 列氧传感器 2 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x003E, new Peak(0x003E, "", "第 1 列氧传感器 3 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x003F, new Peak(0x003F, "毫安", "第 1 列氧传感器 3 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0040, new Peak(0x0040, "", "第 1 列氧传感器 4 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0041, new Peak(0x0041, "毫安", "第 1 列氧传感器 4 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0042, new Peak(0x0042, "", "第 2 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0043, new Peak(0x0043, "毫安", "第 2 列氧传感器 1 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0044, new Peak(0x0044, "", "第 2 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0045, new Peak(0x0045, "毫安", "第 2 列氧传感器 2 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0046, new Peak(0x0046, "", "第 2 列氧传感器 3 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0047, new Peak(0x0047, "毫安", "第 2 列氧传感器 3 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0048, new Peak(0x0048, "", "第 2 列氧传感器 4 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0049, new Peak(0x0049, "毫安", "第 2 列氧传感器 4 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x004A, new Peak(0x004A, "OBD", "系统的车辆设计要求", "%s，（[OBD II] [EOBD]…）"));
        PEAK_MAP.put(0x004B, new Peak(0x004B, "", "氧传感器的位置", "%s，（O2S11 O2S12）"));
        PEAK_MAP.put(0x004C, new Peak(0x004C, "伏特", "第 1 列传感器 1 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x004D, new Peak(0x004D, "%", "第 1 列传感器 1 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x004E, new Peak(0x004E, "伏特", "第 1 列传感器 2 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x004F, new Peak(0x004F, "%", "第 1 列传感器 2 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0050, new Peak(0x0050, "伏特", "第 2 列传感器 1 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0051, new Peak(0x0051, "%", "第 2 列传感器 1 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0052, new Peak(0x0052, "伏特", "第 2 列传感器 2 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0053, new Peak(0x0053, "%", "第 2 列传感器 2 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0054, new Peak(0x0054, "伏特", "第 3 列传感器 1 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0055, new Peak(0x0055, "%", "第 3 列传感器 1 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0056, new Peak(0x0056, "伏特", "第 3 列传感器 2 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0057, new Peak(0x0057, "%", "第 3 列传感器 2 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x0058, new Peak(0x0058, "伏特", "第 4 列传感器 1 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x0059, new Peak(0x0059, "%", "第 4 列传感器 1 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x005A, new Peak(0x005A, "伏特", "第 4 列传感器 2 的输出电压", "%.3f，（0~1.275）"));
        PEAK_MAP.put(0x005B, new Peak(0x005B, "%", "第 4 列传感器 2 的短期燃油修正", "%.1f，（-100.00~99.22）"));
        PEAK_MAP.put(0x005C, new Peak(0x005C, "", "第 1 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x005D, new Peak(0x005D, "伏特", "第 1 列氧传感器 1 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x005E, new Peak(0x005E, "", "第 1 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x005F, new Peak(0x005F, "伏特", "第 1 列氧传感器 2 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0060, new Peak(0x0060, "", "第 2 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0061, new Peak(0x0061, "伏特", "第 2 列氧传感器 1 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0062, new Peak(0x0062, "", "第 2 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0063, new Peak(0x0063, "伏特", "第 2 列氧传感器 2 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0064, new Peak(0x0064, "", "第 3 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0065, new Peak(0x0065, "伏特", "第 3 列氧传感器 1 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0066, new Peak(0x0066, "", "第 3 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0067, new Peak(0x0067, "伏特", "第 3 列氧传感器 2 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x0068, new Peak(0x0068, "", "第 4 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0069, new Peak(0x0069, "伏特", "第 4 列氧传感器 1 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x006A, new Peak(0x006A, "", "第 4 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x006B, new Peak(0x006B, "伏特", "第 4 列氧传感器 2 的电压", "%.3f，（0~7.999）"));
        PEAK_MAP.put(0x006C, new Peak(0x006C, "", "第 1 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x006D, new Peak(0x006D, "毫安", "第 1 列氧传感器 1 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x006E, new Peak(0x006E, "", "第 1 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x006F, new Peak(0x006F, "毫安", "第 1 列氧传感器 2 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0070, new Peak(0x0070, "", "第 2 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0071, new Peak(0x0071, "毫安", "第 2 列氧传感器 1 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0072, new Peak(0x0072, "", "第 2 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0073, new Peak(0x0073, "毫安", "第 2 列氧传感器 2 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0074, new Peak(0x0074, "", "第 3 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0075, new Peak(0x0075, "毫安", "第 3 列氧传感器 1 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0076, new Peak(0x0076, "", "第 3 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0077, new Peak(0x0077, "毫安", "第 3 列氧传感器 2 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x0078, new Peak(0x0078, "", "第 4 列氧传感器 1 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x0079, new Peak(0x0079, "毫安", "第 4 列氧传感器 1 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x007A, new Peak(0x007A, "", "第 4 列氧传感器 2 的等效比", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x007B, new Peak(0x007B, "毫安", "第 4 列氧传感器 2 的电流", "%.3f，（-128~127.996）"));
        PEAK_MAP.put(0x007C, new Peak(0x007C, "", "辅助输入状态", "%s，（关 开）"));
        PEAK_MAP.put(0x007D, new Peak(0x007D, "秒", "自发动机起动的时间", "%d，（0~65535）"));
        PEAK_MAP.put(0x007E, new Peak(0x007E, "千米", "在故障指示灯激活状态下行驶的里程", "%d，（0~65535）"));
        PEAK_MAP.put(0x007F, new Peak(0x007F, "千帕", "相对于歧管真空度的油轨压力", "%.3f，（0~5177.27）"));
        PEAK_MAP.put(0x0080, new Peak(0x0080, "千帕", "相对于大气压力的油轨压力", "%d，（0~655350）"));
        PEAK_MAP.put(0x0081, new Peak(0x0081, "%", "废气再循环系统指令开度", "%.1f，（0~100）"));
        PEAK_MAP.put(0x0082, new Peak(0x0082, "%", "EGR 开度误差", "(实际开度 — 指令开度)/指令开度*100%，%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x0083, new Peak(0x0083, "%", "蒸发冲洗控制指令", "%.1f，（0~100）"));
        PEAK_MAP.put(0x0084, new Peak(0x0084, "%", "燃油液位输入", "%.1f，（0~100）"));
        PEAK_MAP.put(0x0085, new Peak(0x0085, "", "故障码清除后的暖机次数", "%d，（0~255）"));
        PEAK_MAP.put(0x0086, new Peak(0x0086, "千米", "故障码清除后的行驶里程", "%d，（0~65535）"));
        PEAK_MAP.put(0x0087, new Peak(0x0087, "Pa", "蒸发系统的蒸气压力", "%.2f，（-8192.0~0.0）"));
        PEAK_MAP.put(0x0088, new Peak(0x0088, "千帕", "大气压", "%d，（0~255）"));
        PEAK_MAP.put(0x0089, new Peak(0x0089, "℃", "第 1 列传感器 1 的催化剂温度", "%.1f，（-40~6513.5）"));
        PEAK_MAP.put(0x008A, new Peak(0x008A, "℃", "第 2 列传感器 1 的催化剂温度", "%.1f，（-40~6513.5）"));
        PEAK_MAP.put(0x008B, new Peak(0x008B, "℃", "第 1 列传感器 2 的催化剂温度", "%.1f，（-40~6513.5）"));
        PEAK_MAP.put(0x008C, new Peak(0x008C, "℃", "第 2 列传感器 2 的催化剂温度", "%.1f，（-40~6513.5）"));
        PEAK_MAP.put(0x008D, new Peak(0x008D, "", "失火监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x008E, new Peak(0x008E, "", "燃油系统监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x008F, new Peak(0x008F, "", "综合元件监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x0090, new Peak(0x0090, "", "失火监控", "%s，（完成 没完成）"));
        PEAK_MAP.put(0x0091, new Peak(0x0091, "", "燃油系统监控", "%s，（完成 没完成）"));
        PEAK_MAP.put(0x0092, new Peak(0x0092, "", "综合元件监控", "%s，（完成 没完成）"));
        PEAK_MAP.put(0x0093, new Peak(0x0093, "", "催化剂监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x0094, new Peak(0x0094, "", "加热型催化剂监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x0095, new Peak(0x0095, "", "燃油蒸发系统监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x0096, new Peak(0x0096, "", "二次空气系统监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x0097, new Peak(0x0097, "", "空调系统制冷剂监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x0098, new Peak(0x0098, "", "氧传感器监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x0099, new Peak(0x0099, "", "氧传感器加热器监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x009A, new Peak(0x009A, "", "废气再循环系统监控", "%s，（无效 有效）"));
        PEAK_MAP.put(0x009B, new Peak(0x009B, "", "催化剂监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x009C, new Peak(0x009C, "", "加热型催化剂监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x009D, new Peak(0x009D, "", "燃油蒸发系统监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x009E, new Peak(0x009E, "", "二次空气系统监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x009F, new Peak(0x009F, "", "空调系统制冷剂监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x00A0, new Peak(0x00A0, "", "氧传感器监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x00A1, new Peak(0x00A1, "", "氧传感器加热器监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x00A2, new Peak(0x00A2, "", "废气再循环系统监控", "%s，（未完成 完成）"));
        PEAK_MAP.put(0x00A3, new Peak(0x00A3, "伏特", "控制单元电压", "%.3f，（0~65.535）"));
        PEAK_MAP.put(0x00A4, new Peak(0x00A4, "%", "绝对负载值", "%.1f，（0~25700.0）"));
        PEAK_MAP.put(0x00A5, new Peak(0x00A5, "", "等效比指令", "%.3f，（0~1.999）"));
        PEAK_MAP.put(0x00A6, new Peak(0x00A6, "%", "相对节气门位置", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00A7, new Peak(0x00A7, "℃", "环境空气温度", "%d，（-40~215）"));
        PEAK_MAP.put(0x00A8, new Peak(0x00A8, "%", "绝对节气门位置 B", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00A9, new Peak(0x00A9, "%", "绝对节气门位置 C", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00AA, new Peak(0x00AA, "%", "加速踏板位置 D", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00AB, new Peak(0x00AB, "%", "加速踏板位置 E", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00AC, new Peak(0x00AC, "%", "加速踏板位置 F", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00AD, new Peak(0x00AD, "%", "节气门执行器控制指令", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00AE, new Peak(0x00AE, "分钟", "故障指示灯处于激活状态下的发动机运转时间", "%d，（0~65535）"));
        PEAK_MAP.put(0x00AF, new Peak(0x00AF, "分钟", "自故障码清除之后的时间", "%d，（0~65535）"));
        PEAK_MAP.put(0x00B0, new Peak(0x00B0, "", "最大等效比", "%d，（0~255）"));
        PEAK_MAP.put(0x00B1, new Peak(0x00B1, "伏特", "氧传感器最高电压", "%d，（0~255）"));
        PEAK_MAP.put(0x00B2, new Peak(0x00B2, "毫安", "氧传感器最大电流", "%d，（0~255）"));
        PEAK_MAP.put(0x00B3, new Peak(0x00B3, "千帕", "进气歧管最大绝对压力", "%d，（0~2550）"));
        PEAK_MAP.put(0x00B4, new Peak(0x00B4, "加仑/秒", "空气流量传感器的最大流量", "%d，（0~2550）"));
        PEAK_MAP.put(0x00B5, new Peak(0x00B5, "", "车辆当前使用的燃油类型", "%s，（Protocol GAS）"));
        PEAK_MAP.put(0x00B6, new Peak(0x00B6, "%", "酒精燃料百分比", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00B7, new Peak(0x00B7, "千帕", "蒸发系统蒸气绝对压力", "%.3f，（0~327.675）"));
        PEAK_MAP.put(0x00B8, new Peak(0x00B8, "帕", "蒸发系统蒸气压力", "%d，（-32767~32768）"));
        PEAK_MAP.put(0x00B9, new Peak(0x00B9, "%", "第 1 列氧传感器 2 的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00BA, new Peak(0x00BA, "%", "第 3 列氧传感器 2 的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00BB, new Peak(0x00BB, "%", "第 1 列氧传感器 2 的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00BC, new Peak(0x00BC, "%", "第 3 列氧传感器 2 的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00BD, new Peak(0x00BD, "%", "第 2 列氧传感器 2 的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00BE, new Peak(0x00BE, "%", "第 4 列氧传感器 2 的短期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00BF, new Peak(0x00BF, "%", "第 2 列氧传感器 2 的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00C0, new Peak(0x00C0, "%", "第 4 列氧传感器 2 的长期燃油修正", "%.1f，（-100~99.22）"));
        PEAK_MAP.put(0x00C1, new Peak(0x00C1, "千帕", "油轨绝对压力", "%d,，（0~2550）"));
        PEAK_MAP.put(0x00C2, new Peak(0x00C2, "%", "加速踏板相对位置", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00C3, new Peak(0x00C3, "%", "混合电池组剩余寿命", "%.1f，（0~100.0）"));
        PEAK_MAP.put(0x00C4, new Peak(0x00C4, "摄氏度", "发动机机油温度", "%d，（-40~215）"));
        PEAK_MAP.put(0x00C5, new Peak(0x00C5, "度", "喷油正时角", "%.2f，（-210.0~301.992）"));
        PEAK_MAP.put(0x00C6, new Peak(0x00C6, "升/小时", "发动机耗油率", "%.2f，（0~3276.75）"));
        PEAK_MAP.put(0x00C7, new Peak(0x00C7, "", "车辆设计的排放物要求", "%s，（[---] [EURO C]）"));
        PEAK_MAP.put(0x00C8, new Peak(0x00C8, "%", "驾驶员要求的发动机转矩百分比", "%d，（-125~130）"));
        PEAK_MAP.put(0x00C9, new Peak(0x00C9, "", "发动机实际转矩百分比", "%d，（-125~130）"));
        PEAK_MAP.put(0x00CA, new Peak(0x00CA, "", "发动机基准转矩", "%d，（0~65535）"));
        PEAK_MAP.put(0x00CB, new Peak(0x00CB, "%", "发动机在怠速点 1 的转矩百分比", "%d，（-125~130）"));
        PEAK_MAP.put(0x00CC, new Peak(0x00CC, "%", "发动机在怠速点 2 的转矩百分比", "%d，（-125~130）"));
        PEAK_MAP.put(0x00CD, new Peak(0x00CD, "%", "发动机在怠速点 3 的转矩百分比", "%d，（-125~130）"));
        PEAK_MAP.put(0x00CE, new Peak(0x00CE, "%", "发动机在怠速点 4 的转矩百分比", "%d，（-125~130）"));
    }

    protected LanduDataClassifyUtil(){}
}
