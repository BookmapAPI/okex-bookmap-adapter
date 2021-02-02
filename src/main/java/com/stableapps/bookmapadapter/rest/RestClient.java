/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.rest;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableapps.bookmapadapter.client.Connector;
import com.stableapps.bookmapadapter.model.rest.ErrorResponse;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderRequest;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderRequestFuturesOrSwap;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderRequestTokenOrMargin;
import com.stableapps.bookmapadapter.model.rest.PlaceOrderResponse;

import velox.api.layer1.common.Log;

/**
 *
 * @author aris
 */
public class RestClient extends AbstractRestClient {
	private static final int MAX_RETRY = 1;

	public RestClient(String apiKey, String secretKey, String exchange) {
		super("https://www." + exchange + ".com", apiKey, secretKey);
	}

	public PlaceOrderResponse placeOrder(PlaceOrderRequest order, String apiKey, String secretKey, String passPhraze) {

        if (order instanceof PlaceOrderRequestTokenOrMargin) {
            PlaceOrderRequestTokenOrMargin order0 = (PlaceOrderRequestTokenOrMargin) order;
            ObjectMapper mapper = new ObjectMapper();
            String s = null;

            try {
                s = mapper.writeValueAsString(order0);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return call("/api/spot/v3/orders", s, PlaceOrderResponse.class, apiKey, secretKey, passPhraze);
        } else if (order instanceof PlaceOrderRequestFuturesOrSwap) {
            PlaceOrderRequestFuturesOrSwap order0 = (PlaceOrderRequestFuturesOrSwap) order;
            ObjectMapper mapper = new ObjectMapper();
            String s = null;
            
            try {
                s = mapper.writeValueAsString(order0);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            
            return call("/api/futures/v3/order", s, PlaceOrderResponse.class, apiKey, secretKey, passPhraze);
        }
        return null;
	}

	public <R> R call(String path, String json, Class<R> responseClass, String apiKey, String secretKey, String passPhraze) {
		int retry = 0;
		ErrorResponse errorResponse = null;
		
		while (retry < MAX_RETRY) {
			try {
			    String timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
			    String formAsJson = json;
			    String messageBody = Connector.createMessageBody(timestamp, "POST", path, formAsJson);
			    String sign = Connector.generateSignature(secretKey, messageBody);
			    
			    Response response = getWebTarget()
				    .path(path)
					.request(javax.ws.rs.core.MediaType.APPLICATION_JSON)
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36")
					.header("OK-ACCESS-KEY", apiKey)
					.header("OK-ACCESS-SIGN", sign)
					.header("OK-ACCESS-TIMESTAMP", timestamp)
					.header("OK-ACCESS-PASSPHRASE", passPhraze)
					.post(javax.ws.rs.client.Entity.entity(
                                        formAsJson,
                                        javax.ws.rs.core.MediaType.APPLICATION_JSON)
					);
			    
			    R r = null;
			    if (response.getStatus() == 200) {
			        r = response.readEntity(responseClass);
			    } else {
			        errorResponse = response.readEntity(ErrorResponse.class);
			        throw new Exception(errorResponse.getMessage() + " (error code " + errorResponse.getCode() + ")" );
			    }
			   
				Log.info(String.format("Rest call to %s successful", path));
				return r;
			} catch (Exception e) {
				Log.info(String.format("Rest call to %s failed: %s", path, e.getMessage()));
				try {
					retry++;
					Log.info(String.format("Retrying rest call to %s n times: %d", path, retry));
					Thread.sleep(retry * 1000);
				} catch (InterruptedException ex) {
					Log.info("Interrupted while trying to retry rest call");
					return null;
				}
			}
		}

		Log.info( "Giving up calling REST: " +path);

		try {
		    R blankResponse = responseClass.newInstance();
		    
		    try {
		        Field field = responseClass.getDeclaredField("errorMessage");
		        field.setAccessible(true);
		        field.set(blankResponse, errorResponse.getMessage());
		        
		        Field field1 = responseClass.getDeclaredField("errorCode");
                field1.setAccessible(true);
                field1.set(blankResponse, errorResponse.getCode());
	        } catch (NoSuchFieldException | SecurityException e) {
	            e.printStackTrace();
            }
		    return blankResponse;
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new RuntimeException("Can't instantiate a blank response class");
		}
	}
	
    @SuppressWarnings("unchecked")
    public <R> R customCall(String path, String method, String json, Class<R> responseClass, String apiKey,
            String secretKey, String passPhraze, List<Map.Entry<String, String>> queryParams) {
        int retry = 0;

        while (retry < MAX_RETRY) {
            try {
                String timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
                String formAsJson = json;
                String pathForMessage = path;
                String messageBody = Connector.createMessageBody(timestamp, method, pathForMessage, queryParams,
                        formAsJson);
                String sign = Connector.generateSignature(secretKey, messageBody);
                WebTarget target = getWebTarget().path(path);

                if (queryParams != null) {
                    for (Map.Entry<String, String> entry : queryParams) {
                        target = target.queryParam(entry.getKey(), entry.getValue());
                    }
                }

                Response response = target.request(javax.ws.rs.core.MediaType.APPLICATION_JSON).header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36")
                        .header("OK-ACCESS-KEY", apiKey).header("OK-ACCESS-SIGN", sign)
                        .header("OK-ACCESS-TIMESTAMP", timestamp).header("OK-ACCESS-PASSPHRASE", passPhraze)
                        .get(Response.class);
                if (response.getStatus() != Response.Status.OK.getStatusCode() ) {
                    // if they put the custom error stuff in the entity
                    String errorMessage = response.readEntity(String.class);
                    Log.info(errorMessage);
                    throw new Exception(errorMessage);
                } else {
                    Log.info(String.format("Rest call to %s successful", path));
                    return response.readEntity(responseClass);    
                }
            } catch (Exception e) {
                Log.info(String.format("Rest call to %s failed: %s", path, e.getMessage()));
                try {
                    retry++;
                    Log.info(String.format("Retrying rest call to %s n times: %d", path, retry));
                    Thread.sleep(retry * 1000);
                } catch (InterruptedException ex) {
                    Log.info("Interrupted while trying to retry rest call");
                    return null;
                }
            }
        }
	    
	    Log.info( "Giving up calling REST: " +path);
	    
	    try {
            if (responseClass.isArray()) {
                Class<?> componentType = responseClass.getComponentType();
                return (R) Array.newInstance(componentType, 0);
            } else {
                return responseClass.newInstance();
            }
	    } catch (InstantiationException | IllegalAccessException ex) {
	        throw new RuntimeException("Can't instantiate a blank response class" + responseClass.getCanonicalName());
	    }
	}

}
