/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
// $codepro.audit.disable unnecessaryExceptions

package com.aptana.webserver.core;

import java.net.URI;
import java.net.URL;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

import com.aptana.core.IURIMapper;
import com.aptana.core.Identifiable;
import com.aptana.core.epl.IMemento;
import com.aptana.webserver.internal.core.ServerManager;
import com.aptana.webserver.internal.core.ServerType;

/**
 * @author Max Stepanov
 */
abstract class AbstractWebServerConfiguration implements IExecutableExtension, Identifiable, IURIMapper, IServer
{

	protected static final String ELEMENT_NAME = "name"; //$NON-NLS-1$

	private IServerType type;
	private String name;

	/**
	 * 
	 */
	protected AbstractWebServerConfiguration()
	{
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.webserver.core.IURLMapper#resolve(org.eclipse.core.filesystem.IFileStore)
	 */
	public abstract URI resolve(IFileStore file);

	/*
	 * (non-Javadoc)
	 * @see com.aptana.webserver.core.IURLMapper#resolve(java.net.URL)
	 */
	public abstract IFileStore resolve(URI uri);

	public abstract URL getBaseURL();

	public void loadState(IMemento memento)
	{
		IMemento child = memento.getChild(ELEMENT_NAME);
		if (child != null)
		{
			name = child.getTextData();
		}
	}

	public void saveState(IMemento memento)
	{
		memento.createChild(ELEMENT_NAME).putTextData(name);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.webserver.core.Iserver#isPersistent()
	 */
	public boolean isPersistent()
	{
		return true;
	}

	/*
	 * @see
	 * org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement
	 * , java.lang.String, java.lang.Object)
	 */
	public final void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException
	{
		type = new ServerType(config.getAttribute(ServerManager.ATT_ID),
				config.getAttribute(ServerManager.ATT_NAME));
	}

	public final IServerType getType()
	{
		return type;
	}

	/*
	 * @see com.aptana.core.Identifiable#getId()
	 */
	/*
	 * (non-Javadoc)
	 * @see com.aptana.webserver.core.Iserver#getId()
	 */
	public final String getId()
	{
		return type.getId();
	}

	public final String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public final void setName(String name)
	{
		this.name = name;
	}
}
