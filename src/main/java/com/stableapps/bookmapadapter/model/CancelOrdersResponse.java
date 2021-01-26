/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *
 * @author aris
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CancelOrdersResponse {

	boolean result;
	@JsonProperty("order_id")
	long orderId;
	@JsonProperty("error_code")
	String errorCode;
}
