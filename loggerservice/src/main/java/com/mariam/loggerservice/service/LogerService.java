package com.mariam.loggerservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mariam.loggerservice.repository.LogRepository;

@Service
public class LogerService {

	@Autowired
	LogRepository logREpository ;
	
	public void Log() {
		//Log newLog = 
	}
}
