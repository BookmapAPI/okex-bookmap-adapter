/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.decoder;

import javax.websocket.DecodeException;

import com.stableapps.bookmapadapter.model.StringMessage;

/**
 *
 * @author aris
 */
public class StringMessageDecoder extends AbstractDecoder<StringMessage> {

	public StringMessageDecoder() {
		super(StringMessage.class);
	}

	@Override
	public boolean willDecode(String arg0) {
		return true;
	}

	@Override
	public StringMessage decode(String arg0) throws DecodeException {
		StringMessage message = new StringMessage(arg0);
		return message;
	}

	

}
