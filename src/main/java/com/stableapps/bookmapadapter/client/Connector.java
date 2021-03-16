/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableapps.bookmapadapter.decoder.ErrorWsDecoder;
import com.stableapps.bookmapadapter.decoder.LoginResponseDecoder;
import com.stableapps.bookmapadapter.decoder.PongDecoder;
import com.stableapps.bookmapadapter.decoder.StringMessageDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeContractMarketDepthIncrementalInitialResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeContractMarketDepthIncrementalResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeContractTradeRecordInitialResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeContractTradeRecordResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeFuturesAccountInitialResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeFuturesAccountResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeOrderFuturesDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeOrderInitialResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeOrderSpotDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribePositionFuturesDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribePositionFuturesInitialResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeSpotAccountInitialResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeSpotAccountResponseDecoder;
import com.stableapps.bookmapadapter.decoder.SubscribeSpotMarginAccountInitialResponseDecoder;
import com.stableapps.bookmapadapter.decoder.UnsubscribeResponseDecoder;
import com.stableapps.bookmapadapter.model.ErrorWs;
import com.stableapps.bookmapadapter.model.Event;
import com.stableapps.bookmapadapter.model.Expiration;
import com.stableapps.bookmapadapter.model.ForecastPrice;
import com.stableapps.bookmapadapter.model.LoginResponse;
import com.stableapps.bookmapadapter.model.MarketDepths;
import com.stableapps.bookmapadapter.model.MarketPrice;
import com.stableapps.bookmapadapter.model.Message;
import com.stableapps.bookmapadapter.model.OrderData;
import com.stableapps.bookmapadapter.model.OrderDataFutures;
import com.stableapps.bookmapadapter.model.OrderDataSpot;
import com.stableapps.bookmapadapter.model.Pong;
import com.stableapps.bookmapadapter.model.PositionsFixedMargin;
import com.stableapps.bookmapadapter.model.SpotAccount;
import com.stableapps.bookmapadapter.model.StringMessage;
import com.stableapps.bookmapadapter.model.SubscribeContractMarketDepthInitialResponse;
import com.stableapps.bookmapadapter.model.SubscribeContractMarketDepthResponse;
import com.stableapps.bookmapadapter.model.SubscribeContractTradeRecordInitialResponse;
import com.stableapps.bookmapadapter.model.SubscribeContractTradeRecordResponse;
import com.stableapps.bookmapadapter.model.SubscribeFuturesAccountInitialResponse;
import com.stableapps.bookmapadapter.model.SubscribeFuturesAccountResponse;
import com.stableapps.bookmapadapter.model.SubscribeFuturesPositionInitialResponse;
import com.stableapps.bookmapadapter.model.SubscribeFuturesPositionResponse;
import com.stableapps.bookmapadapter.model.SubscribeOrderFutures;
import com.stableapps.bookmapadapter.model.SubscribeOrderInitialResponse;
import com.stableapps.bookmapadapter.model.SubscribeOrderSpot;
import com.stableapps.bookmapadapter.model.SubscribeSpotAccountInitialResponse;
import com.stableapps.bookmapadapter.model.SubscribeSpotAccountResponse;
import com.stableapps.bookmapadapter.model.SubscribeSpotMarginAccountInitialResponse;
import com.stableapps.bookmapadapter.model.Trade;
import com.stableapps.bookmapadapter.model.UnsubscribeResponse;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderRequest;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderResponse;
import com.stableapps.bookmapadapter.rest.RestClient;

import velox.api.layer1.Layer1ApiAdminListener;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.DisconnectionReason;

public class Connector extends Endpoint implements AutoCloseable {

	private static final int HEART_BEAT_INTERVAL = 5_000;//5 seconds
	private static final long SIGNATURE_LIFE = 25;

	public static String getServerResponse(String address) {
	        String response = null;

	        try {
	            URL url = new URL(address);
	            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
	            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
	            conn.setRequestMethod("GET");

	            if (conn.getResponseCode() == 200) {
	                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
	                StringBuilder sb = new StringBuilder();
	                String output = null;

	                while ((output = br.readLine()) != null) {
	                    sb.append(output);
	                }
	                response = sb.toString();
	            } else {
	                Log.info(conn.getResponseCode() + " " + conn.getResponseMessage());
	            }
	        } catch (UnknownHostException | NoRouteToHostException e) {
	            Log.info("Connector getServerResponse: no response from server");
	        } catch (SocketException e) {
	            Log.info("Connector getServerResponse: network is unreachable");
	        } catch (IOException e) {
	            Log.info("Connector getServerResponse: buffer reading exception");
	            e.printStackTrace();
	        }
	        return response;
	    }
	   
    public static String generateSignature(String apiSecret, String messageBody) {
        try {
            byte[] decoded = apiSecret.getBytes();
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(decoded, "HmacSHA256");
            sha256_HMAC.init(secretKey);
            byte[] hash = sha256_HMAC.doFinal(messageBody.getBytes());
            String encoded = Base64.getEncoder().encodeToString(hash);
            return encoded;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    public static String longToTransactTime(long moment) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(moment*1000l),
                ZoneOffset.UTC);
        String time = zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        return time;
    }
    
    public static String getTimestamp() {
        return Long.toString(Instant.now().plusSeconds(SIGNATURE_LIFE).getEpochSecond());
    }
    
    public static long getTimestampLong() {
        return Instant.now().plusSeconds(SIGNATURE_LIFE).getEpochSecond();
    }
    
    public static String createMessageBody(String timestamp, String method, String requestPath, String body) {
        /*
         * The body is the request body string or omitted if there is no request body
         * (typically for GET requests). For
         * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
         */
        if (body == null) {
            body = "";
        }
        String messageBody = timestamp + method + requestPath + body;
        return messageBody;
    }
    
    public static String createMessageBody(String timestamp, String method, String requestPath,
            List <Map.Entry<String, String>> queryParams, String body) {
        /*
         * The body is the request body string or omitted if there is no request body
         * (typically for GET requests). For
         * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
         */
        if (body == null) {
            body = "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(timestamp)
        .append(method)
        .append(requestPath);
        
        if (queryParams != null) {
            sb.append("?");
            queryParams.forEach(entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&"));
            sb.deleteCharAt(sb.length() - 1);
        }
        
        sb.append(body);

        String messageBody = sb.toString();
        return messageBody;
    }
    
    /*
     * generates query map
     * {"op":"<operation>","args":["<api_key>","<passphrase>","<timestamp>","<sign>"]}
     */
    public static LinkedHashMap<String, Object> generateSignedQueryMap(String apiKey, String passPhraze, String secretKey, String op, String method, String requestPath, String body) {
     // create request
        List<String> parameters = new LinkedList<>();
        parameters.add(apiKey);
        parameters.add(passPhraze);
        
//        String timestamp = ((Long)Instant.now().plusSeconds(25).getEpochSecond()).toString();
        String timestamp = getTimestamp();
        parameters.add(timestamp);
        
        /*
         * The body is the request body string or omitted if there is no request body
         * (typically for GET requests). For
         * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
         */
        String messageBody = createMessageBody(timestamp, method, requestPath, body);
        String newSign = generateSignature(secretKey, messageBody);
        parameters.add(newSign);
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("op", op);
        params.put("args", parameters);
        return params;
    }

	public Session session;
	private Timer timer;
	private final ReentrantReadWriteLock.WriteLock wLock;
	private final Condition newPong;
	private Pong pong;
    private final ObjectMapper objectMapper;
    private final Condition newSubscribeContractMarketDepthInitialResponse;
    private final Condition newSubscribeContractTradeRecordInitialResponse;
    private final Condition newSubscribeOrderInitialResponse;
    private final Condition newSubscribeOrderSpot;
    private final Condition newSubscribeOrderFutures;
    private final Condition newSubscribeFuturesAccountInitialResponse;
    private final Condition newSubscribeFuturesAccountResponse;
    private final Condition newSubscribeSpotAccountInitialResponse;
    private final Condition newSubscribeSpotAccountResponse;
    private final Condition newSubscribeSpotMarginAccountInitialResponse;
    private final Condition newSubscribeFuturesPositionResponse;
    private final Condition newSubscribeFuturesPositionInitialResponse;
    private final Condition newLoginResponse;
    private final Condition newUnsubscribeResponse;
    
    private SubscribeContractMarketDepthInitialResponse subscribeContractMarketDepthInitialResponse;
    private SubscribeOrderInitialResponse subscribeOrderInitialResponse;
    private SubscribeOrderSpot subscribeOrderSpot;
    private SubscribeOrderFutures subscribeOrderFutures;
    private SubscribeContractTradeRecordInitialResponse subscribeContractTradeRecordInitialResponse;
    private SubscribeFuturesAccountInitialResponse subscribeFuturesAccountInitialResponse;
    private SubscribeFuturesAccountResponse subscribeFuturesAccountResponse;
    private SubscribeSpotAccountInitialResponse subscribeSpotAccountInitialResponse;
    private SubscribeSpotAccountResponse subscribeSpotAccountResponse;
    private SubscribeSpotMarginAccountInitialResponse subscribeSpotMarginAccountInitialResponse;
    private SubscribeFuturesPositionResponse subscribeFuturesPositionResponse;
    private SubscribeFuturesPositionInitialResponse subscribeFuturesPositionInitialResponse;
    private LoginResponse loginResponse;
    private UnsubscribeResponse unsubscribeResponse;
    public final AbstractClient client;
    public boolean isReconnecting;
    public final String apiKey;
    public final String secretKey;
    public final String passPhraze;
	
	public final RestClient restClient;
	private final ExecutorService singleThreadExecutor;
	private CopyOnWriteArrayList<Layer1ApiAdminListener> adminListeners;
	private String wsLink;
	private String exchange;
	
	public void setAdminListeners(CopyOnWriteArrayList<Layer1ApiAdminListener> adminListeners) {
        this.adminListeners = adminListeners;
    }

    public Connector(String apiKey, String secretKey, String passPhraze, AbstractClient client, String wsLink, String exchange) {
		this.apiKey = apiKey;
		this.secretKey = secretKey;
		this.passPhraze = passPhraze;
		this.exchange = exchange;
		ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
		wLock = rwLock.writeLock();
		newPong = wLock.newCondition();
		newSubscribeContractMarketDepthInitialResponse = wLock.newCondition();
		newSubscribeContractTradeRecordInitialResponse = wLock.newCondition();
		newSubscribeOrderInitialResponse = wLock.newCondition();
		newSubscribeOrderSpot = wLock.newCondition();
		newSubscribeOrderFutures = wLock.newCondition();
		newSubscribeFuturesAccountInitialResponse = wLock.newCondition();
		newSubscribeFuturesAccountResponse = wLock.newCondition();
		newSubscribeSpotAccountInitialResponse = wLock.newCondition();
		newSubscribeSpotAccountResponse = wLock.newCondition();
        newSubscribeSpotMarginAccountInitialResponse = wLock.newCondition();
        newSubscribeFuturesPositionResponse = wLock.newCondition();
        newSubscribeFuturesPositionInitialResponse = wLock.newCondition();
		newLoginResponse = wLock.newCondition();
		newUnsubscribeResponse = wLock.newCondition();
		objectMapper = new ObjectMapper();
		this.client = client;
		this.restClient = new RestClient(apiKey, secretKey, exchange);
		singleThreadExecutor = Executors.newSingleThreadExecutor();
		this.wsLink = wsLink;
	}

	/*
	** The next _ requests are implementations of Contract Price API.
	** This will received the latest OKEX Contract Data. 
	 */
	
	public boolean subscribeTrade(String symbol, String type) {
	    try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("op", "subscribe");
            List <String> args = new LinkedList<String>();
            args.add(type + "/trade:" + symbol);
            params.put("args", args);
            String json = objectMapper.writeValueAsString(params);
            Log.info(json);
            getSession().getBasicRemote().sendText(json);
            SubscribeContractTradeRecordInitialResponse response
            = waitFor(SubscribeContractTradeRecordInitialResponse.class);
            
            if (!response.getData().isResult()) {
                return false;
            }
            return true;
        } catch (JsonProcessingException ex) {
            Log.error("Error converting to JSON", ex);
            return false;
        } catch (IOException ex) {
            Log.error("Error sending request to " + exchange, ex);
            return false;
        }
	}
	
	public boolean subscribeOrder(String symbol, String type) {
	    try {
	        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
	        params.put("op", "subscribe");
	        List <String> args = new LinkedList<String>();
	        List<String> parameters = new LinkedList<>();
            parameters.add(apiKey);
            parameters.add(passPhraze);
            String timestamp = ((Long)Instant.now().plusSeconds(25).getEpochSecond()).toString();
            parameters.add(timestamp);
            String method = "GET";
            String requestPath = "/" + type + "/order:" + symbol;
            String body = "";
            
            /*
             * The body is the request body string or omitted if there is no request body
             * (typically for GET requests). For
             * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
             */
            String messageBody = createMessageBody(timestamp, method, requestPath, body);
            String newSign = generateSignature(secretKey, messageBody);
            parameters.add(newSign);
	        args.add(type + "/order:" + symbol);
	        params.put("args", args);
	        String json = objectMapper.writeValueAsString(params);
	        getSession().getBasicRemote().sendText(json);
	        SubscribeOrderInitialResponse response
	        = waitFor(SubscribeOrderInitialResponse.class);
	        
	        if (!response.event.equals("subscribe")) {
	            return false;
	        }
	        return true;
	    } catch (JsonProcessingException ex) {
	        Log.error("Error converting to JSON", ex);
	        return false;
	    } catch (IOException ex) {
	        Log.error("Error sending request to " + exchange, ex);
	        return false;
	    }
	}
	
	public void subscribeAccount(String symbol, String type) {
	    try {
	        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
	        params.put("op", "subscribe");
	        List <String> args = new LinkedList<String>();
	        List<String> parameters = new LinkedList<>();
	        parameters.add(apiKey);
	        parameters.add(passPhraze);
	        String timestamp = ((Long)Instant.now().plusSeconds(25).getEpochSecond()).toString();
	        parameters.add(timestamp);
	        String method = "GET";
	        String requestPath = "/" + type + "/account:" + "BTC";
	        String body = "";
	        
	        /*
	         * The body is the request body string or omitted if there is no request body
	         * (typically for GET requests). For
	         * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
	         */
	        String messageBody = createMessageBody(timestamp, method, requestPath, body);
	        String newSign = generateSignature(secretKey, messageBody);
	        parameters.add(newSign);
	        args.add(type + "/account:" + "BTC");
	        params.put("args", args);
	        String json = objectMapper.writeValueAsString(params);
	        Log.info(json);
	        getSession().getBasicRemote().sendText(json);
	    } catch (JsonProcessingException ex) {
	        Log.error("Error converting to JSON", ex);
	    } catch (IOException ex) {
	        Log.error("Error sending request to " + exchange, ex);
	    }
	}
	
	public void subscribePositionFutures(String symbol, String type) {
	    try {
	        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
	        params.put("op", "subscribe");
	        List <String> args = new LinkedList<String>();
	        List<String> parameters = new LinkedList<>();
	        parameters.add(apiKey);
	        parameters.add(passPhraze);
	        String timestamp = getTimestamp();
	        parameters.add(timestamp);
	        String method = "GET";
	        String requestPath = "/" + type + "/position:" + symbol;
	        String body = "";
	        
	        String messageBody = createMessageBody(timestamp, method, requestPath, body);
	        Log.info("messageBody\t" + messageBody);
	        String newSign = generateSignature(secretKey, messageBody);
	        parameters.add(newSign);
	        args.add(type + "/position:" + symbol);
	        params.put("args", args);
	        String json = objectMapper.writeValueAsString(params);
	        Log.info(json);
	        getSession().getBasicRemote().sendText(json);
	    } catch (JsonProcessingException ex) {
	        Log.error("Error converting to JSON", ex);
	    } catch (IOException ex) {
	        Log.error("Error sending request to " + exchange, ex);
	    }
	}
	
	public boolean TEMPsubscribeAccount(String symbol, String type, String baseCurrency) {
	    try {
	        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
	        params.put("op", "subscribe");
	        List <String> args = new LinkedList<String>();
	        List<String> parameters = new LinkedList<>();
	        parameters.add(apiKey);
	        parameters.add(passPhraze);
	        String timestamp = ((Long)Instant.now().plusSeconds(25).getEpochSecond()).toString();
	        parameters.add(timestamp);
	        String method = "GET";
	        String subLink = "/account:";
	        String requestPath = "/" + type + subLink + baseCurrency;
	        String body = "";
	        
	        /*
	         * The body is the request body string or omitted if there is no request body
	         * (typically for GET requests). For
	         * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
	         */
	        String messageBody = createMessageBody(timestamp, method, requestPath, body);
	        Log.info("messageBody\t" + messageBody);
	        String newSign = generateSignature(secretKey, messageBody);
	        parameters.add(newSign);
	        args.add(type + subLink + baseCurrency);
	        params.put("args", args);
	        String json = objectMapper.writeValueAsString(params);
	        getSession().getBasicRemote().sendText(json);
	        return true;
	    } catch (JsonProcessingException ex) {
	        Log.error("Error converting to JSON", ex);
	        return false;
	    } catch (IOException ex) {
	        Log.error("Error sending request to " + exchange, ex);
	        return false;
	    }
	}
	
	public boolean TEMPunsubscribeOrder(String symbol, String type) {
	    try {
	        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
	        params.put("op", "unsubscribe");
	        List <String> args = new LinkedList<String>();
	        List<String> parameters = new LinkedList<>();
	        parameters.add(apiKey);
	        parameters.add(passPhraze);
	        String timestamp = getTimestamp();
	        parameters.add(timestamp);
	        String method = "GET";
	        String requestPath = "/" + type + "/order:" + symbol;
	        String body = "";
	        /*
	         * The body is the request body string or omitted if there is no request body
	         * (typically for GET requests). For
	         * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
	         */
	        String messageBody = createMessageBody(timestamp, method, requestPath, body);
	        Log.info("messageBody\t" + messageBody);
	        String newSign = generateSignature(secretKey, messageBody);
	        parameters.add(newSign);
	        args.add(type + "/order:" + symbol);
	        params.put("args", args);
	        String json = objectMapper.writeValueAsString(params);
	        getSession().getBasicRemote().sendText(json);
	        UnsubscribeResponse response
	        = waitFor(UnsubscribeResponse.class);
	        
	        if (!response.getEvent().contains("unsubscribe")) {
	            return false;
	        }
	        return true;
	    } catch (JsonProcessingException ex) {
	        Log.error("Error converting to JSON", ex);
	        return false;
	    } catch (IOException ex) {
	        Log.error("Error sending request to " + exchange, ex);
	        return false;
	    }
	}

	public boolean subscribeContractMarketDepthIncremental(String symbol, String type) {
        try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("op", "subscribe");
            List <String> args = new LinkedList<String>();
            args.add(type + "/depth:" + symbol);
            params.put("args", args);
            String json = objectMapper.writeValueAsString(params);
            Log.info(json);
            getSession().getBasicRemote().sendText(json);
            SubscribeContractMarketDepthInitialResponse response
            = waitFor(SubscribeContractMarketDepthInitialResponse.class);
            
            if (!response.getData().isResult()) {
                return false;
            }
            return true;
        } catch (JsonProcessingException ex) {
            Log.error("Error converting to JSON", ex);
            return false;
        } catch (IOException ex) {
            Log.error("Error sending request to " + exchange, ex);
            return false;
        }
    }

	public boolean unsubscribeContractMarketDepthFull(String symbol, String type) {
		try {
		    LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("op", "unsubscribe");
            List <String> args = new LinkedList<String>();
            args.add(type + "/depth:" + symbol);
            params.put("args", args);
            String json = objectMapper.writeValueAsString(params);
			getSession().getBasicRemote().sendText(json);
			UnsubscribeResponse response
					= waitFor(UnsubscribeResponse.class);

		    if (!response.getEvent().contains("unsubscribe")) {
				return false;
			}
			return true;
		} catch (JsonProcessingException ex) {
			Log.error("Error converting to JSON", ex);
			return false;
		} catch (IOException ex) {
			Log.error("Error sending request to " + exchange, ex);
			return false;
		}
	}
	
	public boolean unsubscribeTrade(String symbol, String type) {
	    try {
	        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
	        params.put("op", "unsubscribe");
	        List <String> args = new LinkedList<String>();
	        args.add(type + "/trade:" + symbol);
	        params.put("args", args);
	        String json = objectMapper.writeValueAsString(params);
	        getSession().getBasicRemote().sendText(json);
	        UnsubscribeResponse response
	        = waitFor(UnsubscribeResponse.class);
	        
	        if (!response.getEvent().contains("unsubscribe")) {
	            return false;
	        }
	        return true;
	    } catch (JsonProcessingException ex) {
	        Log.error("Error converting to JSON", ex);
	        return false;
	    } catch (IOException ex) {
	        Log.error("Error sending request to " + exchange, ex);
	        return false;
	    }
	}
//	public boolean unsubscribeContractMarketDepthFull(String symbol, Expiration expiration, int amount) {
//	    try {
//	        HashMap<String, String> params = new HashMap<>();
//	        params.put("event", Event.removeChannel.name());
//	        final String channel = "ok_sub_futureusd_X_depth_Y_Z".replace("X", symbol)
//	                .replace("Y", expiration.name()).replace("Z", String.valueOf(amount));
//	        params.put("channel", channel);
//	        String json = objectMapper.writeValueAsString(params);
//	        getSession().getBasicRemote().sendText(json);
//	        UnsubscribeResponse response
//	        = waitFor(UnsubscribeResponse.class);
//	        
//	        if (!response.getData().isResult()) {
//	            return false;
//	        }
//	        return true;
//	    } catch (JsonProcessingException ex) {
//	        Log.error("Error converting to JSON", ex);
//	        return false;
//	    } catch (IOException ex) {
//	        Log.error("Error sending request to " + exchange, ex);
//	        return false;
//	    }
//	}

	// subscribe to existing trades
    public boolean subscribeContractTradeRecord(String symbol, Expiration expiration) {
		try {
			HashMap<String, String> params = new HashMap<>();
			params.put("event", Event.addChannel.name());
			final String channel = "ok_sub_futureusd_X_trade_Y".replace("X", symbol)
					.replace("Y", expiration.name());
			params.put("channel", channel);
			String json = objectMapper.writeValueAsString(params);
			getSession().getBasicRemote().sendText(json);
			SubscribeContractTradeRecordInitialResponse response
					= waitFor(SubscribeContractTradeRecordInitialResponse.class);

			if (!response.getData().isResult()) {
				return false;
			}
			return true;
		} catch (JsonProcessingException ex) {
			Log.error("Error converting to JSON", ex);
			return false;
		} catch (IOException ex) {
			Log.error("Error sending request to " + exchange, ex);
			return false;
		}
	}

	public boolean unsubscribeContractTradeRecord(String symbol, Expiration expiration) {
		try {
			HashMap<String, String> params = new HashMap<>();
			params.put("event", Event.removeChannel.name());
			final String channel = "ok_sub_futureusd_X_trade_Y".replace("X", symbol)
					.replace("Y", expiration.name());
			params.put("channel", channel);
			String json = objectMapper.writeValueAsString(params);
			getSession().getBasicRemote().sendText(json);
			UnsubscribeResponse response = waitFor(UnsubscribeResponse.class);

			if (!response.getData().isResult()) {
				return false;
			}
			return true;
		} catch (JsonProcessingException ex) {
			Log.error("Error converting to JSON", ex);
			return false;
		} catch (IOException ex) {
			Log.error("Error sending request to " + exchange, ex);
			return false;
		}
	}

	public boolean wslogin() {
        if (session == null) {
            connect();
        }
	    
		try {
			// create request
			List<String> parameters = new LinkedList<>();
			parameters.add(apiKey);
			parameters.add(passPhraze);
			String timestamp = getTimestamp();
			parameters.add(timestamp);
			String method = "GET";
			String requestPath = "/users/self/verify";
			String body = "";
	        /*
	         * The body is the request body string or omitted if there is no request body
	         * (typically for GET requests). For
	         * example:{"product_id":"BTC-USD-0309","order_id":"377454671037440"}
	         */
			String messageBody = createMessageBody(timestamp, method, requestPath, body);
			String newSign = generateSignature(secretKey, messageBody);
			parameters.add(newSign);
			LinkedHashMap<String, Object> params = new LinkedHashMap<>();
			params.put("op", Event.login.name());
			params.put("args", parameters);
			String json = objectMapper.writeValueAsString(params);
			Basic endPoint = getSession().getBasicRemote();
			endPoint.sendText(json);
			LoginResponse response = waitFor(LoginResponse.class);
			return response.getData().isResult();
		} catch (JsonProcessingException ex) {
			Log.error("Error converting to JSON", ex);
			return false;
		} catch (IOException ex) {
			Log.error("Error sending request to " + exchange, ex);
			return false;
		}
	}

	private Session getSession() {
		return session;
	}

	public void connect() {
		try {
			final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create()
					.decoders(Arrays.asList(PongDecoder.class,
							SubscribeContractMarketDepthIncrementalInitialResponseDecoder.class,
							SubscribeContractMarketDepthIncrementalResponseDecoder.class,
							SubscribeContractTradeRecordInitialResponseDecoder.class,
							SubscribeContractTradeRecordResponseDecoder.class,
							SubscribeOrderInitialResponseDecoder.class,
							SubscribeOrderSpotDecoder.class,
							SubscribeOrderFuturesDecoder.class,
							LoginResponseDecoder.class,
							ErrorWsDecoder.class,
							SubscribeFuturesAccountInitialResponseDecoder.class,
							SubscribeFuturesAccountResponseDecoder.class,
							SubscribeSpotAccountInitialResponseDecoder.class,
							SubscribeSpotAccountResponseDecoder.class,
                            SubscribeSpotMarginAccountInitialResponseDecoder.class,
                            SubscribePositionFuturesInitialResponseDecoder.class,
                            SubscribePositionFuturesDecoder.class,
							UnsubscribeResponseDecoder.class,
							StringMessageDecoder.class))
					.build();

			ClientManager client = ClientManager.createClient();
			client.getProperties().put(ClientProperties.RECONNECT_HANDLER,
					new ReconnectHandlerImpl(5));
			Log.info("Connector: connect() (Connecting to server)");
			client.connectToServer(this, cec,
					new URI(wsLink));
		} catch (URISyntaxException | DeploymentException | IOException ex) {
			Log.error("Connector: connect() (Could not connect to " + exchange + ")", ex);
		}
	}

	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	private Timer getTimer() {
		if (timer == null) {
			timer = new Timer();
		}
		return timer;
	}

	@Override
	public void close() throws Exception {
		if (session != null) {
			Log.info("Closing Client");
			timer.cancel();
			session.close();
		}
		restClient.close();
	}

	private void invalidateSession() {
		Log.info("Connector" + this.hashCode() +  ": invalidateSession() " + session.hashCode());
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		Log.info("Connector " + this.hashCode() +  ": onOpen() session " + session.hashCode() + " isReconnecting " + isReconnecting);
		this.session = session;
		session.addMessageHandler((MessageHandler.Whole<Message>) (Message t) -> handleMessage(t));

		cancelTimer();
		getTimer().schedule(new PingTask(), 1000, HEART_BEAT_INTERVAL);

		if (isReconnecting) {
			isReconnecting = false;
			singleThreadExecutor.submit(() -> client.onConnectionRestored());
		}
	}

	@Override
	public void onError(Session session, Throwable thr) {
		Log.error("Connector: onError() ", thr);
		throw new RuntimeException();
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
        try {
            getSession().getBasicRemote().sendText("ping");
            Log.info("Connector onClose().. ping sent");
        } catch (Exception e) {
            Log.info("Unable to ping the remote");
        }
        
		Log.info("Connector " + this.hashCode() +  ": onClose() session " + session.hashCode() + " reason " + closeReason );
		invalidateSession();
//		adminListeners.forEach(l -> l.onConnectionLost(DisconnectionReason.FATAL, "You have not been suscribed to any instrument for 30 seconds"));
		try {
//            this.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	private void handleMessage(Message t) {
		if (t instanceof Pong) {
			setPong((Pong) t);
	    } else if (t instanceof StringMessage && ((StringMessage)t).getMessage().equals("pong")) {
	            Pong pong = new Pong();
	            pong.setEvent(Event.pong);
	            setPong(pong);
		} else if (t instanceof SubscribeContractMarketDepthInitialResponse) {
			setSubscribeContractMarketDepthInitialResponse(
					(SubscribeContractMarketDepthInitialResponse) t);
		} else if (t instanceof SubscribeContractMarketDepthResponse) {
		    SubscribeContractMarketDepthResponse response = ((SubscribeContractMarketDepthResponse)t);
		    int i = response.table.indexOf('/');
		    String type = response.table.substring(0, i);
		    response.alias = type + "@" + response.data.instrument_id;
			setSubscribeContractMarketDepthResponse(response);
		} else if (t instanceof SubscribeContractTradeRecordInitialResponse) {
			setSubscribeContractTradeRecordInitialResponse(
					(SubscribeContractTradeRecordInitialResponse) t);
		} else if (t instanceof SubscribeContractTradeRecordResponse) {
		    SubscribeContractTradeRecordResponse response = (SubscribeContractTradeRecordResponse)t;
		    int i = response.table.indexOf('/');
            String type = response.table.substring(0, i);
            response.alias = type + "@" + response.getData().get(0).instrument_id;
			setSubscribeContractTradeRecordResponse(response);
		} else if (t instanceof SubscribeOrderInitialResponse) {
		    setSubscribeOrderInitialResponse(
		            (SubscribeOrderInitialResponse) t);
		} else if (t instanceof SubscribeOrderSpot) {
		    setSubscribeOrderSpotInitialResponse(
		            (SubscribeOrderSpot) t);
		} else if (t instanceof ErrorWs) {
		    onErrorWs(
		            (ErrorWs) t);
		} else if (t instanceof SubscribeOrderFutures) {
		    setSubscribeOrderFutures(
		            (SubscribeOrderFutures) t);
		} else if (t instanceof SubscribeFuturesAccountInitialResponse) {
		    setSubscribeFuturesAccountInitialResponse(
		            (SubscribeFuturesAccountInitialResponse) t);
		} else if (t instanceof SubscribeFuturesAccountResponse) {
		    setSubscribeFuturesAccountResponse(
		            (SubscribeFuturesAccountResponse) t);
		} else if (t instanceof SubscribeSpotAccountInitialResponse) {
		    setSubscribeSpotAccountInitialResponse(
		            (SubscribeSpotAccountInitialResponse) t);
		} else if (t instanceof SubscribeSpotAccountResponse) {
            setSubscribeSpotAccountResponse(
                    (SubscribeSpotAccountResponse) t);    
        } else if (t instanceof SubscribeSpotMarginAccountInitialResponse) {
            setSubscribeSpotMarginAccountInitialResponse(
                    (SubscribeSpotMarginAccountInitialResponse) t);
        } else if (t instanceof SubscribeFuturesPositionInitialResponse) {
            setSubscribeFuturesPositionInitialResponse(
                    (SubscribeFuturesPositionInitialResponse) t);
        } else if (t instanceof SubscribeFuturesPositionResponse) {
            setSubscribeFuturesPositionResponse(
                    (SubscribeFuturesPositionResponse) t);
		} else if (t instanceof LoginResponse) {
			setLoginResponse((LoginResponse) t);
		} else if (t instanceof UnsubscribeResponse) {
			setUnsubscribeResponse((UnsubscribeResponse) t);
		} else {
			// fall back decoder. Print no decoder available + message 
			Log.info("No decoder available. Response is : \n" + t);
		}
	}

	public static interface MarketPriceListener {
		public void onMarketPrice(String symbol, Expiration expiration, MarketPrice marketPrice);
	}

	public static interface MarketDepthListener {
		public void onMarketDepth(String symbol, String action, MarketDepths marketDepths);
	}
	
	public static interface PositionListener {
	    public void onPosition();
	}
	
	public static interface FuturesPositionListener {
	    public void onFuturesPosition(SubscribeFuturesPositionResponse subscribeFuturesPositionResponse);
	}
	
	public static interface FuturesAccountListener {
	    public void onFuturesAccount(SubscribeFuturesAccountResponse response);
	}
	
	public static interface SpotAccountListener {
	    public void onSpotAccount(List<SpotAccount> accounts);
	}

	public static interface TradeRecordListener {
		public void onTradeRecord(String symbol, Expiration expiration, Trade tradeRecord);
	}

	public static interface ForecastPriceListener {
		public void onForecastPrice(ForecastPrice forecastPrice);
	}

	public static interface OrderListener {
		public void onOrder(OrderData order);
	}

	public static interface PositionsFixedMarginListener {
		public void onPositionsFixedMargin(PositionsFixedMargin positions);
	}

	public static interface ConnectionListener {
		public void onConnectionLost(ClosedConnectionType closeConnectionType, String message);
		public void onConnectionRestored();
	}

	private class PingTask extends TimerTask {

	    public PingTask() {
		}

		@Override
		public void run() {
			try {
			    if (!isReconnecting)
				getSession().getBasicRemote().sendText("ping");
			} catch (IOException ex) {
				Log.error("Could not send ping request", ex);
			} catch (Exception e){
			    Log.error("The connection has been closed", e);
			}
		}
	}

	/**
	 * @return the pong
	 */
	@SuppressWarnings("unchecked")
    private <T> T waitFor(Class<T> msgClass) {
		wLock.lock();
		try {
			if (msgClass.equals(Pong.class)) {
				newPong.await();
				return (T) pong;
			} else if (msgClass.equals(SubscribeContractMarketDepthInitialResponse.class)) {
				newSubscribeContractMarketDepthInitialResponse.await();
				return (T) subscribeContractMarketDepthInitialResponse;
			} else if (msgClass.equals(SubscribeOrderInitialResponse.class)) {
			    newSubscribeOrderInitialResponse.await();
			    return (T) subscribeOrderInitialResponse;
			} else if (msgClass.equals(SubscribeOrderSpot.class)) {
			    newSubscribeOrderSpot.await();
			    return (T) subscribeOrderSpot;
			} else if (msgClass.equals(SubscribeOrderFutures.class)) {
			    newSubscribeOrderFutures.await();
			    return (T) subscribeOrderFutures;
			} else if (msgClass.equals(SubscribeFuturesAccountInitialResponse.class)) {
			    newSubscribeFuturesAccountInitialResponse.await();
			    return (T) subscribeFuturesAccountInitialResponse;
			} else if (msgClass.equals(SubscribeFuturesAccountResponse.class)) {
			    newSubscribeFuturesAccountResponse.await();
			    return (T) subscribeFuturesAccountResponse;
			} else if (msgClass.equals(SubscribeSpotAccountInitialResponse.class)) {
			    newSubscribeSpotAccountInitialResponse.await();
			    return (T) subscribeSpotAccountInitialResponse;
			} else if (msgClass.equals(SubscribeSpotAccountResponse.class)) {
			    newSubscribeSpotAccountResponse.await();
			    return (T) subscribeSpotAccountResponse;
	         } else if (msgClass.equals(SubscribeSpotMarginAccountInitialResponse.class)) {
	                newSubscribeSpotMarginAccountInitialResponse.await();
	                return (T) subscribeSpotMarginAccountInitialResponse;
	         } else if (msgClass.equals(SubscribeFuturesPositionResponse.class)) {
	             newSubscribeFuturesPositionResponse.await();
	             return (T) subscribeFuturesPositionResponse;
	         } else if (msgClass.equals(SubscribeFuturesPositionInitialResponse.class)) {
	             newSubscribeFuturesPositionInitialResponse.await();
	             return (T) subscribeFuturesPositionInitialResponse;
			} else if (msgClass.equals(SubscribeContractTradeRecordInitialResponse.class)) {
				newSubscribeContractTradeRecordInitialResponse.await();
				return (T) subscribeContractTradeRecordInitialResponse;
			} else if (msgClass.equals(LoginResponse.class)) {
			    Log.info("msgClass is LoginResponse");
				newLoginResponse.await();
				Log.info("return is LoginResponse");
				return (T) loginResponse;
			} else if (msgClass.equals(UnsubscribeResponse.class)) {
				newUnsubscribeResponse.await();
				return (T) unsubscribeResponse;
			} else {
				throw new UnsupportedOperationException(msgClass.getSimpleName() + " is not supported");
			}
		} catch (InterruptedException ex) {
			Log.error("Interrupted while waiting for message: " + msgClass.getSimpleName(), ex);
			return null;
		} finally {
			wLock.unlock();
		}
	}

	/**
	 * @param pong the pong to set
	 */
	private void setPong(Pong pong) {
		wLock.lock();
		try {
			this.pong = pong;
			newPong.signal();
		} finally {
			wLock.unlock();
		}
	}

	private void setSubscribeContractMarketDepthInitialResponse(
			SubscribeContractMarketDepthInitialResponse subscribeContractMarketDepthInitialResponse) {
		wLock.lock();
		try {
			this.subscribeContractMarketDepthInitialResponse
					= subscribeContractMarketDepthInitialResponse;
			newSubscribeContractMarketDepthInitialResponse.signal();
		} finally {
			wLock.unlock();
		}
	}

	private void setSubscribeContractMarketDepthResponse(
			SubscribeContractMarketDepthResponse subscribeContractMarketDepthResponse) {
		MarketDepths marketDepths = subscribeContractMarketDepthResponse.getData();
		String symbol = subscribeContractMarketDepthResponse.alias;
		client.onMarketDepth(symbol, subscribeContractMarketDepthResponse.action, marketDepths);
	}

	// subscribe to trade
	private void setSubscribeContractTradeRecordInitialResponse(
			SubscribeContractTradeRecordInitialResponse subscribeContractTradeRecordInitialResponse) {
		wLock.lock();
		try {
			this.subscribeContractTradeRecordInitialResponse
					= subscribeContractTradeRecordInitialResponse;
			newSubscribeContractTradeRecordInitialResponse.signal();
		} finally {
			wLock.unlock();
		}
	}
	
	private void setSubscribeSpotAccountInitialResponse(
	        SubscribeSpotAccountInitialResponse subscribeSpotAccountInitialResponse) {
	    wLock.lock();
	    try {
	        this.subscribeSpotAccountInitialResponse
	        = subscribeSpotAccountInitialResponse;
	        newSubscribeSpotAccountInitialResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	}
	
	private void setSubscribeFuturesAccountInitialResponse(
	        SubscribeFuturesAccountInitialResponse subscribeFuturesAccountInitialResponse) {
	    wLock.lock();
	    try {
	        this.subscribeFuturesAccountInitialResponse
	        = subscribeFuturesAccountInitialResponse;
	        newSubscribeFuturesAccountInitialResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	}
	
	private void setSubscribeSpotMarginAccountInitialResponse(
	        SubscribeSpotMarginAccountInitialResponse subscribeSpotMarginAccountInitialResponse) {
	    wLock.lock();
	    try {
	        this.subscribeSpotMarginAccountInitialResponse
	        = subscribeSpotMarginAccountInitialResponse;
	        newSubscribeSpotMarginAccountInitialResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	}
	
	private void setSubscribeFuturesPositionResponse(
	        SubscribeFuturesPositionResponse subscribeFuturesPositionResponse) {
	    wLock.lock();
	    try {
	        this.subscribeFuturesPositionResponse
	        = subscribeFuturesPositionResponse;
	        newSubscribeFuturesPositionResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	    
	    client.onFuturesPosition(subscribeFuturesPositionResponse);
	}
	
	private void setSubscribeFuturesPositionInitialResponse(
	        SubscribeFuturesPositionInitialResponse subscribeFuturesPositionInitialResponse) {
	    wLock.lock();
	    try {
	        this.subscribeFuturesPositionInitialResponse
	        = subscribeFuturesPositionInitialResponse;
	        newSubscribeFuturesPositionInitialResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	}

	// subscribe to trade
	private void setSubscribeContractTradeRecordResponse(
			SubscribeContractTradeRecordResponse subscribeContractTradeRecordResponse) {
		String symbol = subscribeContractTradeRecordResponse.alias;
		for (Trade tradeRecord : subscribeContractTradeRecordResponse.getData()) {
			client.onTradeRecord(symbol, Expiration.quarter, tradeRecord);
		}
	}

	
	private void setSubscribeOrderInitialResponse(
	        SubscribeOrderInitialResponse subscribeOrderInitialResponse) {
	    wLock.lock();
	    try {
	        this.subscribeOrderInitialResponse
	        = subscribeOrderInitialResponse;
	        newSubscribeOrderInitialResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	    client.onPosition();
	}
	
	private void setSubscribeFuturesAccountResponse(
	        SubscribeFuturesAccountResponse subscribeFuturesAccountResponse) {
	    wLock.lock();
	    try {
	        this.subscribeFuturesAccountResponse
	        = subscribeFuturesAccountResponse;
	        newSubscribeFuturesAccountResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	    client.onFuturesAccount(subscribeFuturesAccountResponse);
	}
	
	private void setSubscribeSpotAccountResponse(
	        SubscribeSpotAccountResponse subscribeSpotAccountResponse) {
	    wLock.lock();
	    try {
	        this.subscribeSpotAccountResponse
	        = subscribeSpotAccountResponse;
	        newSubscribeSpotAccountResponse.signal();
	    } finally {
	        wLock.unlock();
	    }
	    client.onSpotAccount(subscribeSpotAccountResponse.data);
	}
	
    private void setSubscribeOrderSpotInitialResponse(SubscribeOrderSpot subscribeOrderSpotInitialResponse) {
        List<OrderDataSpot> orders = subscribeOrderSpotInitialResponse.getData();

        for (OrderDataSpot order : orders) {
            if (order instanceof OrderDataSpot) {
                order.setInstrumentType("spot");
            }
            client.onOrder(order);
        }
    }
	
	private void onErrorWs (ErrorWs error) {
	    Log.info("Error");
	}
	
    private void setSubscribeOrderFutures(SubscribeOrderFutures subscribeOrderFutures) {
        List<OrderDataFutures> orders = subscribeOrderFutures.getData();

        for (OrderDataFutures order : orders) {
            if (order instanceof OrderDataFutures) {
                order.setInstrumentType("futures");
            }
            client.onOrder(order);
        }
    }

	private void setLoginResponse(LoginResponse loginResponse) {
	    Log.info("setLoginResponse preLock");
		wLock.lock();
		Log.info("setLoginResponse Lock");
		try {
			this.loginResponse = loginResponse;
			Log.info(loginResponse.toString());
			if (loginResponse.getData().isResult()) {
				Log.info("Login Successful...");
				newLoginResponse.signal();
			} else {
				Log.info("Login Unsuccessful...");
                newLoginResponse.signal();
			}
		} finally {
			wLock.unlock();
			Log.info("setLoginResponse unLock");
		}

	}

    private void setUnsubscribeResponse(UnsubscribeResponse unsubscribeResponse) {
        wLock.lock();
        try {
            this.unsubscribeResponse = unsubscribeResponse;
            Log.debug(unsubscribeResponse.toString());
            if (this.unsubscribeResponse.getEvent().contains("unsubscribe")) {
                Log.info("Unsubscribing  Successful...");
                newUnsubscribeResponse.signal();
            } else {
                Log.debug("Unsubscribing is Unsuccessful...");
            }
        } finally {
            wLock.unlock();
        }
    }

	public PlaceOrderResponse placeOrder(PlaceOrderRequest order) {
		try (RestClient restClient = new RestClient(apiKey, secretKey, exchange)) {
			return restClient.placeOrder(order, apiKey, secretKey, passPhraze);
		}
	}


	public enum ClosedConnectionType {
		Disconnect, ConnectionFailure;
	}

	private class ReconnectHandlerImpl extends ClientManager.ReconnectHandler {

		private final long delay;

		public ReconnectHandlerImpl(long delay) {
			this.delay = delay;
		}

		@Override
		public boolean onDisconnect(CloseReason closeReason) {
			client.onConnectionLost(ClosedConnectionType.Disconnect, closeReason.getReasonPhrase());
			
            if (closeReason.getCloseCode() == CloseReason.CloseCodes.NORMAL_CLOSURE) {
                Log.info("OkexConnector.ReconnectHandlerImpl " + this.hashCode() + ": onDisconnect() Disconnect due to normal closure. No need to reconnect.");
                return false;
            } else if (closeReason.getCloseCode().getCode() == 4001) {
                if (closeReason.getReasonPhrase().equals("no data received in 30s")) {
                    adminListeners.forEach(l -> l.onConnectionLost(DisconnectionReason.FATAL, "You have not been suscribed to any instrument for 30 seconds"));
                }
                return false;
			}
			Log.info("Connector: onDisconnect() Got Disconnected.  Reconnecting...");
		     adminListeners.forEach(l -> l.onConnectionLost(DisconnectionReason.NO_INTERNET, "(onDisconnect)"));
			return true;
		    
		}

		@Override
		public boolean onConnectFailure(Exception exception) {
			client.onConnectionLost(ClosedConnectionType.ConnectionFailure, "Network Connection Problem");
			Log.info("Connector.ReconnectHandlerImpl " + this.hashCode() +  ": onDisconnect (### Reconnecting caused by:  " + exception.getMessage());
			if (!isReconnecting) {
		          adminListeners.forEach(l -> l.onConnectionLost(DisconnectionReason.NO_INTERNET, "(onConnectFailure)"));
			}
			isReconnecting = true;
			return true;
		}

		@Override
		public long getDelay() {
			return delay;
		}
	}

	public String getLoginResponseMessage() {
	    return loginResponse == null ? "" : loginResponse.getMessage();
	}
}
