/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.decoder;

import java.util.Arrays;

import com.stableapps.bookmapadapter.model.LoginResponse;

/**
 *
 * @author aris
 */
public class LoginResponseDecoder extends AbstractDecoder<LoginResponse> {

    private static final String ERROR_CODE_PREFIX = "\"errorCode\":";

    public LoginResponseDecoder() {
		super(LoginResponse.class);
	}

	private int[] authErrorCodes = new int[] {
	        30001, //request header "OK_ACCESS_KEY" cannot be blank
	        30002, //request header "OK_ACCESS_SIGN" cannot be blank
	        30003, //request header "OK_ACCESS_TIMESTAMP" cannot be blank
	        30004, //request header "OK_ACCESS_PASSPHRASE" cannot be blank
	        30005, //invalid OK_ACCESS_TIMESTAMP
	        30006, //invalid OK_ACCESS_KEY
	        30012, //invalid authorization
	        30013, //invalid sign
	        30027, //login failure,operating orders of other users
	        30028, //unauthorized execution
	        30029  //account suspended
	};

	private String[] candidates = initializeCandidates(authErrorCodes, "\"event\":\"login\",");

	@Override
	public boolean willDecode(String arg0) {
	    return Arrays.stream(candidates).anyMatch(v -> arg0.contains(v));
	}

	private String[] initializeCandidates(int[] authErrorCodes, String... otherCandidates) {
	    String[] candidates = new String[authErrorCodes.length + otherCandidates.length];
	    
	    for (int i = 0; i < authErrorCodes.length; i++) {
	        candidates[i] = ERROR_CODE_PREFIX + authErrorCodes[i];
	    }
	    for (int i = authErrorCodes.length, k = 0; i < candidates.length; i++, k++) {
            candidates[i] = otherCandidates[k];
        }
	    return candidates;
	}

}
