/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.usage.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.EclipseUtil;
import com.aptana.core.util.IConfigurationElementProcessor;
import com.aptana.core.util.StringUtil;
import com.aptana.usage.IAnalyticsUserManager;
import com.aptana.usage.UsagePlugin;

public class AnalyticsInfoManager
{

	private static final String EXTENSION_POINT_ID = "analyticsInfo"; //$NON-NLS-1$
	private static final String ELEMENT_ANALYTICS = "analytics"; //$NON-NLS-1$
	private static final String ELEMENT_INFO = "info"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String ATTR_INFO = "info"; //$NON-NLS-1$
	private static final String ATTR_OVERRIDES = "overrides"; //$NON-NLS-1$
	private static final String ATTR_APP_ID = "appId"; //$NON-NLS-1$
	private static final String ATTR_APP_NAME = "appName"; //$NON-NLS-1$
	private static final String ATTR_APP_GUID = "appGuid"; //$NON-NLS-1$
	private static final String ATTR_VERSION_PLUGIN_ID = "versionPluginId"; //$NON-NLS-1$
	private static final String ATTR_USER_AGENT = "userAgent"; //$NON-NLS-1$
	private static final String ATTR_USER_MANAGER = "userManager"; //$NON-NLS-1$

	private static AnalyticsInfoManager instance;

	private Map<String, AnalyticsInfo> analyticsInfoMap;
	private Map<String, Analytics> analyticsMap;

	public synchronized static AnalyticsInfoManager getInstance()
	{
		if (instance == null)
		{
			instance = new AnalyticsInfoManager();
		}
		return instance;
	}

	public AnalyticsInfo getInfo(String id)
	{
		Analytics analytics = analyticsMap.get(id);
		return (analytics == null) ? null : analytics.info;
	}

	private AnalyticsInfoManager()
	{
		analyticsInfoMap = new HashMap<String, AnalyticsInfo>();
		analyticsMap = new HashMap<String, Analytics>();
		loadExtension();
	}

	private void loadExtension()
	{
		EclipseUtil.processConfigurationElements(UsagePlugin.PLUGIN_ID, EXTENSION_POINT_ID,
				new IConfigurationElementProcessor()
				{

					public void processElement(IConfigurationElement element)
					{
						String name = element.getName();
						if (ELEMENT_INFO.equals(name))
						{
							String appId = element.getAttribute(ATTR_APP_ID);
							if (StringUtil.isEmpty(appId))
							{
								return;
							}
							String appName = element.getAttribute(ATTR_APP_NAME);
							if (StringUtil.isEmpty(appName))
							{
								return;
							}
							String appGuid = element.getAttribute(ATTR_APP_GUID);
							if (StringUtil.isEmpty(appGuid))
							{
								return;
							}
							String versionPluginId = element.getAttribute(ATTR_VERSION_PLUGIN_ID);
							if (StringUtil.isEmpty(versionPluginId))
							{
								return;
							}
							String userAgent = element.getAttribute(ATTR_USER_AGENT);
							if (StringUtil.isEmpty(userAgent))
							{
								return;
							}
							IAnalyticsUserManager userManager = null;
							String userManagerClass = element.getAttribute(ATTR_USER_MANAGER);
							if (!StringUtil.isEmpty(userManagerClass))
							{
								try
								{
									Object clazz = element.createExecutableExtension(ATTR_USER_MANAGER);
									if (clazz instanceof IAnalyticsUserManager)
									{
										userManager = (IAnalyticsUserManager) clazz;
									}
								}
								catch (CoreException e)
								{
									IdeLog.logError(UsagePlugin.getDefault(), e);
								}
							}
							analyticsInfoMap.put(appId, new AnalyticsInfo(appId, appName, appGuid, versionPluginId,
									userAgent, userManager));
						}
						else if (ELEMENT_ANALYTICS.equals(name))
						{
							String id = element.getAttribute(ATTR_ID);
							if (StringUtil.isEmpty(id))
							{
								return;
							}
							String infoId = element.getAttribute(ATTR_INFO);
							if (StringUtil.isEmpty(infoId))
							{
								return;
							}
							AnalyticsInfo info = analyticsInfoMap.get(infoId);
							if (info == null)
							{
								return;
							}
							String overridesId = element.getAttribute(ATTR_OVERRIDES);
							analyticsMap.put(id, new Analytics(info, overridesId));
						}
					}
				}, ELEMENT_INFO, ELEMENT_ANALYTICS);

		Set<String> keys = analyticsMap.keySet();
		Analytics analytics;
		for (String key : keys)
		{
			analytics = analyticsMap.get(key);
			if (analytics.overridesId != null && analyticsMap.containsKey(analytics.overridesId))
			{
				// replaces the overridden analytics info
				analyticsMap.put(analytics.overridesId, analytics);
			}
		}
	}

	private static class Analytics
	{

		public AnalyticsInfo info;
		public String overridesId;

		private Analytics(AnalyticsInfo info, String overridesId)
		{
			this.info = info;
			this.overridesId = overridesId;
		}
	}
}
