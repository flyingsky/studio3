/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.scripting.model;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{
	public static Test suite()
	{
		TestSuite suite = new TestSuite("Test for com.aptana.scripting.model");
		// $JUnit-BEGIN$
		suite.addTestSuite(BundleCacherTest.class);
		// suite.addTestSuite(BundleLoadingPerformanceTest.class);
		suite.addTestSuite(BundleTests.class);
		suite.addTestSuite(BundleVisibilityTests.class);
		suite.addTestSuite(CommandBlockRunnerTests.class);
		suite.addTestSuite(CommandTests.class);
		suite.addTestSuite(ContextTests.class);
		suite.addTestSuite(CommandBlockRunnerTests.class);
		suite.addTestSuite(FilterTests.class);
		suite.addTestSuite(KeyBindingTests.class);
		suite.addTestSuite(PlatformSpecificCommandTests.class);
		suite.addTestSuite(WithDefaultsTests.class);

		// TODO: uncomment once timing issues are resolved. We're still getting
		// Intermittent failures
		// suite.addTestSuite(ProjectBundleMonitorTests.class);
		// suite.addTestSuite(UserBundleMonitorTests.class);
		// $JUnit-END$
		return suite;
	}
}
