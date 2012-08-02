package org.backmeup.keysrv.worker;

public class User
{
	private long id = -1;
	private long bmu_id = -1;
	private String pwd = null;
	private String pwd_hash = null;
	
	public User (long bmu_id)
	{
		this.bmu_id = bmu_id;
	}
	
	public User (long id, long bmu_id)
	{
		this.id = id;
		this.bmu_id = bmu_id;
	}
	
	public long getId ()
	{
		return id;
	}
	
	public long getBmuId ()
	{
		return bmu_id;
	}
	
	public void setPwd (String pwd)
	{
		this.pwd = pwd;
		this.generateHash ();
	}
	
	public String getPwd ()
	{
		return pwd;
	}

	public String getPwd_hash ()
	{
		return pwd_hash;
	}

	public void setPwd_hash (String pwd_hash)
	{
		this.pwd_hash = pwd_hash;
	}
	
	private void generateHash ()
	{
		HashGenerator hasher = new HashGenerator ();
		this.pwd_hash = hasher.calcSaltedSHA512 (this.pwd);
	}
	
	public boolean validatePwd (String pwd)
	{
		HashGenerator hasher = new HashGenerator ();
		return hasher.isCorrectValue (pwd, this.pwd_hash);
	}
}
