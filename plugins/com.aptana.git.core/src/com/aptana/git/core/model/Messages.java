/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.git.core.model;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS // NO_UCD
{
	private static final String BUNDLE_NAME = "com.aptana.git.core.model.messages"; //$NON-NLS-1$
	
	public static String GitExecutable_UnableToParseGitVersion;

	public static String GitIndex_BinaryDiff_Message; // NO_UCD
	public static String GitRepositoryManager_UnableToFindGitExecutableError; // NO_UCD
	public static String GitExecutable_UnableToLaunchCloneError;

	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}
