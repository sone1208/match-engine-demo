package com.qhy.util;

public class Constant {
    private void Constant() {}

    public static final class Common {
        private static final String ORDER_LIST_KEY = "ORDER";
        private static final String STOCK_MARKET_PRICE_KEY = "STOCK_MARKET_PRICE_KEY";
        private static final String BUY_ORDER_LIST_KEY_PRE = "BUY_";
        private static final String SELL_ORDER_LIST_KEY_PRE = "SELL_";
        private static final String TMP_MATCH_RECORDS_KEY = "TMP_MATCH_RECORDS";
        private static final String TMP_TAKER_ORDERS_KEY = "TMP_TAKER_ORDERS";
        private static final String TMP_TRADING_RECORDS_KEY = "TMP_TRADING_RECORDS";
        private static final String TMP_NEW_TAKER_ORDERS_KEY = "TMP_NEW_TAKER_ORDERS";
        private static final String TMP_CANCEL_TAKER_ORDERS_KEY = "TMP_CANCEL_TAKER_ORDERS";
        private static final Long SHOWED_ORDER_NUMBER = 5L;

        private void Common() {}

        public static String ORDER_LIST_KEY() {
            return ORDER_LIST_KEY;
        }
        public static String STOCK_MARKET_PRICE() {
            return STOCK_MARKET_PRICE_KEY;
        }
        public static String BUY_ORDER_LIST_KEY_PRE() {
            return BUY_ORDER_LIST_KEY_PRE;
        }
        public static String SELL_ORDER_LIST_KEY_PRE() {
            return SELL_ORDER_LIST_KEY_PRE;
        }
        public static String TMP_MATCH_RECORDS_KEY() {
            return TMP_MATCH_RECORDS_KEY;
        }
        public static String TMP_TAKER_ORDERS_KEY() {
            return TMP_TAKER_ORDERS_KEY;
        }
        public static String TMP_TRADING_RECORDS_KEY() {
            return TMP_TRADING_RECORDS_KEY;
        }
        public static String TMP_NEW_TAKER_ORDERS_KEY() {
            return TMP_NEW_TAKER_ORDERS_KEY;
        }
        public static String TMP_CANCEL_TAKER_ORDERS_KEY() {
            return TMP_CANCEL_TAKER_ORDERS_KEY;
        }
        public static Long SHOWED_ORDER_NUMBER() {
            return SHOWED_ORDER_NUMBER;
        }
    }

}
