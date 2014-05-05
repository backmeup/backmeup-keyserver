package org.backmeup.keysrv.dal.postgres.impl;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.backmeup.keyserver.dal.UserDao;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.rest.exceptions.RestUserAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestUserNotFoundException;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.User;

public class UserDaoImpl implements UserDao {
	private static final String PS_INSERT_USER = "INSERT INTO users (bmu_user_id, bmu_user_pwd_hash) VALUES (?, (pgp_pub_encrypt (?, dearmor(?))))";
	private static final String PS_UPDATE_USER = "UPDATE users SET bmu_user_pwd_hash=(pgp_pub_encrypt (?, dearmor(?))) WHERE bmu_user_id=?";
	private static final String PS_SELECT_USER_BY_BMU_USER_ID = "SELECT id, bmu_user_id, pgp_pub_decrypt (bmu_user_pwd_hash, dearmor (?)) AS bmu_user_pwd_hash FROM users WHERE bmu_user_id=?";
	private static final String PS_DELETE_USER_BY_BMU_USER_ID = "DELETE FROM users WHERE bmu_user_id=?";

	private PGPKeys pgpkeys;

	public UserDaoImpl() {
		try {
			pgpkeys = new PGPKeys();
		} catch (IOException e) {
			// should not come up
			FileLogger.logException("failed to load pgp keys", e);
		}
	}

	@Override
	public void insertUser(User user) {
		try {
			this.getUser(user.getBmuId());
			throw new RestUserAlreadyExistException(user.getBmuId());
		} catch (RestUserNotFoundException e) {
		}

		PreparedStatement ps = null;

		try {
			ps = Connection.getPreparedStatement(PS_INSERT_USER);

			ps.setLong(1, user.getBmuId());
			ps.setString(2, user.getPwdHash());
			ps.setString(3, pgpkeys.getPublickey());

			ps.executeUpdate();
		} catch (SQLException e) {
			FileLogger.logException("failed to insert user to db", e);
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(ps);
		}
	}

	@Override
	public User getUser(long bmuUserId) {
		User user = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = Connection.getPreparedStatement(PS_SELECT_USER_BY_BMU_USER_ID);

			ps.setString(1, pgpkeys.getPrivatekey());
			ps.setLong(2, bmuUserId);

			rs = ps.executeQuery();
			if (rs.next()) {
				user = new User(rs.getLong("id"), rs.getLong("bmu_user_id"));
				user.setPwdHash(rs.getString("bmu_user_pwd_hash"));
			}
		} catch (SQLException e) {
			FileLogger.logException("failed to get user from db", e);
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(rs);
			Connection.closeQuiet(ps);
		}

		if (user == null) {
			throw new RestUserNotFoundException(bmuUserId);
		}

		return user;
	}

	@Override
	public void changeUser(User user) {
		PreparedStatement ps = null;

		try {
			ps = Connection.getPreparedStatement(PS_UPDATE_USER);

			ps.setString(1, user.getPwdHash());
			ps.setString(2, pgpkeys.getPublickey());
			ps.setLong(3, user.getBmuId());

			ps.executeUpdate();
		} catch (SQLException e) {
			FileLogger.logException("failed to modify user in db", e);
		} finally {
			org.backmeup.keysrv.dal.postgres.impl.Connection.closeQuiet(ps);
		}
	}

	@Override
	public void deleteUser(User user) {
		this.getUser(user.getBmuId());

		PreparedStatement ps = null;

		try {
			ps = Connection.getPreparedStatement(PS_DELETE_USER_BY_BMU_USER_ID);

			ps.setLong(1, user.getBmuId());
			ps.executeUpdate();
		} catch (SQLException e) {
			FileLogger.logException("failed to delete user from db", e);
			throw new RestSQLException(e);
		} finally {
			Connection.closeQuiet(ps);
		}
	}

}
