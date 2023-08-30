package com.qhy.util;

public class Constant {
    private void Constant() {}

    public static final class Common {
        private static final String MAX_ORDER_ID_KEY = "MAX_ORDER_ID";
        private static final String ORDER_LIST_KEY = "ORDER";
        private static final String STOCK_MARKET_PRICE_KEY = "STOCK_MARKET_PRICE_KEY";
        private static final Long SHOWED_ORDER_NUMBER = 5L;

        private void Common() {}

        public static String MAX_ORDER_ID_KEY() {
            return MAX_ORDER_ID_KEY;
        }
        public static String ORDER_LIST_KEY() {
            return ORDER_LIST_KEY;
        }
        public static String STOCK_MARKET_PRICE() {
            return STOCK_MARKET_PRICE_KEY;
        }
        public static Long SHOWED_ORDER_NUMBER() {
            return SHOWED_ORDER_NUMBER;
        }
    }

}
