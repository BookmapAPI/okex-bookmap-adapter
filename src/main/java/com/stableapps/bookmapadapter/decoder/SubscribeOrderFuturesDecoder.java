package com.stableapps.bookmapadapter.decoder;

import com.stableapps.bookmapadapter.model.SubscribeOrderFutures;

public class SubscribeOrderFuturesDecoder
  extends AbstractDecoder<SubscribeOrderFutures> {

	public SubscribeOrderFuturesDecoder() {
		super(SubscribeOrderFutures.class);
	}

	@Override
	public boolean willDecode(String arg0) {
        boolean contains = arg0.contains("table\":\"futures/order\",\"data\":[");
        return contains;
	}

}
