package com.qhy.util;

import java.util.Calendar;

public class Function {
    public static Long getRandomOrderId() {
        Calendar cal= Calendar.getInstance();
        String order_id = "";

        order_id += String.format("%04d", cal.get(Calendar.YEAR));
        order_id += String.format("%02d", cal.get(Calendar.MONTH)+1);
        order_id += String.format("%02d", cal.get(Calendar.DATE));
        order_id += String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
        order_id += String.format("%02d", cal.get(Calendar.MINUTE));
        order_id += String.format("%02d", cal.get(Calendar.SECOND));

        int random_id = (int)(Math.random()*1000-1);
        order_id += String.valueOf(random_id);

        return Long.valueOf(order_id);
    }
}
