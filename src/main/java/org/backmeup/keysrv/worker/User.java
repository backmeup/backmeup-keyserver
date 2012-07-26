package org.backmeup.keysrv.worker;

public class User
{
	private long id = -1;
	private long bmu_id = -1;
	private String pwd = null;
	
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
	}
	
	public String getPwd ()
	{
		return pwd;
	}
}
