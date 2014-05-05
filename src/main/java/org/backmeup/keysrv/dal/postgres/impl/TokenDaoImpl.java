package org.backmeup.keysrv.dal.postgres.impl;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.backmeup.keyserver.dal.TokenDao;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.rest.exceptions.RestTokenNotFoundException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.CipherGenerator;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.User;

public class TokenDaoImpl implements TokenDao {
	private static final String PS_INSERT_TOKEN = "INSERT INTO tokens (token_id, user_id, service_id, bmu_authinfo_id, reusable, token_key, token_value, backupdate) VALUES (?, ?, ?, ?, ?, (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))))";
	private static final String PS_SELECT_TOKEN_BY_TOKEN_ID = "SELECT tokens.id AS id, users.id AS user_id, users.bmu_user_id AS bmu_user_id, services.id AS service_id, services.bmu_service_id AS bmu_service_id, tokens.bmu_authinfo_id AS bmu_authinfo_id, tokens.reusable AS reusable , pgp_pub_decrypt_bytea (token_key, dearmor (?)) AS token_key, pgp_pub_decrypt_bytea (token_value, dearmor (?)) AS token_value, pgp_pub_decrypt_bytea (backupdate, dearmor (?)) AS backupdate FROM tokens INNER JOIN users ON users.id=tokens.user_id INNER JOIN services ON services.id=tokens.service_id WHERE token_id=? ORDER BY tokens.bmu_authinfo_id";
	private static final String PS_DELETE_TOKEN_BY_TOKEN_ID = "DELETE FROM tokens WHERE token_id=?";
	private static final String PS_DELETE_TOKEN_BY_ID = "DELETE FROM tokens WHERE id=?";

	private static final String COLUMN_SERVICE_ID = "bmu_service_id";
	private static final String COLUMN_USER_ID = "bmu_user_id";
	private static final String COLUMN_AUTHINFO_ID = "bmu_authinfo_id";
	private static final String COLUMN_TOKEN_KEY = "token_key";
	private static final String COLUMN_TOKEN_VALUE = "token_value";
	
	private PGPKeys pgpkeys;

	public TokenDaoImpl() {
		try {
			pgpkeys = new PGPKeys();
		} catch (IOException e) {
			// should not come up
			FileLogger.logException("failed to load pgp keys", e);
		}
	}

	@Override
	public long insertToken(Token token) {
		long tokeId = -1;

		PreparedStatement psInsert = null;
		PreparedStatement psDelete = null;
		ResultSet rs = null;

		try {
			CipherGenerator cipher = new CipherGenerator();
			psInsert = Connection.getPreparedStatement(PS_INSERT_TOKEN,
					PreparedStatement.RETURN_GENERATED_KEYS);

			byte[] filler = {56, 57};
			
			// Create first entry to get an valid and unique id
			psInsert.setLong(1, -1);
			psInsert.setLong(2, token.getUser().getId());
			psInsert.setLong(3, token.getAuthInfo(0).getService().getId());
			psInsert.setLong(4, token.getAuthInfo(0).getBmuAuthinfoId());
			psInsert.setBoolean(5, token.isReusable());
			psInsert.setBytes(6, filler);
			psInsert.setString(7, pgpkeys.getPublickey());
			psInsert.setBytes(8, filler);
			psInsert.setString(9, pgpkeys.getPublickey());
			psInsert.setBytes(
					10,
					cipher.encData(token.getBackupdate().getTime() + "",
							token.getTokenpwd()));
			psInsert.setString(11, pgpkeys.getPublickey());

			psInsert.executeUpdate();
			rs = psInsert.getGeneratedKeys();

			if (rs.next()) {
				tokeId = rs.getLong(1);
			}

			psInsert.setLong(1, tokeId);

			User tokenuser = new User(-1);
			tokenuser.setPwd(token.getTokenpwd());

			for (int i = 0; i < token.getAuthInfoCount(); i++) {
				Map<String, String> aiData = token.getAuthInfo(i)
						.getDecAiData();
				Map<byte[], byte[]> aiEncData = cipher.encData(aiData,
						tokenuser);

				for (Map.Entry<byte[], byte[]> entry : aiEncData.entrySet()) {
					psInsert.setLong(3, token.getAuthInfo(i).getService()
							.getId());
					psInsert.setLong(4, token.getAuthInfo(i)
							.getBmuAuthinfoId());
					psInsert.setBytes(6, entry.getKey());
					psInsert.setBytes(8, entry.getValue());
					psInsert.executeUpdate();
				}
			}

			// remove the extra line from database
			psDelete = Connection.getPreparedStatement(PS_DELETE_TOKEN_BY_ID);
			psDelete.setLong(1, tokeId);
			psDelete.executeUpdate();
		} catch (SQLException e) {
			FileLogger.logException("failed to insert token to db", e);
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(rs);
			Connection.closeQuiet(psInsert);
			Connection.closeQuiet(psDelete);
		}

		return tokeId;
	}

	@Override
	public Token getTokenData(long tokenId, String tokenPwd) {
		Token token = null;

		PreparedStatement psSelect = null;
		PreparedStatement psDelete = null;

		try {
			psSelect = Connection
					.getPreparedStatement(PS_SELECT_TOKEN_BY_TOKEN_ID);

			psSelect.setString(1, pgpkeys.getPrivatekey());
			psSelect.setString(2, pgpkeys.getPrivatekey());
			psSelect.setString(3, pgpkeys.getPrivatekey());
			psSelect.setLong(4, tokenId);

			ResultSet rs = psSelect.executeQuery();

			CipherGenerator cipher = new CipherGenerator();

			User user = null;
			AuthInfo ai = null;
			Map<byte[], byte[]> aiData = null;
			while (rs.next()) {
				if (token == null) {
					user = new User(rs.getLong("user_id"),
							rs.getLong(COLUMN_USER_ID));
					user.setPwd(tokenPwd);

					String strBackupdate = "";
					strBackupdate = cipher.decData(rs.getBytes("backupdate"),
							user);
					Date backupdate = new Date(Long.valueOf(strBackupdate));

					token = new Token(user, backupdate,
							rs.getBoolean("reusable"));
					token.setTokenpwd(tokenPwd);
				}

				Service service = new Service(rs.getLong("service_id"),
						rs.getLong(COLUMN_SERVICE_ID));

				if (ai == null) {
					ai = new AuthInfo(rs.getLong(COLUMN_AUTHINFO_ID), user,
							service);
					aiData = new HashMap<byte[], byte[]>();
				}
				if (ai.getBmuAuthinfoId() != rs.getLong(COLUMN_AUTHINFO_ID)) {
					ai.setAiData(aiData);
					token.addAuthInfo(ai);

					ai = new AuthInfo(rs.getLong(COLUMN_AUTHINFO_ID), user,
							service);
					aiData = new HashMap<byte[], byte[]>();

					aiData.put(rs.getBytes(COLUMN_TOKEN_KEY),
							rs.getBytes(COLUMN_TOKEN_VALUE));
				} else {
					aiData.put(rs.getBytes(COLUMN_TOKEN_KEY),
							rs.getBytes(COLUMN_TOKEN_VALUE));
				}
			}

			if (aiData == null) {
				throw new RestTokenNotFoundException(tokenId);
			}

			ai.setAiData(aiData);
			token.addAuthInfo(ai);

			rs.close();

			psDelete = Connection
					.getPreparedStatement(PS_DELETE_TOKEN_BY_TOKEN_ID);
			psDelete.setLong(1, tokenId);
			psDelete.executeUpdate();

		} catch (SQLException e) {
			FileLogger.logException("failed to get token from db", e);
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(psSelect);
			Connection.closeQuiet(psDelete);
		}

		token.setId(tokenId);
		return token;
	}

}
