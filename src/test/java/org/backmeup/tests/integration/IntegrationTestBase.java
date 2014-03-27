package org.backmeup.tests.integration;

import java.io.InputStream;

import org.backmeup.tests.utils.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;

public abstract class IntegrationTestBase {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Configuration config = new Configuration();
		InputStream in = IntegrationTestBase.class.getClassLoader().getResourceAsStream("integrationtests.properties");
		config.load(in);
		in.close();
		
		RestAssured.baseURI = config.getProperty("backmeup.service.baseuri");
		RestAssured.port = Integer.parseInt(config.getProperty("backmeup.service.port"));
		RestAssured.basePath = config.getProperty("backmeup.service.basepath");
		RestAssured.defaultParser = Parser.JSON;
		RestAssured.requestContentType("application/json");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		RestAssured.reset();
	}
}
