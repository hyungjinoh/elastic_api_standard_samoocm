package com.rayful.elastic.api.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {
	private static Gson gson = new Gson();
	private static GsonBuilder gsonBuilder = new GsonBuilder();
	
	private GsonUtils() {}
	
	public static <T> String toJson(T object) {
		return gson.toJson(object);
	}
	
	public static <T> String toPrettyJson(T object) {
		return gsonBuilder.setPrettyPrinting().create().toJson(object);
	}
}
