/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author aris
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = CustomLoginResponseDeserializer.class)
@EqualsAndHashCode(callSuper=false)
public class LoginResponse extends Message {
	int binary;
	String channel;
	LoginData data;
	
	String message;
	int errorCode;
	
	
	
}
