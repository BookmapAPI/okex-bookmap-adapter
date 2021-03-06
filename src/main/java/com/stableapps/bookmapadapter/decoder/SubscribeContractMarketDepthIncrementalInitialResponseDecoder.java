package com.stableapps.bookmapadapter.decoder;

import com.stableapps.bookmapadapter.model.SubscribeContractMarketDepthInitialResponse;

/**
 *
 * @author aris
 */

public class SubscribeContractMarketDepthIncrementalInitialResponseDecoder extends
	AbstractDecoder<SubscribeContractMarketDepthInitialResponse> {

	public SubscribeContractMarketDepthIncrementalInitialResponseDecoder() {
		super(SubscribeContractMarketDepthInitialResponse.class);
	}

	@Override
	public boolean willDecode(String arg0) {
	    boolean contains = arg0.contains("\"event\":\"subscribe\",\"channel\":\"")&&arg0.contains("depth:");
		return contains;
	}

}
