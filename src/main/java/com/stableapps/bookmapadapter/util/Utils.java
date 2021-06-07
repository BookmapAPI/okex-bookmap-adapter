package com.stableapps.bookmapadapter.util;


import com.stableapps.bookmapadapter.util.Constants.Market;

import velox.api.layer1.data.OrderDuration;

public class Utils {

    public static String getDurationType(OrderDuration duration) {
        switch (duration) {
        case GTC:
            return "0";
        case GTC_PO:
            return "1";
        case FOK:
            return "2";
        case IOC:
            return "3";
        default:
            return "0";
        }
    }
    
    public static String getTypeFromALias(String alias) {
        int at = alias.indexOf("@");
        String type = alias.substring(0, at);
        return type;
    }
    
    public static String getInstrumentIdFromALias(String alias) {
        int at = alias.indexOf("@");
        String instrumentId = alias.substring(at + 1);
        return instrumentId;
    }
    
    public static boolean isSpot(String alias) {
        return getTypeFromALias(alias).equals(Market.SPOT.toString());
    }
    
    public static boolean isFutures(String alias) {
        return getTypeFromALias(alias).equals(Market.FUTURES.toString());
    }

    public static String getMarketInstruments(Market market, String exchange) {
        StringBuilder sb = new StringBuilder();
        return sb.append("https://www.")
        .append(exchange)
        .append(".com/api/")
        .append(market.toString().toLowerCase())
        .append("/v3/instruments")
        .toString();
    }
    
    public String getWebSocketLink(String wsPortNumber) {
        StringBuilder sb = new StringBuilder();
        return sb.append("wss://real.okcoin.com:.")
        .append(wsPortNumber)
        .append("/ws/v3")
        .toString();
    }

}
