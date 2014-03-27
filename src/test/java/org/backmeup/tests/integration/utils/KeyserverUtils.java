package org.backmeup.tests.integration.utils;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import java.util.Properties;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.jayway.restassured.response.ValidatableResponse;

public class KeyserverUtils {
	@SuppressWarnings("unchecked")
	public static ValidatableResponse createAccessToken(long userId, String password, long serviceId, long authInfoId, long backupDate, boolean reusable, String encryptionPwd) {
		JSONObject jsonTokenRequest = new JSONObject();
		jsonTokenRequest.put("bmu_user_id", userId);
		jsonTokenRequest.put("user_pwd", password);
		
		
		jsonTokenRequest.put("backupdate", backupDate);
		jsonTokenRequest.put("reusable", reusable);
		jsonTokenRequest.put("encryption_pwd", encryptionPwd);
		
		JSONArray jsonServiceIds = new JSONArray();
		jsonServiceIds.add(serviceId);
		jsonTokenRequest.put("bmu_service_ids", jsonServiceIds);
		
		JSONArray jsonAuthInfoIds = new JSONArray();
		jsonAuthInfoIds.add(authInfoId);
		jsonTokenRequest.put("bmu_authinfo_ids", jsonAuthInfoIds);
		
		ValidatableResponse response = 
		given()
			.contentType("application/json")
			.body(jsonTokenRequest.toJSONString())
		.when()
			.post("/tokens/token")
		.then()
			.statusCode(200)
			.assertThat().body(containsString("token"));
		
		return response;
	}
	
	@SuppressWarnings("unchecked")
	public static void addAuthInfo(long userId, String password, long serviceId, long authInfoId, Properties authInfoProperties) {

		JSONObject jsonAuthInfo = new JSONObject();
		jsonAuthInfo.put("bmu_user_id", userId);
		jsonAuthInfo.put("user_pwd", password);
		jsonAuthInfo.put("bmu_service_id", serviceId);
		jsonAuthInfo.put("bmu_authinfo_id", authInfoId);		
		
		JSONObject jsonAuthInfoProps = new JSONObject();
		
		for(Entry<?,?> entry : authInfoProperties.entrySet()) {
			String key = (String) entry.getKey();  
			String value = (String) entry.getValue();  
			jsonAuthInfoProps.put(key,value);
		}
		
		jsonAuthInfo.put("ai_data", jsonAuthInfoProps);
				
		given()
			.contentType("application/json")
			.body(jsonAuthInfo.toJSONString())
		.when()
			.post("/authinfos/add")
		.then()
			.statusCode(204);		
	}
	
	public static void deleteAuthInfo(long authInfoId) {
		when()
		  .delete("/authinfos/" + authInfoId)
		.then()
			.statusCode(204);		
	}
	
	public static void addUser(String userId, String password) {
		given()
			.header("Accept", "application/json")
		.when()
			.post("/users/" + userId + "/" + password + "/register")
		.then()
			.assertThat().statusCode(204);
	}
	
	public static void deleteUser(String userId){
		when()
			.delete("/users/" + userId)
		.then()
			.statusCode(204);
	}
	
	public static void addService(String serviceId){
		given()
			.header("Accept", "application/json")
		.when()
			.post("/services/" + serviceId + "/register")
		.then()
			.assertThat().statusCode(204);
	}
	
	public static void deleteService(String serviceId){
		when()
			.delete("/services/" + serviceId)
		.then()
			.statusCode(204);
	}
}
