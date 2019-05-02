package com.schneider.mstt.synchro.projects.test;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.schneider.mstt.synchro.projects.Common;
import org.springframework.beans.factory.annotation.Autowired;

public class TestCommon {
    
    @Autowired
    private Common common;

    /**
     *
     */
    private static final Logger LOG = Logger.getLogger(TestCommon.class);

    /**
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        SimpleDateFormat aDateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String strDate1 = common.getLastProcessDate();
        Date Date1 = aDateformat.parse(strDate1);

        LOG.info("Str1: " + strDate1);
        LOG.info("Date1: " + Date1);

        Thread.sleep(2000);
        common.updateLastProcessDate();
        LOG.info("");

        String strDate2 = common.getLastProcessDate();
        Date Date2 = aDateformat.parse(strDate2);

        LOG.info("Str2: " + strDate2);
        LOG.info("Date2: " + Date2);
    }

}
