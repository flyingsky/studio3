package com.aptana.core.io.tests;

import java.io.File;
import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;

import com.aptana.core.util.ResourceUtil;
import com.aptana.ide.core.io.ConnectionPoint;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.io.IConnectionPointManager;

public class ConnectionPointManagerTest extends TestCase
{

	private static final String BUNDLE_ID = "com.aptana.core.io.tests";
	private static final String RESOURCE_DIR = "resources";

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();

		IConnectionPointManager connectionPointManager = CoreIOPlugin.getConnectionPointManager();
		IConnectionPoint[] connectionPoints = connectionPointManager.getConnectionPoints();
		for (IConnectionPoint point : connectionPoints)
			connectionPointManager.removeConnectionPoint(point);
	}

	@Test
	public void testImportConnections()
	{
		URL resourceURL = Platform.getBundle(BUNDLE_ID).getEntry(RESOURCE_DIR);
		File resourceFolder = ResourceUtil.resourcePathToFile(resourceURL);
		File testInputFile = new File(resourceFolder, "testConnections.xml");
		List<IConnectionPoint> newConnections = CoreIOPlugin.getConnectionPointManager().addConnectionsFrom(
				Path.fromOSString(testInputFile.getAbsolutePath()));
		assertEquals(5, newConnections.size());
		assertEquals(5, CoreIOPlugin.getConnectionPointManager().getConnectionPoints().length);

		for (IConnectionPoint point : newConnections)
		{
			if (point.getName().equals("FTPS1"))
			{
				assertEquals("ftps", ((ConnectionPoint) point).getType());
				assertEquals("a1df2047-0bf0-4642-9f06-ed89e11feca5", point.getId());
			}
			else if (point.getName().equals("FTP1"))
			{
				assertEquals("ftp", ((ConnectionPoint) point).getType());
				assertEquals("8da399fa-bf55-4e16-b562-193db924e063", point.getId());
			}
			else if (point.getName().equals("SFTP1"))
			{
				assertEquals("sftp", ((ConnectionPoint) point).getType());
				assertEquals("89deecbf-0c19-44e4-b2c6-549b8d0a12e6", point.getId());
			}
			else if (point.getName().equals("S3"))
			{
				assertEquals("s3", ((ConnectionPoint) point).getType());
				assertEquals("ab96e9dc-b38b-47bf-9afd-fd753f73d4b3", point.getId());
			}
			else if (point.getName().equals("Sample"))
			{
				assertEquals("local", ((ConnectionPoint) point).getType());
				assertEquals("5acceb19-03f7-4f79-bf06-e7fbafff2364", point.getId());
			}
			else
			{
				fail("Imported unknown connection");
			}
		}
	}

	public void testEmptyConnections()
	{
		URL resourceURL = Platform.getBundle(BUNDLE_ID).getEntry(RESOURCE_DIR);
		File resourceFolder = ResourceUtil.resourcePathToFile(resourceURL);
		File testInputFile = new File(resourceFolder, "emptyConnections.xml");

		List<IConnectionPoint> newConnections = CoreIOPlugin.getConnectionPointManager().addConnectionsFrom(
				Path.fromOSString(testInputFile.getAbsolutePath()));
		assertEquals(0, newConnections.size());

		IConnectionPoint[] connectionPoints = CoreIOPlugin.getConnectionPointManager().getConnectionPoints();
		assertEquals(0, connectionPoints.length);
	}
}
