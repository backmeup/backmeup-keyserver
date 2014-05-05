package org.backmeup.keysrv.dal.postgres.impl;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.backmeup.keyserver.dal.AuthInfoDao;
import org.backmeup.keysrv.rest.exceptions.RestAuthInfoAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestAuthInfoNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

public class AuthInfoDaoImpl implements AuthInfoDao {
	private static final String PS_INSERT_AUTH_INFO = "INSERT INTO auth_infos (bmu_authinfo_id, user_id, service_id, ai_key, ai_value) VALUES"
			+ "(?, ?, ?, (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))))";
	private static final String PS_SELECT_AUTH_INFO_BY_USER = "SELECT DISTINCT auth_infos.bmu_authinfo_id AS bmu_authinfo_id, auth_infos.service_id AS service_id, services.bmu_service_id AS bmu_service_id FROM auth_infos INNER JOIN services ON services.id=auth_infos.service_id WHERE auth_infos.user_id=? ORDER BY auth_infos.bmu_authinfo_id";
	private static final String PS_SELECT_AUTH_INFO_BY_BMU_AUTHINFO_ID = "SELECT id, bmu_authinfo_id, user_id, service_id, pgp_pub_decrypt_bytea (ai_key, dearmor (?)) AS ai_key, pgp_pub_decrypt_bytea (ai_value, dearmor (?)) AS ai_value FROM auth_infos WHERE bmu_authinfo_id=?";
	private static final String PS_DELETE_AUTH_INFO_BY_BMU_AUTHINFO_ID = "DELETE FROM auth_infos WHERE bmu_authinfo_id=?";
	private static final String PS_SELECT_USER_SERVICE_BY_BMU_AUTHINFO_ID = "SELECT users.bmu_user_id AS bmu_user_id, services.bmu_service_id AS bmu_service_id FROM auth_infos INNER JOIN users ON auth_infos.user_id=users.id INNER JOIN services ON auth_infos.service_id=services.id WHERE auth_infos.bmu_authinfo_id=? LIMIT 1";

	private static final String COLUMN_SERVICE_ID = "bmu_service_id";
	private static final String COLUMN_USER_ID = "bmu_user_id";
	private static final String COLUMN_AUTHINFO_ID = "bmu_authinfo_id";
	
	private PGPKeys pgpkeys;

	public AuthInfoDaoImpl() {
		try {
			pgpkeys = new PGPKeys();
		} catch (IOException e) {
			// should not come up
			FileLogger.logException("Loading pgp keys failed", e);
		}
	}

	@Override
	public void insertAuthInfo(AuthInfo authinfo) {
		try {
			this.getAuthInfo(authinfo.getBmuAuthinfoId(), authinfo.getUser(),
					authinfo.getService());
			throw new RestAuthInfoAlreadyExistException(
					authinfo.getBmuAuthinfoId());
		} catch (RestAuthInfoNotFoundException e) {
			// if this happens the user does not exist and can be inserted
		}

		PreparedStatement ps = null;

		try {
			ps = Connection.getPreparedStatement(PS_INSERT_AUTH_INFO);

			ps.setLong(1, authinfo.getBmuAuthinfoId());
			ps.setLong(2, authinfo.getUser().getId());
			ps.setLong(3, authinfo.getService().getId());

			for (byte[] key : authinfo.getAiData().keySet()) {
				ps.setBytes(4, key);
				ps.setString(5, pgpkeys.getPublickey());

				ps.setBytes(6, authinfo.getAiData().get(key));
				ps.setString(7, pgpkeys.getPublickey());

				ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(ps);
		}
	}

	@Override
	public AuthInfo getAuthInfo(long bmuAuthinfoId, User user, Service service) {
		AuthInfo ai = null;
		Map<byte[], byte[]> aiData = new HashMap<byte[], byte[]>();
		long id = -1;

		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = Connection
					.getPreparedStatement(PS_SELECT_AUTH_INFO_BY_BMU_AUTHINFO_ID);

			ps.setString(1, pgpkeys.getPrivatekey());
			ps.setString(2, pgpkeys.getPrivatekey());
			ps.setLong(3, bmuAuthinfoId);

			rs = ps.executeQuery();

			while (rs.next()) {
				aiData.put(rs.getBytes("ai_key"), rs.getBytes("ai_value"));

				if (id < 0) {
					id = rs.getLong("id");
				}
			}

			ai = new AuthInfo(id, bmuAuthinfoId, user, service, aiData);
		} catch (SQLException e) {
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(rs);
			Connection.closeQuiet(ps);
		}

		if (ai.getAiData().isEmpty()) {
			throw new RestAuthInfoNotFoundException(bmuAuthinfoId);
		}

		return ai;
	}

	@Override
	public AuthInfo getAuthInfo(long bmuAuthinfoId) {
		AuthInfo ai = null;

		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = Connection
					.getPreparedStatement(PS_SELECT_USER_SERVICE_BY_BMU_AUTHINFO_ID);

			ps.setLong(1, bmuAuthinfoId);

			rs = ps.executeQuery();

			User user = null;
			Service service = null;
			if (rs.next()) {
				long bmuUserId = rs.getLong(COLUMN_USER_ID);
				long bmuServiceId = rs.getLong(COLUMN_SERVICE_ID);

				user = new User(bmuUserId);
				service = new Service(bmuServiceId);

				ai = new AuthInfo(bmuAuthinfoId, user, service);
			}
		} catch (SQLException e) {
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(rs);
			Connection.closeQuiet(ps);
		}

		if (ai == null) {
			throw new RestAuthInfoNotFoundException(bmuAuthinfoId);
		}

		return ai;
	}

	@Override
	public List<AuthInfo> getUserAuthInfos(User user) {
		List<AuthInfo> authinfos = new ArrayList<AuthInfo>();

		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = Connection.getPreparedStatement(PS_SELECT_AUTH_INFO_BY_USER);

			ps.setLong(1, user.getId());

			rs = ps.executeQuery();

			while (rs.next()) {
				Service service = new Service(rs.getLong("service_id"),
						rs.getLong(COLUMN_SERVICE_ID));
				authinfos.add(this.getAuthInfo(rs.getLong(COLUMN_AUTHINFO_ID),
						user, service));
			}
		} catch (SQLException e) {
			FileLogger.logException("get user auth infos failed", e);
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(rs);
			Connection.closeQuiet(ps);
		}

		return authinfos;
	}

	@Override
	public boolean existAuthInfo(long bmuAuthinfoId) {
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = Connection
					.getPreparedStatement(PS_SELECT_AUTH_INFO_BY_BMU_AUTHINFO_ID);

			ps.setString(1, pgpkeys.getPrivatekey());
			ps.setString(2, pgpkeys.getPrivatekey());
			ps.setLong(3, bmuAuthinfoId);

			rs = ps.executeQuery();

			return rs.next();
			
		} catch (SQLException e) {
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(rs);
			Connection.closeQuiet(ps);
		}
	}

	@Override
	public void deleteAuthInfo(long bmuAuthinfoId) {
		if (!this.existAuthInfo(bmuAuthinfoId)) {
			throw new RestAuthInfoNotFoundException(bmuAuthinfoId);
		}

		PreparedStatement ps = null;

		try {
			ps = Connection
					.getPreparedStatement(PS_DELETE_AUTH_INFO_BY_BMU_AUTHINFO_ID);

			ps.setLong(1, bmuAuthinfoId);

			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(ps);
		}
	}
}
