package org.backmeup.tests.integration.keyserver;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.backmeup.tests.IntegrationTest;
import org.backmeup.tests.integration.IntegrationTestBase;
import org.backmeup.tests.integration.utils.KeyserverUtils;
import org.backmeup.tests.utils.Configuration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.response.ValidatableResponse;

/*
 * for examples rest-assured see:
 * https://github.com/jayway/rest-assured/tree/master/examples/rest-assured-itest-java/src/test/java/com/jayway/restassured/itest/java
 */

@Category(IntegrationTest.class)
public class KeyserverIntegrationTest {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Configuration config = new Configuration();
		InputStream in = IntegrationTestBase.class.getClassLoader().getResourceAsStream("integrationtests.properties");
		config.load(in);
		in.close();
		
		RestAssured.baseURI = config.getProperty("backmeup.keyserver.baseuri");
		RestAssured.port = Integer.parseInt(config.getProperty("backmeup.keyserver.port"));
		RestAssured.basePath = config.getProperty("backmeup.keyserver.basepath");
		RestAssured.defaultParser = Parser.JSON;
		RestAssured.requestContentType("application/json");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		RestAssured.reset();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testRegisterUser() {
		String userId = "101";
		String password = "password1";

		try {
			given()
				.header("Accept", "application/json")
			.when()
				.post("/users/" + userId + "/" + password + "/register")
			.then()
				.assertThat().statusCode(204);
		} finally {
			KeyserverUtils.deleteUser(userId);
		}
	}

	@Test
	public void testDeleteUser() {
		String userId = "101";
		String password = "password1";
		
		KeyserverUtils.addUser(userId, password);
		
		when()
			.delete("/users/" + userId)
		.then()
			.statusCode(204);
	}
	
	@Test
	public void testRegisterService() {
		String serviceId = "101";
		try {
			given()
				.header("Accept", "application/json")
			.when()
				.post("/services/" + serviceId + "/register")
			.then()
				.assertThat().statusCode(204);
		} finally {
			KeyserverUtils.deleteService(serviceId);
		}
	}
	
	@Test
	public void testDeleteService() {
		String serviceId = "101";
		
		KeyserverUtils.addService(serviceId);
		
		when()
			.delete("/services/" + serviceId)
		.then()
			.statusCode(204);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testAddAuthInfos() {
		long userId = 101;
		String password = "password1";
		long serviceId = 201;
		long authInfoId = 301;

		JSONObject jsonAuthInfo = new JSONObject();
		jsonAuthInfo.put("bmu_user_id", userId);
		jsonAuthInfo.put("user_pwd", password);
		jsonAuthInfo.put("bmu_service_id", serviceId);
		jsonAuthInfo.put("bmu_authinfo_id", authInfoId);		
		
		JSONObject jsonAuthInfoProps = new JSONObject();
		jsonAuthInfoProps.put("ai_pwd","PW##!123");
		jsonAuthInfo.put("ai_data", jsonAuthInfoProps);
		
		try {	
			KeyserverUtils.addUser(userId + "", password);
			KeyserverUtils.addService(serviceId + "");
			
			given()
				.contentType("application/json")
				.body(jsonAuthInfo.toJSONString())
			.when()
				.post("/authinfos/add")
			.then()
				.statusCode(204);
		} finally {
			KeyserverUtils.deleteService(serviceId + "");
			KeyserverUtils.deleteUser(userId + "");
		}
	}
	
	@Test
	public void testDeleteAuthInfos() {
		long userId = 101;
		String password = "password1";
		long serviceId = 201;
		long authInfoId = 301;
		
		Properties authInfoProperties = new Properties();
		authInfoProperties.put("ai_pwd","PW##!123");
		
		try {
			KeyserverUtils.addUser(userId + "", password);
			KeyserverUtils.addService(serviceId + "");
			KeyserverUtils.addAuthInfo(userId, password, serviceId, authInfoId, authInfoProperties);
	
			when()
				.delete("/authinfos/" + authInfoId)
			.then()
				.statusCode(204);
		} finally {
			KeyserverUtils.deleteService(serviceId + "");
			KeyserverUtils.deleteUser(userId + "");
		}
	}
	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testCreateAccessToken() {
		long userId = 101;
		String password = "password1";
		long serviceId = 201;
		long authInfoId = 301;
		long backupDate = new Date().getTime();
		boolean reusable = true;
		String encryptionPwd = "encPWD1";
		
		Properties authInfoProperties = new Properties();
		authInfoProperties.put("ai_pwd","PW##!123");
		
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
		
		try {
			KeyserverUtils.addUser(userId + "", password);
			KeyserverUtils.addService(serviceId + "");
			KeyserverUtils.addAuthInfo(userId, password, serviceId, authInfoId, authInfoProperties);
		
			given()
				.contentType("application/json")
				.body(jsonTokenRequest.toJSONString())
			.when()
				.post("/tokens/token")
			.then()
				.statusCode(200)
				.assertThat().body(containsString("token"));
		} finally {
			KeyserverUtils.deleteAuthInfo(authInfoId);
			KeyserverUtils.deleteService(serviceId + "");
			KeyserverUtils.deleteUser(userId + "");
		}
	}
	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testAccessAuthData() {
		long userId = 101;
		String password = "password1";
		long serviceId = 201;
		long authInfoId = 301;
		long backupDate = new Date().getTime();
		boolean reusable = true;
		String encryptionPwd = "encPWD1";
		
		String aiUsername = "username1";
		String aiPassword = "PW##!123";
		
		Properties authInfoProperties = new Properties();
		authInfoProperties.put("ai_user", aiUsername);
		authInfoProperties.put("ai_pwd",aiPassword);
		
		try {
			KeyserverUtils.addUser(userId + "", password);
			KeyserverUtils.addService(serviceId + "");
			KeyserverUtils.addAuthInfo(userId, password, serviceId, authInfoId, authInfoProperties);
			ValidatableResponse tokenResp = KeyserverUtils.createAccessToken(userId, password,
				serviceId, authInfoId, backupDate, reusable, encryptionPwd);
		
			String tokenId = tokenResp.extract().path("bmu_token_id").toString();
			String token = tokenResp.extract().path("token");
				
			JSONObject jsonDataRequest = new JSONObject();
			jsonDataRequest.put("bmu_token_id", tokenId);
			jsonDataRequest.put("token", token);
		
			ValidatableResponse response = 
			given()
				.contentType("application/json")
				.body(jsonDataRequest.toJSONString())
			.when()
				.post("/tokens/data")
			.then()
				.statusCode(200)
				.assertThat().body(containsString("authinfos"));
		
			List<String> authInfoUsernames = response.extract().path("authinfos.ai_data.ai_user");
			List<String> authInfopasswords = response.extract().path("authinfos.ai_data.ai_pwd");
		
			Assert.assertEquals(aiUsername, authInfoUsernames.get(0));
			Assert.assertEquals(aiPassword, authInfopasswords.get(0));
		} finally {
			KeyserverUtils.deleteAuthInfo(authInfoId);
			KeyserverUtils.deleteService(serviceId + "");
			KeyserverUtils.deleteUser(userId + "");
		}
	}
}
