/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.projects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.aptana.projects.templates.ProjectTemplatesManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class ProjectsPlugin extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.aptana.projects"; //$NON-NLS-1$

	// The shared instance
	private static ProjectsPlugin plugin;

	private ProjectTemplatesManager templatesManager;

	/**
	 * The constructor
	 */
	public ProjectsPlugin()
	{
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		if (templatesManager != null)
		{
			templatesManager.dispose();
			templatesManager = null;
		}
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static ProjectsPlugin getDefault()
	{
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String string)
	{
		if (getDefault().getImageRegistry().getDescriptor(string) == null)
		{
			ImageDescriptor id = imageDescriptorFromPlugin(PLUGIN_ID, string);
			if (id != null)
			{
				getDefault().getImageRegistry().put(string, id);
			}
		}
		return getDefault().getImageRegistry().getDescriptor(string);
	}

	public synchronized ProjectTemplatesManager getTemplatesManager()
	{
		if (templatesManager == null)
		{
			templatesManager = new ProjectTemplatesManager();
		}
		return templatesManager;
	}
}
