package com.stableapps.bookmapadapter.util;

public class Constants {

    public enum Market{
        spot, futures
    }
    public static final String API_KEY_FIELD_NAME = "key";
    public static final String API_KEY_FIELD_LABEL = "API key: Key";
    public static final String API_SECRET_FIELD_NAME = "secret";
    public static final String API_SECRET_FIELD_LABEL = "API key: Secret";
    public static final String API_PASSPHRASE_FIELD_NAME = "passphrase";
    public static final String API_PASSPHRASE_FIELD_LABEL = "API key: Passphrase";
    public static final String ENABLE_TRADING_CHECKBOX_NAME = "enableTrading";
    public static final String ENABLE_TRADING_CHECKBOX_LABEL = "Enable trading";

    public static String EXCHANGE;
    public String ADAPTER_FULL_NAME;
    public String ADAPTER_SHORT_NAME;
    public String WS_PORT_NUMBER;
    public String WS_LINK;
    public static final String CLIENT_OID_PREFIX = "BOKMP";
}
