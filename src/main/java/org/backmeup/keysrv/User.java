package org.backmeup.keysrv;

public class User
{
	private long id = -1;
	private String name = null;
	private String pwd = null;
	
	public User (String name)
	{
		this.name = name;
	}
	
	public User (long id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	public long getId ()
	{
		return id;
	}
	
	public String getName ()
	{
		return name;
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
