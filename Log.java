package com.veivo.utils;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {
	
	public static void debug(Class<?> clazz, String message) {
		Logger log = LogManager.getLogger(clazz);
		log.debug(message);
	}
	
	public static void info(Class<?> clazz, String message) {
		Logger log = LogManager.getLogger(clazz);
		log.info(message);
	}
	
	public static void error(Class<?> clazz, String message) {
		Logger log = LogManager.getLogger(clazz);
		log.error(message);
	}
	
	public static Timestamp getNow() {
		return new Timestamp(System.currentTimeMillis());
	}

}
