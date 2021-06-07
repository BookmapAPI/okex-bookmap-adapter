package com.stableapps.okex;

import com.stableapps.bookmapadapter.client.Connector;
import com.stableapps.bookmapadapter.model.rest.InstrumentFutures;
import com.stableapps.bookmapadapter.model.rest.InstrumentSpot;
import com.stableapps.bookmapadapter.provider.RealTimeTradingProvider;
import com.stableapps.bookmapadapter.util.Constants.Market;
import com.stableapps.bookmapadapter.util.Utils;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.SubscribeInfo;

public class OkexRealTimeTradingProvider extends RealTimeTradingProvider {

    public OkexRealTimeTradingProvider(String exchange, String wsPortNumber, String wsLink) {
        super(exchange, wsPortNumber, wsLink);
    }

    /* TODO: this method is copied from OkexRealTimeProvider
     * because the class already extends RealTimeTradingProvider */
    @Override
    protected void getInstruments() {
        if (subscribeInfos.isEmpty() || genericInstruments.isEmpty()) {

            try {
                String futuresContent = Connector
                        .getServerResponse(Utils.getMarketInstruments(Market.FUTURES, super.exchange));
                InstrumentFutures[] futures = objectMapper.readValue(futuresContent, InstrumentFutures[].class);

                for (InstrumentFutures future : futures) {
                    subscribeInfos.add(new SubscribeInfo(future.getInstrumentId(), "", Market.FUTURES.toString()));
                    genericInstruments.put(Market.FUTURES.toString() + "@" + future.getInstrumentId(), future);
                }
            } catch (Exception e) {
                Log.warn("Futures instruments have not been loaded", e);
            }

            try {
                String spotContent = Connector
                        .getServerResponse(Utils.getMarketInstruments(Market.SPOT, super.exchange));
                InstrumentSpot[] spots = objectMapper.readValue(spotContent, InstrumentSpot[].class);
                for (InstrumentSpot spot : spots) {
                    subscribeInfos.add(new SubscribeInfo(spot.getInstrumentId(), "", Market.SPOT.toString()));
                    genericInstruments.put(Market.SPOT.toString() + "@" + spot.getInstrumentId(), spot);
                }
            } catch (Exception e) {
                Log.warn("Spot instruments have not been loaded", e);
            }
        }
    }
}
