/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
// $codepro.audit.disable declaredExceptions
// $codepro.audit.disable unnecessaryExceptions
// $codepro.audit.disable staticFieldNamingConvention

package com.aptana.webserver.core;

import org.eclipse.core.internal.resources.DelayedSnapshotJob;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.aptana.core.io.efs.EFSUtils;
import com.aptana.webserver.internal.core.ServerManager;
import com.aptana.webserver.internal.core.builtin.LocalWebServer;

/**
 * @author Max Stepanov
 */
@SuppressWarnings("restriction")
public class WebServerCorePlugin extends Plugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.aptana.webserver.core"; //$NON-NLS-1$

	// The shared instance
	private static WebServerCorePlugin plugin;

	private ServerManager serverManager;
	private LocalWebServer defaultWebServer;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext )
	 */
	@SuppressWarnings("deprecation")
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
		ISavedState lastState = ResourcesPlugin.getWorkspace().addSaveParticipant(this, new WorkspaceSaveParticipant());
		if (lastState != null)
		{
			IPath location = lastState.lookup(new Path(ServerManager.STATE_FILENAME));
			if (location != null)
			{
				((ServerManager) getServerManager()).loadState(getStateLocation().append(location));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext )
	 */
	@SuppressWarnings("deprecation")
	public void stop(BundleContext context) throws Exception
	{
		ResourcesPlugin.getWorkspace().removeSaveParticipant(this);
		plugin = null;
		serverManager = null;
		if (defaultWebServer != null)
		{
			defaultWebServer.dispose();
		}
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static WebServerCorePlugin getDefault()
	{
		return plugin;
	}

	public static void log(Throwable e)
	{
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e));
	}

	public static void log(String msg)
	{
		log(new Status(IStatus.INFO, PLUGIN_ID, IStatus.OK, msg, null));
	}

	public static void log(String msg, Throwable e)
	{
		log(new Status(IStatus.INFO, PLUGIN_ID, IStatus.OK, msg, e));
	}

	public static void log(IStatus status)
	{
		getDefault().getLog().log(status);
	}

	/**
	 * Get instance of server configuration manager
	 * 
	 * @return
	 */
	public synchronized IServerManager getServerManager()
	{
		if (serverManager == null)
		{
			serverManager = new ServerManager();
		}
		return serverManager;
	}

	/**
	 * Save state of server configurations
	 */
	public void saveServerConfigurations()
	{
		new DelayedSnapshotJob(((Workspace) ResourcesPlugin.getWorkspace()).getSaveManager()).schedule();
	}

	public IServer getDefaultWebServerConfiguration()
	{
		ensureDefaultWebServer();
		if (defaultWebServer != null)
		{
			return defaultWebServer.getConfiguration();
		}
		return null;
	}

	public synchronized void ensureDefaultWebServer()
	{
		if (defaultWebServer == null)
		{
			try
			{
				defaultWebServer = new LocalWebServer(EFSUtils.getFileStore(ResourcesPlugin.getWorkspace().getRoot())
						.toURI());
			}
			catch (CoreException e)
			{
				log(e);
			}
		}
	}

	private class WorkspaceSaveParticipant implements ISaveParticipant
	{

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse .core.resources.ISaveContext)
		 */
		public void doneSaving(ISaveContext context)
		{
			IPath prevSavePath = new Path(ServerManager.STATE_FILENAME).addFileExtension(Integer.toString(context
					.getPreviousSaveNumber()));
			getStateLocation().append(prevSavePath).toFile().delete();
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse .core.resources.ISaveContext)
		 */
		public void prepareToSave(ISaveContext context) throws CoreException
		{
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse. core.resources.ISaveContext)
		 */
		public void rollback(ISaveContext context)
		{
			IPath savePath = new Path(ServerManager.STATE_FILENAME).addFileExtension(Integer.toString(context
					.getSaveNumber()));
			getStateLocation().append(savePath).toFile().delete();
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core .resources.ISaveContext)
		 */
		public void saving(ISaveContext context) throws CoreException
		{
			IPath savePath = new Path(ServerManager.STATE_FILENAME).addFileExtension(Integer.toString(context
					.getSaveNumber()));
			((ServerManager) getServerManager()).saveState(getStateLocation().append(savePath));
			context.map(new Path(ServerManager.STATE_FILENAME), savePath);
			context.needSaveNumber();
		}
	}

}
