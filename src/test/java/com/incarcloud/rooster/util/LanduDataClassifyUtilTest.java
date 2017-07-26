package com.incarcloud.rooster.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * LanduDataClassifyUtilTest
 *
 * @author Aaric, created on 2017-07-26T11:29.
 * @since 1.0-SNAPSHOT
 */
public class LanduDataClassifyUtilTest {

    @Test
    public void testGet() {
        LanduDataClassifyUtil.Peak peak = LanduDataClassifyUtil.PEAK_MAP.get(0x007D);
        Assert.assertEquals("自发动机起动的时间", peak.getPeakName());
    }
}
