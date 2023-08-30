package com.qhy.util;

public class Constant {
    private void Constant() {}

    public static final class Common {
        private static final String SELL_ORDER_BOOK_NAME = "SELL";
        private static final String BUY_ORDER_BOOK_NAME = "BUY";
        private static final String MAX_ORDER_ID_NAME = "MAX_ORDER_ID_NAME";
        private static final String ORDER_BOOK_NAME = "ORDER";
        private static final String SHARE_PRICE_NAME = "SHARE";
        private static final Long SHOWED_ORDER_NUMBER = 5L;

        private void Common() {}

        public static String SELL_LIST() {
            return SELL_ORDER_BOOK_NAME;
        }
        public static String BUY_LIST() {
            return BUY_ORDER_BOOK_NAME;
        }
        public static String MAX_ORDER_ID_NAME() {
            return MAX_ORDER_ID_NAME;
        }
        public static String ORDER_BOOK() {
            return ORDER_BOOK_NAME;
        }
        public static String SHARE_PRICE() {
            return SHARE_PRICE_NAME;
        }
        public static Long SHOWED_ORDER_NUMBER() {
            return SHOWED_ORDER_NUMBER;
        }
    }

}
