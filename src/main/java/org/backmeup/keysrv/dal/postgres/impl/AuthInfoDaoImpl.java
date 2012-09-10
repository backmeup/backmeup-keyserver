package org.backmeup.keysrv.dal.postgres.impl;

import java.io.IOException;
import java.util.ArrayList;

import org.backmeup.keyserver.dal.AuthInfoDao;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

public class AuthInfoDaoImpl implements AuthInfoDao
{
	private static final String PS_INSERT_AUTH_INFO = "INSERT INTO auth_infos (bmu_authinfo_id, user_id, service_id, ai_key, ai_value) VALUES" + "(?, ?, ?, (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))))";
	private static final String PS_SELECT_AUTH_INFO_BY_USER = "SELECT DISTINCT auth_infos.bmu_authinfo_id AS bmu_authinfo_id, auth_infos.service_id AS service_id, services.bmu_service_id AS bmu_service_id FROM auth_infos INNER JOIN services ON services.id=auth_infos.service_id WHERE auth_infos.user_id=? ORDER BY auth_infos.bmu_authinfo_id";
	private static final String PS_SELECT_AUTH_INFO_BY_BMU_AUTHINFO_ID = "SELECT id, bmu_authinfo_id, user_id, service_id, pgp_pub_decrypt_bytea (ai_key, dearmor (?)) AS ai_key, pgp_pub_decrypt_bytea (ai_value, dearmor (?)) AS ai_value FROM auth_infos WHERE bmu_authinfo_id=?";
	private static final String PS_DELETE_AUTH_INFO_BY_BMU_AUTHINFO_ID = "DELETE FROM auth_infos WHERE bmu_authinfo_id=?";
	
	private PGPKeys pgpkeys;

	public AuthInfoDaoImpl ()
	{
		try
		{
			pgpkeys = new PGPKeys ();
		}
		catch (IOException e)
		{
			// should not come up
			FileLogger.logException (e);
			e.printStackTrace ();
		}
	}

	@Override
	public void insertAuthInfo (AuthInfo authinfo)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public AuthInfo getAuthInfo (long bmu_authinfo_id, User user, Service service)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<AuthInfo> getUserAuthInfos (User user)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean existAuthInfo (long bmu_authinfo_id)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void deleteAuthInfo (long bmu_authinfo_id)
	{
		// TODO Auto-generated method stub
	}

}
