/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

/**
 *
 * @author aris
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public enum OrderTypeFuturesOrSwap {
	OpenLongPosition(1),
	OpenShortPosition(2),
	CloseLongPosition(3),
	CloseShortPosition(4);

	@Getter
	private int value;

	OrderTypeFuturesOrSwap(int value) {
		this.value = value;
	}

	public static OrderTypeFuturesOrSwap valueOf(int value) {
		switch (value) {
			case 1:
				return OpenLongPosition;
			case 2:
				return OpenShortPosition;
			case 3:
				return CloseLongPosition;
			case 4:
				return CloseShortPosition;
			default:
				throw new IllegalArgumentException("Value should be within [1-4]");
		}
	}

}
