package org.backmeup.keyserver.dal.rabbitmq.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.backmeup.keyserver.config.Configuration;
import org.backmeup.keyserver.dal.LogDao;
import org.backmeup.keysrv.rest.data.LogContainer;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.User;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class LogDaoImpl implements LogDao {

	private String mqHost;
	private String mqName;
	private String charset;
	private Connection mqConnection;
	private Channel mqChannel;

	public LogDaoImpl() {
		mqHost = Configuration.getProperty("keyserver.queue.host");
		mqName = Configuration.getProperty("keyserver.queue.name");
		charset = Configuration.getProperty("keyserver.charset");

		try {
			this.init();
		} catch (IOException e) {
			FileLogger.logException("initialize connection to queue failed", e);
		}
	}

	private void init() throws IOException {
		// Setup connection to the message queue
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(mqHost);

		mqConnection = factory.newConnection();
		mqChannel = mqConnection.createChannel();
		mqChannel.queueDeclare(mqName, false, false, false, null);
	}

	public void close() {
		try {
			mqChannel.close();
			mqConnection.close();
		} catch (IOException e) {
			FileLogger.logException("Closing connection to queue failed", e);
		}
	}

	@Override
	public void insertLog(User user, String message, String type) {
		insertLog(user, null, null, message, type);
	}

	@Override
	public void insertLog(User user, AuthInfo authinfo, String message,
			String type) {
		insertLog(user, authinfo, null, message, type);
	}

	@Override
	public void insertLog(User user, Token token, String message, String type) {
		insertLog(user, null, token, message, type);
	}

	@Override
	public void insertLog(User user, AuthInfo authinfo, Token token,
			String message, String type) {

		long authinfoId = -1L;
		long serviceId = -1L;
		long tokenId = -1L;

		if (authinfo != null) {
			authinfoId = authinfo.getId();
			serviceId = authinfo.getService().getId();
		}

		if (token != null) {
			tokenId = token.getId();
		}

		LogContainer container = new LogContainer(user.getId(), serviceId,
				authinfoId, tokenId, new Date().getTime(), type, message);

		Gson gson = new Gson();
		String logmessage = gson.toJson(container);
		FileLogger.logDebug("Send Json message to queue: " + logmessage);

		try {
			mqChannel.basicPublish("", mqName, null,
					logmessage.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			FileLogger.logException("converting message to " + charset
					+ " failed", e);
		} catch (IOException e) {
			FileLogger.logException("send logmessage to queue failed", e);
		}
	}

	@Override
	public List<LogContainer> getLogs(User user) {
		// return emty Container to be compatible
		List<LogContainer> containers = new LinkedList<LogContainer>();
		LogContainer container = new LogContainer();
		containers.add(container);
		return containers;
	}

	@Override
	public void deleteAllUserLogs(User user) {
		// do nothing function will be removed in the future
	}
}
