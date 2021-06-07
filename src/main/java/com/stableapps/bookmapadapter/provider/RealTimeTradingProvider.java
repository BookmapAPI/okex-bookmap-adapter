/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.provider;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.java_websocket.util.Base64;

import com.stableapps.bookmapadapter.model.Expiration;
import com.stableapps.bookmapadapter.model.FutureAccountsContractFixedMargin;
import com.stableapps.bookmapadapter.model.FuturesAccount;
import com.stableapps.bookmapadapter.model.FuturesPosition;
import com.stableapps.bookmapadapter.model.FuturesPositionsOfCurrencyResponse;
import com.stableapps.bookmapadapter.model.OrderData;
import com.stableapps.bookmapadapter.model.OrderDataFutures;
import com.stableapps.bookmapadapter.model.OrderDataSpot;
import com.stableapps.bookmapadapter.model.OrderTypeFuturesOrSwap;
import com.stableapps.bookmapadapter.model.OrdersFuturesList;
import com.stableapps.bookmapadapter.model.SpotAccount;
import com.stableapps.bookmapadapter.model.SubscribeFuturesAccountResponse;
import com.stableapps.bookmapadapter.model.SubscribeFuturesPositionResponse;
import com.stableapps.bookmapadapter.model.rest.AccountSpot;
import com.stableapps.bookmapadapter.model.rest.AccountsFuturesCrossMargin;
import com.stableapps.bookmapadapter.model.rest.AccountsFuturesFixedMarginResponse;
import com.stableapps.bookmapadapter.model.rest.InstrumentFutures;
import com.stableapps.bookmapadapter.model.rest.InstrumentGeneric;
import com.stableapps.bookmapadapter.model.rest.InstrumentSpot;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderRequest;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderRequestFuturesOrSwap;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderRequestTokenOrMargin;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderResponse;
import com.stableapps.bookmapadapter.rest.RestClient;
import com.stableapps.bookmapadapter.util.Constants;
import com.stableapps.bookmapadapter.util.Constants.Market;
import com.stableapps.bookmapadapter.util.Utils;

import lombok.Data;
import velox.api.layer0.credentialscomponents.CredentialsSerializationField;
import velox.api.layer1.Layer1ApiAdminListener;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.BalanceInfo.BalanceInCurrency;
import velox.api.layer1.data.ExecutionInfoBuilder;
import velox.api.layer1.data.ExtendedLoginData;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeatures;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.LoginFailedReason;
import velox.api.layer1.data.OrderCancelParameters;
import velox.api.layer1.data.OrderDuration;
import velox.api.layer1.data.OrderInfoBuilder;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderStatus;
import velox.api.layer1.data.OrderType;
import velox.api.layer1.data.OrderUpdateParameters;
import velox.api.layer1.data.SimpleOrderSendParameters;
import velox.api.layer1.data.SubscribeInfo;
import velox.api.layer1.data.SystemTextMessageType;

/**
 * @author aris
 */

public class RealTimeTradingProvider extends RealTimeProvider {

	private static final String ONE_DIR_CLOSE_CLIENT_ID = "OneDirectionCloseClientID";
	private static boolean RUN_MAIN = false;
	public static final EnumSet<OrderTypeFuturesOrSwap> LONG_ORDER_TYPES
		= EnumSet.of(OrderTypeFuturesOrSwap.OpenLongPosition, OrderTypeFuturesOrSwap.CloseShortPosition);
	public static final EnumSet<OrderTypeFuturesOrSwap> SHORT_ORDER_TYPES
		= EnumSet.of(OrderTypeFuturesOrSwap.OpenShortPosition, OrderTypeFuturesOrSwap.CloseLongPosition);

	HashMap<Long, String> okexBmIds = new HashMap<>();
	HashMap<String, Long> bmOkexIds = new HashMap<>();
	private final HashMap<String, OrderInfoBuilder> bmIdWorkingOrders = new HashMap<>();
	private final HashMap<String, OrderInfoBuilder> bmIdSentOrders = new HashMap<>();
	private final HashMap<String, String> clientOidToOrderId= new HashMap<>();
	private final HashMap<String, String> orderIdToClientOid = new HashMap<>();
	private Map <String, Pair <Integer, Integer>> positionsMap = new HashMap<>();
	private Map <String, Set<String>> currenciesForSpotBalance = new HashMap<>();
	private Map <String, Set<String>> currenciesForSpotPosition = new HashMap<>();
	private Map<String, UnrealizedPnlData> unrealizedPnlMap = new HashMap<>();
	
	
	@Data
	private static class UnrealizedPnlData{
	    int qty;
	    double avrCost;
	    double lastBidTrade;
	    double lastAskTrade;
	}
	
	public RealTimeTradingProvider(String exchange, String wsPortNumber, String wsLink) {
		super(exchange, wsPortNumber, wsLink);
	}
	
	@Override
	public Layer1ApiProviderSupportedFeatures getSupportedFeatures() {
		// Expanding parent supported features, reporting basic trading support
		return super.getSupportedFeatures()
			.toBuilder()
			.setExchangeUsedForSubscription(false)
			.setBalanceSupported(true)
			.setTrading(true)
			.setSupportedStopOrders(Arrays.asList(new OrderType[] {
                    OrderType.MKT,
                    OrderType.LMT
            }))
			.setSupportedOrderDurations(Arrays.asList(new OrderDuration[]{
			        OrderDuration.GTC,
			        OrderDuration.GTC_PO,
			        OrderDuration.FOK,
			        OrderDuration.IOC,
			        }))
			.build();
	}
	
    @Override
    public void login(LoginData loginData) {
        ExtendedLoginData extendedLoginData = (ExtendedLoginData) loginData;
        Map<String, CredentialsSerializationField> credentialsMap = extendedLoginData.extendedData;

        this.apiKey = credentialsMap.get(Constants.API_KEY_FIELD_NAME).getStringValue();
        this.secretKey = credentialsMap.get(Constants.API_SECRET_FIELD_NAME).getStringValue();
        this.passPhraze = credentialsMap.get(Constants.API_PASSPHRASE_FIELD_NAME).getStringValue();
        boolean isTradingEnabled = Boolean.valueOf(
                extendedLoginData.extendedData.get(Constants.ENABLE_TRADING_CHECKBOX_NAME).getStringValue());
        boolean areCredentialsProvided = StringUtils.isNoneBlank(apiKey, secretKey, passPhraze);

        if (isTradingEnabled && !areCredentialsProvided) {
            adminListeners.forEach(l -> l.onLoginFailed(
                    LoginFailedReason.WRONG_CREDENTIALS,
                    "Credentials field is empty"));
            return;
        }
        connectionThread = new Thread(() -> handleLogin());
        connectionThread.start();
    }

    private void handleLogin() {
        Log.info("Logging in to OKEX");
        boolean isValid = getNewConnector().wslogin();

        if (isValid) {
            Log.info("Login to OKEX successful");
            adminListeners.forEach(Layer1ApiAdminListener::onLoginSuccessful);
        } else {
            Log.info("Login to OKEX failed");
            adminListeners.forEach(l -> l.onLoginFailed(
                    LoginFailedReason.WRONG_CREDENTIALS,
                    connector.getLoginResponseMessage()));
            close();
        }
    }

	@Override
	protected void onConnectionRestored() {
        Log.info("\t\tOkexClient (TradingProvider)" + this.hashCode() +  ": onConnectionRestored()");
	}

	@Override
	public void sendOrder(OrderSendParameters orderSendParameters) {
		SimpleOrderSendParameters simpleParameters = (SimpleOrderSendParameters) orderSendParameters;
		String symbol = simpleParameters.alias;
		Expiration expiration = null;

		OrderType orderType = OrderType.getTypeFromPrices(simpleParameters.stopPrice, simpleParameters.limitPrice);
		if (RUN_MAIN) {
			orderType = OrderType.LMT;
		}
		Log.info("Order Type: " + orderType);

		String bmId = UUID.randomUUID().toString();
		final OrderInfoBuilder orderInfo = new OrderInfoBuilder(
			simpleParameters.alias,
			bmId,
			simpleParameters.isBuy,
			orderType,
			simpleParameters.clientId.equals(ONE_DIR_CLOSE_CLIENT_ID)
			? null : simpleParameters.clientId,
			simpleParameters.doNotIncrease);

		Log.info("Limit Price: " + simpleParameters.limitPrice);
		Log.info("Stop Price: " + simpleParameters.stopPrice);
		orderInfo.setStopPrice(simpleParameters.stopPrice)
			.setLimitPrice(simpleParameters.limitPrice)
			.setUnfilled(simpleParameters.size)
			.setDuration(simpleParameters.duration)
			.setStatus(OrderStatus.PENDING_SUBMIT);
		tradingListeners.forEach(l -> l.onOrderUpdated(orderInfo.build()));

		orderInfo.markAllUnchanged();
		
		if (orderType == OrderType.STP 
		        || orderType == OrderType.STP_LMT 
		        || simpleParameters.stopLossOffset != 0
		        || simpleParameters.takeProfitOffset != 0
		        || simpleParameters.trailingStep > 0
		        ) {
			orderInfo.setStatus(OrderStatus.REJECTED);
			tradingListeners.forEach(l -> l.onOrderUpdated(orderInfo.build()));
			orderInfo.markAllUnchanged();

			adminListeners.forEach(l -> l.onSystemTextMessage(
						"StopLoss or TakeProfit orders are not supported at the moment",
						SystemTextMessageType.ORDER_FAILURE
					)
				);
			return;
		}

		int size = simpleParameters.size;
		double price = simpleParameters.limitPrice;
		int position = 0;
		StatusInfoLocal statusInfo = aliasedStatusInfos.get(simpleParameters.alias);
		if (statusInfo != null) {
			position = statusInfo.position;
		}
		OrderTypeFuturesOrSwap okexOrderType = determineOkexOrderType(orderType,
			simpleParameters.isBuy, position, simpleParameters.clientId);
		Log.info("OkexOrderType: " + okexOrderType);
		sendOrder(symbol, expiration, okexOrderType, size, price,
			orderInfo);
	}

    private void sendOrder(String alias, Expiration expiration, OrderTypeFuturesOrSwap orderType, int size,
            double price, final OrderInfoBuilder orderInfo) {

        PlaceOrderRequest orderRequest = new PlaceOrderRequest();
        String instrumentType = alias.substring(0, alias.indexOf('@'));
        String symbol = alias.substring(alias.indexOf('@') + 1);

        switch (Market.valueOf(instrumentType)) {
        case SPOT:
            PlaceOrderRequestTokenOrMargin spotRequest = new PlaceOrderRequestTokenOrMargin();
            spotRequest.setType("limit");
            spotRequest.setSide(orderInfo.isBuy() ? "buy" : "sell");
            spotRequest.setInstrumentId(symbol);
            spotRequest.setMarginTrading(1);//
            spotRequest.setDuration(Utils.getDurationType(orderInfo.getDuration()));

            if (orderInfo.getType() == OrderType.LMT) {
                spotRequest.setPrice(price);
                spotRequest.setFloatingPointSize(size / getSizeMultiplier(alias));
            } else if (orderInfo.getType() == OrderType.MKT) {
                spotRequest.setFloatingPointSize(size / getSizeMultiplier(alias));
                int bestPrice = ((OkexClient) connector.client).getBestPrice(orderInfo.isBuy(), alias);
                spotRequest.setPrice(bestPrice);
            }

            orderRequest = spotRequest;
            break;
        case MARGIN:
            PlaceOrderRequestTokenOrMargin marginRequest = new PlaceOrderRequestTokenOrMargin();
            marginRequest.setType(orderInfo.getType().equals(OrderType.MKT) ? "market" : "limit");
            marginRequest.setMarginTrading(2);
            orderRequest = marginRequest;
            break;
        case FUTURES:
            PlaceOrderRequestFuturesOrSwap futures = new PlaceOrderRequestFuturesOrSwap();
            futures.setType(orderInfo.isBuy() ? "1" : "2");
            futures.setInstrumentId(symbol);
            futures.setOrderType(Utils.getDurationType(orderInfo.getDuration()));

            if (orderInfo.getType().equals(OrderType.MKT)) {
                futures.setMatchPrice("1");
            } else {
                futures.setPrice(orderInfo.getLimitPrice());
            }
            futures.setLeverage(String.valueOf(leverRate));
            futures.setFloatingPointSize(null);
            futures.setSize(size);//
            orderRequest = futures;
            break;

        }

        String clientOid = orderInfo.getClientId();

        try {
            byte[] decoded = Base64.decode(clientOid);
            Base32 base32 = new Base32();
            StringBuilder sb = new StringBuilder();
            sb.append(Constants.CLIENT_OID_PREFIX).append(base32.encodeAsString(decoded));

            while (sb.charAt(sb.length() - 1) == '=') {
                sb.deleteCharAt(sb.length() - 1);
            }
            clientOid = sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        orderRequest.setClientOrderId(clientOid);
        final String workaroundId = clientOid;
        PlaceOrderRequest workaroundRequest = orderRequest;
        
        int positionLong = 0;
        int positionShort = 0;
        boolean isCodirectionalWithPosition = true;

        if (instrumentType.equals(Market.FUTURES.toString())) {
            positionLong = positionsMap.get(alias).getRight();
            positionShort = positionsMap.get(alias).getLeft();
            isCodirectionalWithPosition = positionsMap.get(alias) == null || positionShort == positionLong
                    || !orderInfo.isBuy() && positionShort > positionLong
                    || orderInfo.isBuy() && positionLong > positionShort;
        }

        if (instrumentType.equals(Market.FUTURES.toString()) && orderInfo.getType() == OrderType.MKT && !isCodirectionalWithPosition) {

            if (size == Math.abs(positionLong - positionShort)) {

                if (positionLong != 0) {
                    closeFuturesPosition(workaroundRequest, workaroundId, orderInfo, "3", positionLong);
                }
                if (positionShort != 0) {
                    closeFuturesPosition(workaroundRequest, workaroundId, orderInfo, "4", positionShort);
                }
            } else {
                if (orderInfo.isBuy()) {
                    closeFuturesPosition(workaroundRequest, workaroundId, orderInfo, "4", size);
                } else {
                    closeFuturesPosition(workaroundRequest, workaroundId, orderInfo, "3", size);
                }
            }
        } else {

            singleThreadExecutor.submit(() -> {
                Log.debug("+++Enter synchronize");
                synchronized (bmIdSentOrders) {
                    bmIdSentOrders.put(workaroundId, orderInfo);

                    orderInfo.setStatus(OrderStatus.PENDING_SUBMIT);
                    tradingListeners.forEach(l -> l.onOrderUpdated(orderInfo.build()));
                    orderInfo.markAllUnchanged();

                    PlaceOrderResponse orderResponse = getConnector().placeOrder(workaroundRequest);

                    if (!orderResponse.isResult()) {
                        Log.info("REST Order Rejected: " + orderResponse.getErrorCode() + " "
                                + orderResponse.getErrorMessage());
                        orderInfo.setStatus(OrderStatus.REJECTED);
                        tradingListeners.forEach(l -> l.onOrderUpdated(orderInfo.build()));
                        orderInfo.markAllUnchanged();

                        adminListeners
                                .forEach(l -> l.onSystemTextMessage(
                                        "Failed to place order with error code: " + orderResponse.getErrorCode() + "\n"
                                                + orderResponse.getErrorMessage(),
                                        SystemTextMessageType.ORDER_FAILURE));
                        return;
                    }
                }
            });
        }
    }

	private OrderTypeFuturesOrSwap determineOkexOrderType(OrderType orderType, boolean buy,
		int position, String clientId) {
		switch (orderType) {
			case LMT:
				return buy
					? OrderTypeFuturesOrSwap.OpenLongPosition
					: OrderTypeFuturesOrSwap.OpenShortPosition;
			case MKT:
				if (clientId != null && clientId.equals(ONE_DIR_CLOSE_CLIENT_ID)) {
					return buy
						? OrderTypeFuturesOrSwap.CloseShortPosition
						: OrderTypeFuturesOrSwap.CloseLongPosition;
				} else if (position > 0) {
					return buy
						? OrderTypeFuturesOrSwap.OpenLongPosition
						: OrderTypeFuturesOrSwap.CloseLongPosition;
				} else if (position < 0) {
					return !buy
						? OrderTypeFuturesOrSwap.OpenShortPosition
						: OrderTypeFuturesOrSwap.CloseShortPosition;
				} else {
					return buy
						? OrderTypeFuturesOrSwap.OpenLongPosition
						: OrderTypeFuturesOrSwap.OpenShortPosition;
				}
			default:
				String message = "No supported order type: " + orderType;
				Log.info(message);
				throw new IllegalArgumentException(message);
		}
	}

	@Override
	public void updateOrder(OrderUpdateParameters orderUpdateParameters) {
		if (orderUpdateParameters.getClass() == OrderCancelParameters.class) {
			Log.debug("+++Enter synchronize");
			synchronized (bmIdWorkingOrders) {
				Log.info("Cancel order with provided ID: " + orderUpdateParameters.orderId);
				OrderCancelParameters orderCancelParameters = (OrderCancelParameters) orderUpdateParameters;
				
				String id = orderIdToClientOid.get(orderCancelParameters.orderId);
				
				String alias = bmIdWorkingOrders.get(id).getInstrumentAlias();
	            int at = alias.indexOf('@');
	            String symbol = alias.substring(at + 1);
	            String type = alias.substring(0, at);
		        String body = "{\"client_oid\":\"" + id + "\", \"instrument_id\":\"" + symbol + "\"}";
				String path = "";
				
				if (Market.valueOf(type) == Market.SPOT) {
				    path = "/api/spot/v3/cancel_orders/";
				} else if (Market.valueOf(type) == Market.FUTURES) {
                    path = "/api/futures/v3/cancel_order/" + symbol + "/";
                }
				connector.restClient.call(path + id, body, String.class, apiKey, secretKey, connector.passPhraze);
			}
		} else {
            Log.error("Unsupported order update parameter: " + orderUpdateParameters.getClass().getSimpleName());
            adminListeners.forEach(
                    l -> l.onSystemTextMessage("Unsupported order update action", SystemTextMessageType.ORDER_FAILURE));
        }
    }

    @SuppressWarnings("unlikely-arg-type")
    protected void onOrder(OrderData order) {

        synchronized (bmIdWorkingOrders) {
            boolean isBuy = false;

            if (order instanceof OrderDataSpot) {
                isBuy = ((OrderDataSpot) order).getSide().equals("buy");
                order.setInstrumentType(Market.SPOT.toString());
            } else if (order instanceof OrderDataFutures) {
                order.setInstrumentType(Market.FUTURES.toString());
                OrderDataFutures futures = (OrderDataFutures) order;

                if (futures.getType().equals("1") || futures.getType().equals("4")) {
                    isBuy = true;
                } else if (futures.getType().equals("2") || futures.getType().equals("3")) {
                    isBuy = false;
                } else {
                    throw new RuntimeException("futures order type = " + futures.getType());
                }
            } else {
                throw new RuntimeException("unknown order type");
            }

            String alias = order.getInstrumentType() + "@" + order.getInstrumentId();

            if (order.getClientOid().equals("") || !bmIdSentOrders.containsKey(order.getClientOid())) {
                OrderInfoBuilder newBuilder = new OrderInfoBuilder(alias, order.getOrderId(), isBuy,
                        order.getType().equals("limit") ? OrderType.LMT : OrderType.MKT, "", false);
                if (order instanceof OrderDataSpot && order.getType().equals("limit")
                        || order instanceof OrderDataFutures
                                && ((order.getType().equals("1") || order.getType().equals("4"))
                                        || (order.getType().equals("2") || order.getType().equals("3")))) {
                    newBuilder.setLimitPrice(order.getPrice());
                    newBuilder.setType(OrderType.LMT);
                }

                order.setClientOid(order.getOrderId());

                if (order instanceof OrderDataSpot) {
                    newBuilder.setUnfilled((int) (Math.round(order.getSize() * getSizeMultiplier(alias))));
                }
                if (order instanceof OrderDataFutures) {
                    newBuilder
                            .setUnfilled((int) Math.round(order.getSize() - ((OrderDataFutures) order).getFilledQty()));
                }

                newBuilder.setDuration(OrderDuration.GTC);

                OrderStatus status;
                if (order.getState().equals(0)) {
                    status = OrderStatus.PENDING_SUBMIT;
                    bmIdSentOrders.put(order.getOrderId(), newBuilder);
                } else {
                    status = OrderStatus.WORKING;
                    bmIdWorkingOrders.put(order.getOrderId(), newBuilder);
                }

                newBuilder.setStatus(status);
                tradingListeners.forEach(l -> l.onOrderUpdated(newBuilder.build()));
            }

            OrderInfoBuilder orderInfo;
            OrderInfoUpdate update;

            StatusInfoLocal info = aliasedStatusInfos.computeIfAbsent(alias, v -> {
                StatusInfoLocal infoLocal =  new StatusInfoLocal();
                infoLocal.setInstrumentAlias(alias);
                return infoLocal;
                });
                        
            switch (order.getState()) {
            case -2:// previous API "rejected" current API "failed"
                orderInfo = bmIdSentOrders.get(order.getClientOid());
                bmIdSentOrders.remove(orderInfo.getClientId());

                orderInfo.setStatus(OrderStatus.REJECTED);
                update = orderInfo.build();
                tradingListeners.forEach(l -> l.onOrderUpdated(update));
                orderInfo.markAllUnchanged();
                break;
            case 0: // "open"
                orderInfo = bmIdSentOrders.get(order.getClientOid());
                bmIdSentOrders.remove(orderInfo.getClientId());

                Log.info("ORDER OPEN clOid" + order.getClientOid() + " clId " + orderInfo.getClientId());
                orderInfo.setStatus(OrderStatus.WORKING);
                update = orderInfo.build();
                tradingListeners.forEach(l -> l.onOrderUpdated(update));
                orderInfo.markAllUnchanged();

                clientOidToOrderId.put(order.getClientOid(), orderInfo.getOrderId());
                orderIdToClientOid.put(orderInfo.getOrderId(), order.getClientOid());
                bmIdWorkingOrders.put(order.getClientOid(), orderInfo);
                break;
            case -1:// "cancelled"
                orderInfo = bmIdWorkingOrders.get(order.getClientOid());
                Log.info("Order Cancelled");
                orderInfo.setStatus(OrderStatus.CANCELLED);
                bmOkexIds.remove(orderInfo.getOrderId());
                okexBmIds.remove(order.getOrderId());
                bmIdWorkingOrders.remove(orderInfo.getOrderId());
                update = orderInfo.build();
                tradingListeners.forEach(l -> l.onOrderUpdated(update));
                orderInfo.markAllUnchanged();
                break;
            case 2:// "fully filled"
                orderInfo = bmIdWorkingOrders.get(order.getClientOid());
                Log.info("Order filled");
                orderInfo.setUnfilled(0);
                if (order.getInstrumentType().equals(Market.SPOT.toString())) {
                    orderInfo.setFilled((int) Math.round(order.getSize() * getSizeMultiplier(alias)));
                } else {
                    orderInfo.setFilled((int) order.getSize());// !!!!!!!!!!! size is fp, turn to integer
                }
                orderInfo.setStatus(OrderStatus.FILLED);
                orderInfo.setAverageFillPrice(order.getLastFillPx());
                update = orderInfo.build();
                tradingListeners.forEach(l -> l.onOrderUpdated(update));
                orderInfo.markAllUnchanged();
                bmOkexIds.remove(orderInfo.getOrderId());
                okexBmIds.remove(order.getOrderId());
                bmIdWorkingOrders.remove(orderInfo.getOrderId());
                String execId = UUID.randomUUID().toString();
                ExecutionInfoBuilder executionInfoBuilder = new ExecutionInfoBuilder(orderInfo.getOrderId(),
                        orderInfo.getFilled(), orderInfo.getAverageFillPrice(), execId, System.currentTimeMillis(),
                        false);
                tradingListeners.forEach(l -> l.onOrderExecuted(executionInfoBuilder.build()));
                break;
            case 4:// cancel in process
                Log.info("Order Cancel In Process");
                orderInfo = bmIdWorkingOrders.get(order.getClientOid());
                orderInfo.setStatus(OrderStatus.PENDING_CANCEL);
                tradingListeners.forEach(l -> l.onOrderUpdated(orderInfo.build()));
                orderInfo.markAllUnchanged();
                break;
//				case PartiallyFilled: not implemented
//				case Unfilled: not implemented
            }
            
            int workingBuys = bmIdWorkingOrders.values()
                    .parallelStream()
                    .filter(e -> e.getInstrumentAlias().equals(alias))
                    .filter(e -> e.getStatus() == OrderStatus.WORKING)
                    .filter(e -> e.isBuy())
                    .mapToInt(e -> e.getUnfilled())
                    .sum();
            int workingSells = bmIdWorkingOrders.values()
                    .parallelStream()
                    .filter(e -> e.getInstrumentAlias().equals(alias))
                    .filter(e -> e.getStatus() == OrderStatus.WORKING)
                    .filter(e -> !e.isBuy())
                    .mapToInt(e -> e.getUnfilled())
                    .sum();
            info.setWorkingBuys(workingBuys);
            info.setWorkingSells(workingSells);
            updateStatus(info);
        }
    }
   
    protected void onFuturesAccount(SubscribeFuturesAccountResponse response) {
	    
        Map<String, FuturesAccount> map = response.data.get(0);

        for (String token : map.keySet()) {
            FuturesAccount account = map.get(token);

            if (account.getContracts() != null) {
                List<FutureAccountsContractFixedMargin> contracts = account.getContracts();

                for (FutureAccountsContractFixedMargin contract : contracts) {
                    account.setRealizedPnl(account.getRealizedPnl() + contract.getRealizedPnl());
                    account.setUnrealizedPnl(account.getUnrealizedPnl() + contract.getUnrealizedPnl());
                }
            }

            BalanceInCurrency balance = new BalanceInfo.BalanceInCurrency(account.getEquity(), // balance
                    account.getRealizedPnl(), // realized PnL,
                    account.getUnrealizedPnl(), // unrealized PnL,
                    Double.NaN, // previousDayBalance,
                    Double.NaN, // netLiquidityValue
                    token, null);// rateToBase)
            tradingListeners.forEach(l -> l.onBalance(new BalanceInfo(Collections.singletonList(balance))));
        }
    }
	
	private void onFuturesAccountRestResponse(FuturesAccount account) {
        if (account.getContracts() != null) {
            List<FutureAccountsContractFixedMargin> contracts = account.getContracts();

            for (FutureAccountsContractFixedMargin contract : contracts) {
                account.setRealizedPnl(account.getRealizedPnl() + contract.getRealizedPnl());
                account.setUnrealizedPnl(account.getUnrealizedPnl() + contract.getUnrealizedPnl());
            }
        }

        BalanceInCurrency balance = new BalanceInfo.BalanceInCurrency(account.getEquity(), // balance
                account.getRealizedPnl(), // realized PnL,
                account.getUnrealizedPnl(), // unrealized PnL,
                Double.NaN, // previousDayBalance,
                Double.NaN, // netLiquidityValue
                account.getCurrency(), null);// rateToBase)
        tradingListeners.forEach(l -> l.onBalance(new BalanceInfo(Collections.singletonList(balance))));
    }
	
    protected void onSpotAccount(List<SpotAccount> accounts) {
        for (SpotAccount account : accounts) {
            String currency = account.getCurrency();

            if (currenciesForSpotBalance.containsKey(currency)) {
                BalanceInCurrency balance = new BalanceInfo.BalanceInCurrency(account.getAvailable(), // balance
                        Double.NaN, // realized PnL,
                        Double.NaN, // unrealized PnL,
                        Double.NaN, // previousDayBalance,
                        Double.NaN, // netLiquidityValue
                        currency, null);// rateToBase)
                tradingListeners.forEach(l -> l.onBalance(new BalanceInfo(Collections.singletonList(balance))));
            }

            if (currenciesForSpotPosition.containsKey(currency)) {
                Set<String> aliases = currenciesForSpotPosition.get(currency);

                for (String alias : aliases) {
                    double minSize = ((InstrumentSpot) genericInstruments.get(alias)).getMinSize();

                    aliasedStatusInfos.computeIfAbsent(alias, v -> {
                        StatusInfoLocal infoLocal = new StatusInfoLocal();
                        infoLocal.setInstrumentAlias(alias);
                        infoLocal.setCurrency(getPositionCurrency(genericInstruments.get(alias)));
                        return infoLocal;
                        });
                    StatusInfoLocal info = aliasedStatusInfos.computeIfPresent(alias, (k, v) -> {
                        v.setPosition((int) Math.round(account.getAvailable() / minSize));
                        v.setCurrency(getPositionCurrency(genericInstruments.get(alias)));
                        return v;
                        });
                    updateStatus(info);
                }
            }
        }
    }


    @Override
    public void subscribe(SubscribeInfo subscribeInfo) {
        try {
            if (isSubscribed(subscribeInfo, false)) {
                getSubscribed(subscribeInfo);
            }
        } catch (NullPointerException e) {
            if (subscribeInfo == null) {
                Log.info("subscribeInfo is null");
            } else {
                String type = subscribeInfo.type;
                String symbol = subscribeInfo.symbol;
                String alias = type + "@" + symbol;
                Log.info("Cannot be subscribed to " + alias);
                throw new RuntimeException();
            }
        }
    }

    public void getSubscribed(SubscribeInfo subscribeInfo) {
            String type = subscribeInfo.type.toUpperCase();
            Market market = Market.valueOf(type);
            String symbol = subscribeInfo.symbol;
            String alias = market.toString() + "@" + symbol;

            singleThreadExecutor.execute(() -> {
                synchronized (aliasInstruments) {
                    getConnector().subscribeOrder(symbol, market);

                    if (market == Market.FUTURES) {
                        refreshFuturesPosition(alias);

                        getConnector().subscribePositionFutures(symbol, market);
                        String underlyingIndex = ((InstrumentFutures) genericInstruments.get(alias))
                                .getUnderlyingIndex();
                        refreshFuturesAccount(underlyingIndex);

                        getConnector().TEMPsubscribeAccount(symbol, market, underlyingIndex);

                    } else if (market == Market.SPOT) {
                        refreshBalance(alias);

                        String baseCurrency = ((InstrumentSpot) genericInstruments.get(alias)).getBaseCurrency();
                        String quoteCurrency = ((InstrumentSpot) genericInstruments.get(alias)).getQuoteCurrency();

                        currenciesForSpotPosition.computeIfAbsent(baseCurrency, v -> new HashSet<String>());
                        currenciesForSpotPosition.computeIfPresent(baseCurrency, (k, v) -> {
                            v.add(alias);
                            return v;
                        });

                        currenciesForSpotBalance.computeIfAbsent(quoteCurrency, v -> new HashSet<String>());
                        currenciesForSpotBalance.computeIfPresent(quoteCurrency, (k, v) -> {
                            v.add(alias);
                            return v;
                        });

                        getConnector().TEMPsubscribeAccount(symbol, market, baseCurrency);
                        getConnector().TEMPsubscribeAccount(symbol, market, quoteCurrency);
                    }

                    refreshOrders(alias);

                }
            });
        
    }
    
    @Override
    public void unsubscribe(String alias) {
        super.unsubscribe(alias);

        int at = alias.indexOf('@');
        String symbol = alias.substring(at + 1);
        Market market = Market.valueOf(alias.substring(0, at));

        getConnector().TEMPunsubscribeOrder(symbol, market);

        if (genericInstruments.get(alias) instanceof InstrumentSpot) {

            String quoteCurrency = ((InstrumentSpot) genericInstruments.get(alias)).getQuoteCurrency();

            currenciesForSpotPosition.computeIfPresent(quoteCurrency, (k, v) -> {
                v.remove(alias);
                return v;
            });

            String baseCurrency = ((InstrumentSpot) genericInstruments.get(alias)).getBaseCurrency();

            currenciesForSpotBalance.computeIfPresent(baseCurrency, (k, v) -> {
                v.remove(alias);
                return v;
            });
        }
    }

    private void refreshBalance(String alias) {
        String type = Utils.getTypeFromALias(alias);

        if (type.equals(Market.SPOT.toString())) {
            AccountSpot[] accounts;
            try (RestClient restClient = new RestClient(apiKey, secretKey, exchange)) {
                accounts = restClient.customCall("/api/spot/v3/accounts", "GET", "", AccountSpot[].class, apiKey,
                        secretKey, getConnector().passPhraze, null);
            }

            Map<String, AccountSpot> accountsMap = Arrays.stream(accounts)
                    .collect(Collectors.toMap(AccountSpot::getCurrency, account -> account));

            String quoteCurrency = ((InstrumentSpot) genericInstruments.get(alias)).getQuoteCurrency();
            double minSize = ((InstrumentSpot) genericInstruments.get(alias)).getMinSize();
            AccountSpot quoteCurrencyAccount = accountsMap.get(quoteCurrency);
            double quoteCurrencyBalance = 0.0;

            if (quoteCurrencyAccount != null) {
                quoteCurrencyBalance = quoteCurrencyAccount.getAvailable();
            }

            BalanceInCurrency balance = new BalanceInfo.BalanceInCurrency(
                    quoteCurrencyBalance, // balance
                    Double.NaN, // realized PnL,
                    Double.NaN, // unrealized PnL,
                    Double.NaN, // previousDayBalance,
                    Double.NaN, // netLiquidityValue
                    quoteCurrency, null);// rateToBase)

            tradingListeners.forEach(l -> l.onBalance(new BalanceInfo(Collections.singletonList(balance))));

            InstrumentGeneric generic = genericInstruments.get(alias);
            String baseCurrency = generic.getBaseCurrency();
            AccountSpot baseCurrencyAccount = accountsMap.get(baseCurrency);

            double baseCurrencyBalance = 0.0;

            if (baseCurrencyAccount != null) {
                baseCurrencyBalance = baseCurrencyAccount.getAvailable();
            }
            
            final double baseCurrencyBalanceFinal = baseCurrencyBalance;
            aliasedStatusInfos.computeIfAbsent(alias, v -> {
                StatusInfoLocal infoLocal = new StatusInfoLocal();
                infoLocal.setInstrumentAlias(alias);
                infoLocal.setCurrency(getPositionCurrency(genericInstruments.get(alias)));
                return infoLocal;
                });
            StatusInfoLocal info = aliasedStatusInfos.computeIfPresent(alias, (k, v) -> {
                v.setPosition((int) Math.round(baseCurrencyBalanceFinal / minSize));
                v.setCurrency(getPositionCurrency(generic));
                return v;
                });

            updateStatus(info);
            
        } else if (type.equals(Market.FUTURES.toString())) {

            String accountsResponse;
            InstrumentGeneric generic = genericInstruments.get(alias);
            InstrumentFutures instrumentFutures = (InstrumentFutures) generic;
            String underlyingInstrument = instrumentFutures.getUnderlyingIndex();
            try (RestClient restClient = new RestClient(apiKey, secretKey, exchange)) {
                accountsResponse = restClient.customCall("/api/futures/v3/accounts/" + underlyingInstrument, "GET", "",
                        String.class, apiKey, secretKey, getConnector().passPhraze, null);
            }

            if (accountsResponse.contains("\"margin_mode\":\"crossed\"")) {
                try {
                    AccountsFuturesCrossMargin accountsCrossMargin = objectMapper.readValue(accountsResponse,
                            AccountsFuturesCrossMargin.class);
                    BalanceInCurrency balance = new BalanceInfo.BalanceInCurrency(
                            accountsCrossMargin.getTotalAvailBalance(), // balance
                            accountsCrossMargin.getRealizedPnl(), // realized PnL,
                            accountsCrossMargin.getUnrealizedPnl(), // unrealized PnL,
                            Double.NaN, // previousDayBalance,
                            accountsCrossMargin.getEquity(), // netLiquidityValue
                            underlyingInstrument, 1.0);// rateToBase)
                    balanceMap.put(alias, balance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (accountsResponse.contains("\"margin_mode\":\"fixed\"")) {
                try {
                    AccountsFuturesFixedMarginResponse accountsFixedMarginResponse = objectMapper
                            .readValue(accountsResponse, AccountsFuturesFixedMarginResponse.class);

                    BalanceInCurrency balance = new BalanceInfo.BalanceInCurrency(
                            accountsFixedMarginResponse.getTotalAvailBalance(), // balance
                            accountsFixedMarginResponse.getRealizedPnl(), // realized PnL,
                            accountsFixedMarginResponse.getUnrealizedPnl(), // unrealized PnL,
                            Double.NaN, // previousDayBalance,
                            accountsFixedMarginResponse.getEquity(), // netLiquidityValue
                            underlyingInstrument, 1.0);// rateToBase)
                    balanceMap.put(alias, balance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        BalanceInfo info = new BalanceInfo(new ArrayList<BalanceInfo.BalanceInCurrency>(balanceMap.values()));
        tradingListeners.forEach(l -> l.onBalance(info));
    }

    private void refreshOrders(String alias) {
        String type = Utils.getTypeFromALias(alias);
        String instrumentId = Utils.getInstrumentIdFromALias(alias);

        if (this instanceof RealTimeTradingProvider) {
            if (type.equals(Market.SPOT.toString())) {
                List<Map.Entry<String, String>> params = new ArrayList<>();
                params.add(new AbstractMap.SimpleEntry<String, String>("instrument_id", instrumentId));
                params.add(new AbstractMap.SimpleEntry<String, String>("status", "open"));

                OrderDataSpot[] ordersSpot = null;
                try (RestClient restClient = new RestClient(apiKey, secretKey, exchange)) {
                    String testordersSpot = restClient.customCall("/api/spot/v3/orders", "GET", "", String.class,
                            apiKey, secretKey, getConnector().passPhraze, params);

                    ordersSpot = objectMapper.readValue(testordersSpot, OrderDataSpot[].class);

                    if (ordersSpot == null)
                        return;

                    for (OrderDataSpot spot : ordersSpot) {
                        ((RealTimeTradingProvider) this).onOrder(spot);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (type.equals(Market.FUTURES.toString())) {
                OrdersFuturesList ordersFutures;
                String path = "/api/futures/v3/orders/" + instrumentId;

                List<Map.Entry<String, String>> params = new ArrayList<>();
                params.add(new AbstractMap.SimpleEntry<String, String>("status", "0"));

                try (RestClient restClient = new RestClient(apiKey, secretKey, exchange)) {
                    ordersFutures = restClient.customCall(path, "GET", "", OrdersFuturesList.class, apiKey, secretKey,
                            getConnector().passPhraze, params);

                    if (ordersFutures == null)
                        return;

                    OrderDataFutures[] futures = ordersFutures.getOrderInfo();

                    if (futures == null)
                        return;

                    for (OrderDataFutures future : futures) {
                        ((RealTimeTradingProvider) this).onOrder(future);
                    }
                }
            }
        }
    }

	    private void refreshFuturesPosition(String alias) {
	        String type = Utils.getTypeFromALias(alias);
	        
	        if (!type.equals(Market.FUTURES.toString())) return;
	        
	        String instrumentId = Utils.getInstrumentIdFromALias(alias);
	        
	        if (this instanceof RealTimeTradingProvider) {
	            FuturesPositionsOfCurrencyResponse positionsResponse;
	                String path = "/api/futures/v3/" + instrumentId + "/position";
	                
	                try (RestClient restClient = new RestClient(apiKey, secretKey, exchange)) {
	                    positionsResponse = restClient.customCall(path, "GET", "",
	                            FuturesPositionsOfCurrencyResponse.class, apiKey, secretKey, getConnector().passPhraze, null);
	                    
	                    if (positionsResponse != null) {
	                        onFuturesPosition(positionsResponse.getHolding());
	                    }
	                }
	        }
	    }
	    
    private void refreshFuturesAccount(String underlyingIndex) {

        if (this instanceof RealTimeTradingProvider) {
            FuturesAccount response;
            String path = "/api/futures/v3/accounts/" + underlyingIndex;

            try (RestClient restClient = new RestClient(apiKey, secretKey, exchange)) {
                response = restClient.customCall(path, "GET", "", FuturesAccount.class, apiKey, secretKey,
                        getConnector().passPhraze, null);

                if (response != null) {
                    onFuturesAccountRestResponse(response);
                }
            }
        }

    }

    public void onFuturesPosition(SubscribeFuturesPositionResponse response) {
        List<FuturesPosition> data = response.getData();
        
        if (data != null) {
            onFuturesPosition(response.getData());
        }
    }

    public void onFuturesPosition(List<FuturesPosition> data) {
	    if (data == null) return;

        for (FuturesPosition position : data) {
            int qty = (int) Math.round(-position.getShortAvailQty() + position.getLongAvailQty());
            double avrPrice = Math.abs((-position.getShortAvgCost() * position.getShortAvailQty()
                    + position.getLongAvgCost() * position.getLongAvailQty()) / (double) qty);
            String alias = Market.FUTURES.toString() + "@" + position.getInstrumentId();

            double unrealizedPnl = getUpdatedUnrealizedPnl(alias, qty, avrPrice);

            aliasedStatusInfos.computeIfAbsent(alias, v -> {
                StatusInfoLocal infoLocal = new StatusInfoLocal();
                infoLocal.setInstrumentAlias(alias);
                infoLocal.setCurrency(getPositionCurrency(genericInstruments.get(alias)));
                return infoLocal;
            });
            StatusInfoLocal info = aliasedStatusInfos.computeIfPresent(alias, (k, v) -> {
                v.setPosition(qty);
                v.setUnrealizedPnl(unrealizedPnl);
                v.setRealizedPnl(position.getRealizedPnl());
                v.setCurrency(getPositionCurrency(genericInstruments.get(alias)));
                return v;
            });
            updateStatus(info);
            positionsMap.put(alias, Pair.of((int) position.getShortAvailQty(), (int) position.getLongAvailQty()));
        }
    }
        
    public double getUpdatedUnrealizedPnl(String alias, int qty, double avrCost) {
        if (qty == 0) {
            return 0.0;
        }
        
        UnrealizedPnlData data = unrealizedPnlMap.computeIfAbsent(alias, v -> new UnrealizedPnlData());
        data.setQty(qty);
        data.setAvrCost(avrCost);
        
        double price = qty > 0 ? data.getLastAskTrade() : data.getLastBidTrade();
        
        if (price == 0.0) {
            return 0.0;
        }
        
        double contractVal = ((InstrumentFutures) genericInstruments.get(alias)).getContractVal();
        double naturalValue = contractVal/avrCost;


        return - naturalValue + Math.abs(qty * contractVal/price);
    }
    
    public double getUpdatedUnrealizedPnl(String alias, boolean isBuy, double lastTrade) {
        UnrealizedPnlData data = unrealizedPnlMap.computeIfAbsent(alias, v -> new UnrealizedPnlData());

        if (isBuy) {
            data.setLastAskTrade(lastTrade);
        } else {
            data.setLastBidTrade(lastTrade);
        }

        int qty = data.getQty();
        if (qty == 0) {
            return 0.0;
        }

        double price = qty > 0 ? data.getLastAskTrade() : data.getLastBidTrade();

        if (price == 0.0) {
            return 0.0;
        }

        double avrCost = data.getAvrCost();

        double contractVal = ((InstrumentFutures) genericInstruments.get(alias)).getContractVal();
        double naturalValue = contractVal/avrCost;

        return - naturalValue + Math.abs(qty * contractVal/price);
        
    }
        
    private void closeFuturesPosition(PlaceOrderRequest workaroundRequest, String workaroundId,
            OrderInfoBuilder orderInfo, String type, int size) {
        workaroundRequest.setType(type);
        ((PlaceOrderRequestFuturesOrSwap) workaroundRequest).setSize(size);
        ((PlaceOrderRequestFuturesOrSwap) workaroundRequest).setMatchPrice("1");

        singleThreadExecutor.submit(() -> {
            Log.debug("+++Enter synchronize");

            bmIdSentOrders.put(workaroundId, orderInfo);

            orderInfo.setStatus(OrderStatus.PENDING_SUBMIT);
            tradingListeners.forEach(l -> l.onOrderUpdated(orderInfo.build()));
            orderInfo.markAllUnchanged();

            PlaceOrderResponse orderResponse = getConnector().placeOrder(workaroundRequest);

            if (!orderResponse.isResult()) {
                Log.info(
                        "REST Order Rejected: " + orderResponse.getErrorCode() + " " + orderResponse.getErrorMessage());
                orderInfo.setStatus(OrderStatus.REJECTED);
                tradingListeners.forEach(l -> l.onOrderUpdated(orderInfo.build()));
                orderInfo.markAllUnchanged();

                adminListeners
                        .forEach(
                                l -> l.onSystemTextMessage(
                                        "Failed to place order with error code: " + orderResponse.getErrorCode() + "\n"
                                                + orderResponse.getErrorMessage(),
                                        SystemTextMessageType.ORDER_FAILURE));
                return;
            }
        });
    }

    private String getPositionCurrency(InstrumentGeneric instrument) {
        if (instrument instanceof InstrumentFutures) {
            InstrumentFutures futures = (InstrumentFutures) instrument;
            if (!futures.getBaseCurrency().equals(futures.getContractValCurrency())) {
                return futures.getContractVal() + " " + futures.getQuoteCurrency(); 
            }
        }
        return instrument.getBaseCurrency();
    }
}
