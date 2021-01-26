/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author aris
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscribeContractUserPositionsFixedMarginResponse extends Message {

	public boolean binary;
	public String channel;
	public PositionsFixedMargin data;
}
