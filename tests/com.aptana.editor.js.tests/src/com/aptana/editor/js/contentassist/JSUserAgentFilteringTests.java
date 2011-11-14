/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;

import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.contentassist.IPreferenceConstants;
import com.aptana.editor.common.contentassist.UserAgentFilterType;
import com.aptana.editor.js.contentassist.index.SDocMLFileIndexingParticipant;
import com.aptana.editor.js.tests.JSEditorBasedTests;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;

/**
 * JSUserAgentFiltering
 */
public class JSUserAgentFilteringTests extends JSEditorBasedTests
{
	class Indexer extends SDocMLFileIndexingParticipant
	{
		public void index(Index index, IFileStore file)
		{
			indexFileStore(index, file, new NullProgressMonitor());
		}
	}

	private static final String NO_USER_AGENTS = StringUtil.EMPTY;
	private static final String ONE_USER_AGENT = "IE";
	private static final String TWO_USER_AGENTS = "IE,Safari";
	private static final String THREE_USER_AGENTS = "IE,Safari,Firefox";

	private IPreferenceStore preferences;

	protected void indexAndCheckProposals(String preference, String indexResource, String fileResource,
			String... proposals)
	{
		URI uri = null;

		// set active user agents
		setActiveUserAgents(preference);

		try
		{
			// create IFileStore for indexing
			IFileStore indexFile = getFileStore(indexResource);

			// grab source file URI
			IFileStore sourceFile = getFileStore(fileResource);
			uri = sourceFile.toURI();

			// create index for file
			Index index = IndexManager.getInstance().getIndex(uri);
			Indexer indexer = new Indexer();

			// index file
			indexer.index(index, indexFile);

			// check proposals
			checkProposals(fileResource, proposals);
		}
		catch (Throwable t)
		{
			fail(t.getMessage());
		}
		finally
		{
			if (uri != null)
			{
				IndexManager.getInstance().removeIndex(uri);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		preferences = CommonEditorPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * setActiveUserAgents
	 * 
	 * @param ids
	 */
	protected void setActiveUserAgents(String ids)
	{
		preferences.setValue(IPreferenceConstants.USER_AGENT_PREFERENCE, ids);
	}

	public void testNoFilterNoUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.NO_FILTER.getText());

		// @formatter:off
		indexAndCheckProposals(
			NO_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"OneUserAgentOutlier",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testNoFilterOneUserAgentActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.NO_FILTER.getText());

		// @formatter:off
		indexAndCheckProposals(
			ONE_USER_AGENT,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"OneUserAgentOutlier",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testNoFilterTwoUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.NO_FILTER.getText());

		// @formatter:off
		indexAndCheckProposals(
			TWO_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"OneUserAgentOutlier",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testNoFilterThreeUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.NO_FILTER.getText());

		// @formatter:off
		indexAndCheckProposals(
			THREE_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"OneUserAgentOutlier",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testSomeFilterNoUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ONE_OR_MORE.getText());

		// @formatter:off
		indexAndCheckProposals(
			NO_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"OneUserAgentOutlier",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testSomeFilterOneUserAgentActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ONE_OR_MORE.getText());

		// @formatter:off
		indexAndCheckProposals(
			ONE_USER_AGENT,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testSomeFilterTwoUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ONE_OR_MORE.getText());

		// @formatter:off
		indexAndCheckProposals(
			TWO_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testSomeFilterThreeUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ONE_OR_MORE.getText());

		// @formatter:off
		indexAndCheckProposals(
			THREE_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testAllFilterNoUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ALL.getText());

		// @formatter:off
		indexAndCheckProposals(
			NO_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"OneUserAgentOutlier",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testAllFilterOneUserAgentActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ALL.getText());

		// @formatter:off
		indexAndCheckProposals(
			ONE_USER_AGENT,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"OneUserAgent",
			"TwoUserAgents",
			"TwoUserAgentsOutlier",
			"ThreeUserAgents",
			"ThreeUserAgentsOutlier",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testAllFilterTwoUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ALL.getText());

		// @formatter:off
		indexAndCheckProposals(
			TWO_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"TwoUserAgents",
			"ThreeUserAgents",
			"FourUserAgents",
			"FourUserAgentsOutlier"
		);
		// @formatter:on
	}

	public void testAllFilterThreeUserAgentsActive()
	{
		// set filter preference
		preferences.setValue(IPreferenceConstants.CONTENT_ASSIST_USER_AGENT_FILTER_TYPE,
				UserAgentFilterType.ALL.getText());

		// @formatter:off
		indexAndCheckProposals(
			THREE_USER_AGENTS,
			"sdocml/userAgents.sdocml",
			"contentAssist/empty.js",
			"NoUserAgents",
			"ThreeUserAgents",
			"FourUserAgents"
		);
		// @formatter:on
	}
}
