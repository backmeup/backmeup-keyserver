package org.backmeup.keysrv;

public class Service
{
	private long id = -1;
	private String name = null;
	
	public Service (String name)
	{
		this.name = name;
	}
	
	public Service (long id, String name)
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
}