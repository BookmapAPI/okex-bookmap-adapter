/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author aris
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Trade {

    String side;
    String trade_id;
	double price;
	public String instrument_id;
	double qty;
	String timestamp;
	
	@JsonSetter("qty")
	public void setQtyFromQty (double qty){
	    this.qty = qty;
	}
	
	@JsonSetter("size")
	public void setQtyFromSize (double size){
	    this.qty = size;
	}
	
}
