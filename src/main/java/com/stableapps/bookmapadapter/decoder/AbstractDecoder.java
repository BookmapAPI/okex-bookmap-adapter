/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 *
 * @author aris
 * @param <T> T is the class to convert JSON response to.
 */
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractDecoder<T> implements Decoder.Binary<T> {

	public static final Pattern INIT_RESPONSE_PATTERN = Pattern.compile("^.*\"result\"\\s*:\\s*true\\s*,\\s*\"channel\"\\s*:\\s*\"([^\"]+)\".*$");
	public static final Pattern RESPONSE_PATTERN = Pattern.compile("^.*\"binary\"\\s*:\\s*\\d+\\s*,\"channel\"\\s*:\\s*\"(ok_sub_futureusd_[^\"]+)\".*$");
	static String string = "";

	private static String uncompress(ByteBuffer buff) {
		byte[] bytes = buff.array();
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             final Deflate64CompressorInputStream zin = new Deflate64CompressorInputStream(in)) {
            final byte[] buffer = new byte[1024];
            int offset;
            while (-1 != (offset = zin.read(buffer))) {
                out.write(buffer, 0, offset);
            }
            return out.toString();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

	protected ObjectMapper objectMapper;
	private final Class<T> msgClass;

	public AbstractDecoder(Class<T> msgClass) {
		this.msgClass = msgClass;
	}

	@Override
	public T decode(ByteBuffer buffer) throws DecodeException {
		return decode(uncompress(buffer));
	}
	
	public T decode(String arg0) throws DecodeException {
		try {
			if (arg0.startsWith("[")) {
				arg0 = arg0.substring(1, arg0.length() - 1);
			}
			return objectMapper.readValue(arg0, msgClass);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean willDecode(ByteBuffer buffer) {
		return willDecode(uncompress(buffer));
	};
	
	public abstract boolean willDecode(String arg0);

	@Override
	public void init(EndpointConfig ec) {
		objectMapper = new ObjectMapper();
	}

	@Override
	public void destroy() {
	}
}
