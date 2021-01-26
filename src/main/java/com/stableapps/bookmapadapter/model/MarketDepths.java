/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

/**
 *
 * @author aris
 */
@Data
@JsonDeserialize(using = CustomMarketDepthsDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketDepths {

    public String instrument_id;
    public String timestamp;
    public int checksum;
    
	public List<List<Double>> asks = null;
	public List<List<Double>> bids = null;

	List<MarketDepth> askDatas = new ArrayList<>();;
	List<MarketDepth> bidDatas = new ArrayList<>();;

	public void setAsks(List<List<Double>> asks) {
		this.asks = asks;
		
		MarketDepth askData;
		for (List<Double> ask : asks) {
			askData = MarketDepth.builder()
			  .price(ask.get(0))
			  .contractAmount(ask.get(1))
			  .coinAmount(ask.get(2))
			  .coinCumulant(ask.get(3))
			  .contractCumulant(ask.get(4))
			  .build();
			askDatas.add(askData);
		}

	}

	public void setBids(List<List<Double>> bids) {
		this.bids = bids;
		
		MarketDepth bidData;
		for (List<Double> bid : bids) {
			bidData = MarketDepth.builder()
			  .price(bid.get(0))
			  .contractAmount(bid.get(1))
			  .coinAmount(bid.get(2))
			  .coinCumulant(bid.get(3))
			  .contractCumulant(bid.get(4))
			  .build();
			bidDatas.add(bidData);
		}
	}
}
