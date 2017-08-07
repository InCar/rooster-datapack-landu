package com.incarcloud.rooster.datapack;

import io.netty.buffer.ByteBufUtil;
import org.junit.Test;

/**
 * LandRespTest
 *
 * @author Aaric, created on 2017-08-07T15:26.
 * @since 2.0
 */
public class LandRespUtilTest {

    @Test
    public void testResp() {
        AcceptServerParameterResp resp = new AcceptServerParameterResp();
        resp.setActionParameterNum(0);
        resp.setNetworkConfigNum(0);
        resp.setSegmentNum(0);
        resp.setLocationParameterNum(0);
        resp.setWarnNum(0);
        resp.setMisFireDataNum(0);
        resp.setDataIDNum(0);
        resp.setUpdateID("V0.00.00");
        byte[] resBytes = resp.encoded(resp, (byte) 0x05);
        System.out.println(ByteBufUtil.hexDump(resBytes));
    }
}
