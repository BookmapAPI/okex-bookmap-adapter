package com.stableapps.bookmapadapter.provider;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableapps.bookmapadapter.client.AbstractClient;
import com.stableapps.bookmapadapter.client.Connector;
import com.stableapps.bookmapadapter.model.Expiration;
import com.stableapps.bookmapadapter.model.MarketDepth;
import com.stableapps.bookmapadapter.model.MarketDepths;
import com.stableapps.bookmapadapter.model.MarketPrice;
import com.stableapps.bookmapadapter.model.OrderData;
import com.stableapps.bookmapadapter.model.SpotAccount;
import com.stableapps.bookmapadapter.model.SubscribeFuturesAccountResponse;
import com.stableapps.bookmapadapter.model.SubscribeFuturesPositionResponse;
import com.stableapps.bookmapadapter.model.Trade;
import com.stableapps.bookmapadapter.model.rest.InstrumentFutures;
import com.stableapps.bookmapadapter.model.rest.InstrumentGeneric;
import com.stableapps.bookmapadapter.model.rest.InstrumentSpot;
import com.stableapps.bookmapadapter.util.Constants.Market;

import lombok.Data;
import velox.api.layer0.live.ExternalLiveBaseProvider;
import velox.api.layer1.Layer1ApiAdminListener;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.BalanceInfo.BalanceInCurrency;
import velox.api.layer1.data.DefaultAndList;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeatures;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeaturesBuilder;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderUpdateParameters;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.data.SubscribeInfo;
import velox.api.layer1.data.SubscribeInfoCrypto;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.layers.utils.OrderByOrderBook;

/**
 * This provides real time data from OKEX.
 *
 * @author aris
 */

public class RealTimeProvider extends ExternalLiveBaseProvider {
	
	public static final int DEFAULT_MARKET_DEPTH_AMOUNT = 20;
	public static final double FUTURES_GENERIC_MIN_SIZE = 0.001; 

	public Connector connector;
	protected final HashMap<String, Instrument> aliasInstruments;
	protected Thread connectionThread = null;
	protected String apiKey;
	protected String secretKey;
	protected String passPhraze;
	public double priceGranularity = 1.0;
	protected final int leverRate = 10;
	protected final ExecutorService singleThreadExecutor;
	protected final ScheduledExecutorService singleThreadScheduledExecutor;
    protected ObjectMapper objectMapper;
    protected CopyOnWriteArrayList<SubscribeInfo> subscribeInfos = new CopyOnWriteArrayList<>();
    protected Map<String, InstrumentGeneric> genericInstruments = new HashMap<>();
    protected Map<String, Double> futuresMinSizeCalculated = new HashMap<>();
    protected Map<String, Pair<Double, Double>> pipsSizeMultipliers = new HashMap<>();
    protected Map<String, BalanceInCurrency> balanceMap = new HashMap<>();
    protected Map <String, Pair<Integer, Integer>> positionPairsBySymbol = new HashMap<String, Pair<Integer, Integer>>();
    protected Map<String, StatusInfoLocal> aliasedStatusInfos = new ConcurrentHashMap<>();
    public final String exchange;
    public final String wsPortNumber;
    public final String wsLink;
    
    static DecimalFormat df1 = new DecimalFormat(".###");
    Set<Double> askPrices = new TreeSet<>(Collections.reverseOrder());
    Set<Double> bidPrices = new TreeSet<>(Collections.reverseOrder());
    static StringBuilder sb = new StringBuilder();
    OrderBook linkedBook = new OrderBook();
    
    @Data
    public static class MarketDataComponent {
        public MarketDataComponent(String alias, long id, int price, int size, boolean isBid) {
            super();
            this.alias = alias;
            this.id = id;
            this.price = price;
            this.size = size;
            this.isBid = isBid;
        }

        String alias;
        long id;
        int price;
        int size;
        boolean isBid;
    }
    
    @Data
    protected static class StatusInfoLocal{
            String instrumentAlias;
            double unrealizedPnl;
            double realizedPnl;
            String currency;
            int position;
            double averagePrice;
            int volume;
            int workingBuys;
            int workingSells; 
    }

    
    public double getSizeMultiplier(String alias) {
        return pipsSizeMultipliers.get(alias).getRight();
    }
    
    public double getTickSize(String alias) {
        return genericInstruments.get(alias).getTickSize();
    }
     
	public RealTimeProvider(String exchange, String wsPortNumber, String wsLink) {
        aliasInstruments = new HashMap<>();
        singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.exchange = exchange;
        this.wsPortNumber = wsPortNumber;
        this.wsLink = wsLink;
        getInstruments();
        Log.info("new RealTimeProvider has been created");
    }

    protected void updateStatus(StatusInfoLocal info) {
        tradingListeners.forEach(l -> l.onStatus(new StatusInfo(info.getInstrumentAlias(), info.getUnrealizedPnl(),
                info.getRealizedPnl(), info.getCurrency(), info.getPosition(), info.getAveragePrice(), info.getVolume(),
                info.getWorkingBuys(), info.getWorkingSells())));
    }

	protected boolean subscribeDepthAndTrade(String symbol, Market market) {
	    String type = market.toString();
        ((OkexClient)getConnector().client).askOrderBooksGranulated.computeIfAbsent(type + "@" + symbol, v ->  new OrderByOrderBook());
        ((OkexClient)getConnector().client).bidOrderBooksGranulated.computeIfAbsent(type + "@" + symbol, v ->  new OrderByOrderBook());
	    return 
	            getConnector().subscribeContractMarketDepthIncremental(symbol, market)
	            && getConnector().subscribeTrade(symbol, market);
	}

	public void reportFuturesUnrealizedPnl(String alias) {
//	    InstrumentFutures futures = (InstrumentFutures) genericInstruments.get(alias);
	}
	
	@Override
	public void unsubscribe(String alias) {
		synchronized (aliasInstruments) {
            int at = alias.indexOf('@');
            String symbol = alias.substring(at + 1);
            Market market = Market.valueOf(alias.substring(0, at));
			getConnector().unsubscribeContractMarketDepthFull(symbol, market);
			getConnector().unsubscribeTrade(symbol, market);

			aliasedStatusInfos.remove(alias);

			if (aliasInstruments.remove(alias) != null) {
				instrumentListeners.forEach(l -> l.onInstrumentRemoved(alias));
			}
		}
	}

	@Override
	public String formatPrice(String alias, double price) {
		// Use default Bookmap price formatting logic for simplicity.
		// Values returned by this method will be used on price axis and in few
		// other places.
	    double pips = 0.0;
	    try { 
	        pips = pipsSizeMultipliers.get(alias).getLeft();
	    } catch (Exception e) {
	        Log.info(" * * RealTimeProvider formatPrice failed (String alias " + alias);
	        Log.info(" * * pipsSizeMultipliers ");
            pipsSizeMultipliers.entrySet().forEach(f -> Log.info(f.getKey() + " " + f.getValue()));
            Log.info("", e);
            throw e;
        }
        return formatPriceDefault(pips, price);
	}

	@Override
	public void sendOrder(OrderSendParameters orderSendParameters) {
		// This method will not be called because this adapter does not report
		// trading capabilities
		throw new RuntimeException("Not trading capable");
	}

	@Override
	public void updateOrder(OrderUpdateParameters orderUpdateParameters) {
		// This method will not be called because this adapter does not report
		// trading capabilities
		throw new RuntimeException("Not trading capable");
	}

	@Override
	public void login(LoginData loginData) {
        if (!(this instanceof RealTimeTradingProvider)) {
            if (getConnector().session == null) {
                getConnector().connect();
            }
        }
        adminListeners.forEach(Layer1ApiAdminListener::onLoginSuccessful);
	}


	@Override
	public String getSource() {
		// String identifying where data came from.
		// For example you can use that later in your indicator.
		return "realtime demo";
	}

	@Override
	public void close() {
		try {
			Log.info("Closing connector");
            if (connector != null) {
                connector.close();
            }
		} catch (Exception ex) {
			Log.error("Unable to close connector", ex);
		}
	}

	public String createAlias(String symbol, Expiration expiration) {
		return symbol;
	}

	protected void onMarketPrice(String symbol, Expiration expiration, MarketPrice marketPrice) {
	}

	protected void onConnectionRestored() {
	}

	class OkexClient extends AbstractClient {
	    public Map<String, OrderByOrderBook> askOrderBooksGranulated = new ConcurrentHashMap<>();
	    public Map<String, OrderByOrderBook> bidOrderBooksGranulated = new ConcurrentHashMap<>();
	    

        @Override
        public void onMarketDepth(String symbol, String action, MarketDepths marketDepths) {
            String alias = symbol;

            if (action.equals("partial")) {
                resetOrderBooksOnReconnect(alias, true);
                resetOrderBooksOnReconnect(alias, false);
            }

            marketDepths.getAskDatas().forEach(data -> updateOrderBookGranulated(alias, data, false));
            marketDepths.getBidDatas().forEach(data -> updateOrderBookGranulated(alias, data, true));
        }
        
        
        private void resetOrderBooksOnReconnect(String alias, boolean isBid) {
            OrderByOrderBook obob = (isBid ? bidOrderBooksGranulated : askOrderBooksGranulated).get(alias);
            if (obob.getOrderBook().isEmpty()) return;

            Collection<Long> ids = obob.getAllIds();

            for (long id : ids) {
                Object order = obob.getOrder(id);
                Field[] fields = order.getClass().getFields();
                Map<String, Object> objects = new HashMap<>();
                for (Field field : fields) {
                    try {
                        objects.put(field.getName(), field.get(order));
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException();
                    }
                }
                //a workaround for an obfuscated order field
                Object priceObj = objects.get("price");
                if (priceObj == null) {
                    priceObj = objects.get("b");
                }

                int price = (int) priceObj;
                dataListeners.forEach(l -> l.onDepth(alias, isBid, price, 0));
            }
        }
		
		private void updateOrderBookGranulated(String alias, MarketDepth depth, boolean isBid) {
            double tickSize = pipsSizeMultipliers.get(alias).getLeft();
            InstrumentGeneric generic = genericInstruments.get(alias);
            double defaultTickSize = generic.getTickSize();
            long id = (long) Math.round(depth.getPrice() / defaultTickSize);
            
//            int size = depth.getContractAmount() == 0 ?
//                    0 : (int) Math.min(depth.getContractAmount() * pipsSizeMultipliers.get(alias).getRight(), 1);
            int size = (int) (depth.getContractAmount() * pipsSizeMultipliers.get(alias).getRight());

            Function<Double, Double> rounder = isBid? Math::floor : Math::ceil;
            int price = (int) Math.min(rounder.apply(depth.getPrice() / tickSize), Integer.MAX_VALUE);

            OrderByOrderBook orderBook = isBid? bidOrderBooksGranulated.get(alias) : askOrderBooksGranulated.get(alias) ;
            long newSize;

            if (orderBook.hasOrder(id)) {
                if (size == 0) {
                    newSize = orderBook.removeOrder(id);
                } else {
                    OrderByOrderBook.OrderUpdateResult updateOrder = orderBook.updateOrder(id, price, size);
                    newSize = updateOrder.toSize;
                }
            } else {
                newSize = orderBook.addOrder(id, isBid, price, size);
            }
            int absoluteSize = (int) (newSize);
//          int absoluteSize = (int)(newSize * ((InstrumentSpot) generic).getMinSize());
            dataListeners.forEach(l -> l.onDepth(alias, isBid, price, absoluteSize));
		}
		
		@Override
        public void onTradeRecord(String symbol, Expiration expiration, Trade tradeRecord) {
            double pips = pipsSizeMultipliers.get(symbol).getLeft();
            boolean isBidAggressor = tradeRecord.getSide().equals("sell") ? true : false;
            boolean isOtc = false;
            
            InstrumentGeneric generic = genericInstruments.get(symbol);
            int size = (int) (generic instanceof InstrumentSpot
                    ? tradeRecord.getQty() / ((InstrumentSpot) generic).getMinSize()
                    : tradeRecord.getQty());
            
            if (isBidAggressor) {
                dataListeners.forEach(l -> l.onTrade(symbol, Math.floor(tradeRecord.getPrice() / pips),
                        size, new TradeInfo(isOtc, !isBidAggressor)));
            } else {
                dataListeners.forEach(l -> l.onTrade(symbol, Math.ceil(tradeRecord.getPrice() / pips),
                       size, new TradeInfo(isOtc, !isBidAggressor)));

            }

            
            if (RealTimeProvider.this instanceof RealTimeTradingProvider) {
                double unrealizedPnl = ((RealTimeTradingProvider) RealTimeProvider.this)
                        .getUpdatedUnrealizedPnl(symbol, !isBidAggressor, tradeRecord.getPrice());
                
                StatusInfoLocal info = aliasedStatusInfos.computeIfPresent(symbol, (k, v) -> {
                    v.setUnrealizedPnl(unrealizedPnl);
                    return v;
                });
                
                if (aliasedStatusInfos.get(symbol) != null) {
                    updateStatus(info);
                }
            }
        }

		@Override
		public void onOrder(OrderData order) {
		    ((RealTimeTradingProvider)RealTimeProvider.this).onOrder(order);
		}

		@Override
		public void onConnectionLost(Connector.ClosedConnectionType closedConnectionType, String message) {
			Log.info("OkexClient " + this.hashCode() +  ": onConnectionLost() " + closedConnectionType + ", " + message);
        }

        @Override
        public void onConnectionRestored() {
            Log.info("OkexClient " + this.hashCode() +  ": onConnectionRestored()");
            
            if ( RealTimeProvider.this instanceof RealTimeTradingProvider) {
                @SuppressWarnings("resource")
                RealTimeTradingProvider provider = (RealTimeTradingProvider) RealTimeProvider.this;
                provider.connector.wslogin();
            }

            for (String alias : aliasInstruments.keySet()) {
                int at = alias.indexOf('@');
                String symbol = alias.substring(at + 1);
                String type = alias.substring(0, at);
                Pair<Double, Double> pipsSizeMultiplier = pipsSizeMultipliers.get(alias);
                double pips = pipsSizeMultiplier.getLeft();
                Double sizeMultiplier = pipsSizeMultiplier.getRight() == null ? Double.NaN : pipsSizeMultiplier.getRight() ;
                SubscribeInfoCrypto info = new SubscribeInfoCrypto(symbol, "", type, pips, sizeMultiplier);

                isSubscribed(info, true);

                if (RealTimeProvider.this instanceof RealTimeTradingProvider) {
                    @SuppressWarnings("resource")
                    RealTimeTradingProvider provider = (RealTimeTradingProvider) RealTimeProvider.this;
                    provider.getSubscribed(info);
                }
            }
			adminListeners.forEach(listener -> listener.onConnectionRestored());
			RealTimeProvider.this.onConnectionRestored();
		}

        @Override
        public void onPosition() {
            // TODO Auto-generated method stub
        }

        @Override
        public void onFuturesPosition(SubscribeFuturesPositionResponse response) {
            ((RealTimeTradingProvider)RealTimeProvider.this).onFuturesPosition(response);
        }
       
        public int getBestPrice(boolean isBid, String alias) {
            double tickSize = pipsSizeMultipliers.get(alias).getLeft();
            int price;
            
            if (isBid) {
                OrderBook ob = askOrderBooksGranulated.get(alias).getOrderBook();
                price = ob.getWorstAskPriceOrNone();
            } else {
                OrderBook ob = bidOrderBooksGranulated.get(alias).getOrderBook();
                price = ob.getWorstBidPriceOrNone();
            }
            price = (int)Math.round(price * tickSize);
            return price;

        }

        @Override
        public void onSpotAccount(List<SpotAccount> accounts) {
            ((RealTimeTradingProvider)RealTimeProvider.this).onSpotAccount(accounts);
            
        }

        @Override
        public void onFuturesAccount(SubscribeFuturesAccountResponse response) {
            ((RealTimeTradingProvider)RealTimeProvider.this).onFuturesAccount(response);            
        }
       
	}

	/**
	 * @return the connector
	 */
    public Connector getConnector() {
        if (connector == null) {
            connector = new Connector(apiKey, secretKey, passPhraze, new OkexClient(), wsLink, exchange);
            connector.setAdminListeners(adminListeners);
        }
        return connector;
    }
    
    public Connector getNewConnector() {
            connector = new Connector(apiKey, secretKey, passPhraze, new OkexClient(), wsLink, exchange);
            connector.setAdminListeners(adminListeners);
        return connector;
    }


    protected class Instrument {
        protected final String alias;
        protected final double pips;

        public Instrument(String alias, double pips) {
            this.alias = alias;
            this.pips = pips;
        }
    }

    @Override
    public void subscribe(SubscribeInfo subscribeInfo) {
        isSubscribed(subscribeInfo, false);
    }
	
    protected boolean isSubscribed(SubscribeInfo subscribeInfo, boolean isPreviousSubscriptionIgnored) {
        String type = subscribeInfo.type.toUpperCase();
        String symbol = subscribeInfo.symbol;
        String exchange = subscribeInfo.exchange;
        String alias = type + "@" + symbol;

        if (subscribeInfos.stream()
                .filter(instrument -> instrument.type.equals(type))
                .collect(Collectors.toList()).isEmpty()) {
            Log.info("Type " + type + " not found");
            instrumentListeners.forEach(l -> l.onInstrumentNotFound(symbol, exchange, type));
            return false;
        }
        if (subscribeInfos.stream()
                .filter(instrument -> instrument.type.equals(type))
                .filter(instrument -> instrument.symbol.equals(symbol))
                .collect(Collectors.toList()).isEmpty()) {
            
            Log.info("Symbol " + symbol + " not found for the type " + type);
            instrumentListeners.forEach(l -> l.onInstrumentNotFound(symbol, exchange, type));
            return false;
        }
        
        if (genericInstruments.isEmpty()) {
            getInstruments();
        }
        Pair<Double, Double> pair;
        if (subscribeInfo instanceof SubscribeInfoCrypto) {
            Log.info("isInfoCrypro");
            SubscribeInfoCrypto subscribeInfoCrypto = (SubscribeInfoCrypto) subscribeInfo;
            pair = new ImmutablePair<Double, Double>(subscribeInfoCrypto.pips,
                    subscribeInfoCrypto.sizeMultiplier);
        } else {
            //this is a workaround for a saving alias parameters in Bookmap
            //A default tickSize will be accepted if no saved pipsMultipilier
            InstrumentGeneric generic = genericInstruments.get(alias);
            Double minSizeInverted;
            if (generic instanceof InstrumentSpot) {
                double minSizeInvertedRough = 1/((InstrumentSpot)generic).getMinSize();
                double minSizeInvertedRounded = Math.round(minSizeInvertedRough);
                minSizeInverted = minSizeInvertedRounded == 0 ? minSizeInvertedRough : minSizeInvertedRounded;
            } else if (generic instanceof InstrumentFutures) {
                minSizeInverted = 1.0;
            } else {
                minSizeInverted = null;
            }
            pair = new ImmutablePair<Double, Double>(genericInstruments.get(alias).getTickSize(), minSizeInverted);
        }

        Callable<Boolean> callableTask = () -> {
            synchronized (aliasInstruments) {
                Log.info("Subscribing to " + alias);

                if (!isPreviousSubscriptionIgnored && aliasInstruments.containsKey(alias)) {
                    Log.info("Already subscribed to " + alias);
                    instrumentListeners.forEach(l -> l.onInstrumentAlreadySubscribed(symbol, exchange, type));
                    return false;
                }

                Pair<Double, Double> previousMultiplier = pipsSizeMultipliers.get(alias);
                Log.info("pipsSizeMultipliers put alias " + alias + " pips " + pair.getLeft());
                pipsSizeMultipliers.put(alias, pair);
                
                boolean isSubscribedDepthTrade = subscribeDepthAndTrade(symbol, Market.valueOf(type));
                if (!isSubscribedDepthTrade) {
                    Log.info("Failed to subscribed to " + alias);
                    instrumentListeners.forEach(l -> l.onInstrumentNotFound(symbol, exchange, type));
                    
                    if (previousMultiplier != null) {
                        Log.info("pipsSizeMultipliers put back alias " + alias + " pips " + previousMultiplier);
                        pipsSizeMultipliers.put(alias, previousMultiplier);
                    }
                    return false;
                }
                
                double tickSize = pipsSizeMultipliers.get(alias).getLeft();
                double pips = genericInstruments.get(alias).getTickSize();
                final Instrument instrument = new Instrument(alias, pips);
                aliasInstruments.put(alias, instrument);
                Log.info("instrumentInfo to BM alias " + alias + " tickSize " + tickSize);
                double sizeMultiplier = (double) ObjectUtils.firstNonNull(pipsSizeMultipliers.get(alias).getRight(), 1.0);
                Log.info("sizeMultiplier " + sizeMultiplier);

                final InstrumentInfo instrumentInfo = new InstrumentInfo(symbol, exchange, type, tickSize, 1, null, true, sizeMultiplier);

                Log.info("Now subscribed to " + alias);
                if (!isPreviousSubscriptionIgnored) {
                    Log.info("onInstrumentAdded " + instrumentInfo.toString());
                instrumentListeners.forEach(l -> l.onInstrumentAdded(alias, instrumentInfo));
                }
                return true;
            }
        };

        Future<Boolean> future = singleThreadExecutor.submit(callableTask);

        boolean result = false;
        try {
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

	
	@Override
    public Layer1ApiProviderSupportedFeatures getSupportedFeatures() {
        // Expanding parent supported features, reporting basic trading support
        Layer1ApiProviderSupportedFeaturesBuilder a = super.getSupportedFeatures().toBuilder()
        .setExchangeUsedForSubscription(false)
        .setKnownInstruments(subscribeInfos)
        .setPipsFunction(s -> {
            String alias = getAlias(s);
            
            if (!genericInstruments.containsKey(alias)) {
                return null;
            }

            InstrumentGeneric generic = genericInstruments.get(alias);
            double basicTickSize = generic.getTickSize();
            List<Double> options = new ArrayList<>();
            options.add((double) basicTickSize); 
            options.add((double) 5 * basicTickSize); 
            options.add((double) 10 * basicTickSize); 
            options.add((double) 50 * basicTickSize); 
            options.add((double) 100 * basicTickSize); 
            options.add((double) 500 * basicTickSize); 
            options.add((double) 1_000 * basicTickSize); 
            options.add((double) 5_000 * basicTickSize); 
            options.add((double) 10_000 * basicTickSize); 
            return new DefaultAndList<Double>(basicTickSize, options);
        })
        .setSizeMultiplierFunction(s -> {
            String alias = getAlias(s);
            
            if (!genericInstruments.containsKey(alias)) {
                return null;
            }

            InstrumentGeneric generic = genericInstruments.get(alias);
            double minSize;
            if (generic instanceof InstrumentSpot) {
                minSize = ((InstrumentSpot)generic).getMinSize();
            } else if (generic instanceof InstrumentFutures) {
                return new DefaultAndList<Double>(1.0, Collections.singletonList(1.0));
            } else {
                return null;
            }
            
            if (minSize >= 1) {
                return new DefaultAndList<Double>(1/minSize, Collections.singletonList(1/minSize));
            }
            double invertedRoundedMinSize = Math.round(1/minSize);
            List<Double> options = new ArrayList<>();
            
            double optionSize = invertedRoundedMinSize;
            while (optionSize > 0) {
                options.add(optionSize);
                optionSize = Math.round(optionSize/10);
            }
            return new DefaultAndList<Double>(invertedRoundedMinSize, options);
        });
        return a.build();
    }
	
	private String getAlias(SubscribeInfo subscribeInfo) {
	    StringBuilder sb = new StringBuilder(subscribeInfo.type)
	            .append("@")
	            .append(subscribeInfo.symbol);
	    return sb.toString();
	}
	
	protected void getInstruments() {
	    
    }

}
