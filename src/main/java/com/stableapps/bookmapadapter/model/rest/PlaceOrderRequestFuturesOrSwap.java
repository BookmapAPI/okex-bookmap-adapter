/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author aris
 */
@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceOrderRequestFuturesOrSwap extends PlaceOrderRequest {

    @JsonProperty("instrument_id")
    String instrumentId;
    @JsonProperty("order_type")
    String orderType;
	double price;

	@JsonProperty("match_price")
	@Builder.Default
	String matchPrice = "0";
	@JsonProperty("leverage")
	String leverage;

	@JsonProperty("size")
	int size;

}
