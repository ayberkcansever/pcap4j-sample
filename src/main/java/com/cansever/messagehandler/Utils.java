package com.cansever.messagehandler;

import org.apache.commons.lang.time.FastDateFormat;

/**
 * User: TTACANSEVER
 */
public class Utils {

    private static FastDateFormat df = FastDateFormat.getInstance("ddMMyyyyHHmmssSSS");

    public static String getFormattedDate() {
        return df.format(System.currentTimeMillis());
    }

}
