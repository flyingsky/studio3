/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.hyperlink;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.js.contentassist.index.JSFileIndexingParticipant;
import com.aptana.editor.js.tests.JSEditorBasedTests;
import com.aptana.index.core.IFileStoreIndexingParticipant;

/**
 * JSHyperlinkDetectorTests
 */
public class JSHyperlinkDetectorTests extends JSEditorBasedTests
{
	@Override
	protected IFileStoreIndexingParticipant createIndexer()
	{
		return new JSFileIndexingParticipant();
	}

	public void assertHyperlinks(String resource, JSHyperlink... expectedHyperlinks)
	{
		setupTestContext(resource);

		if (editor instanceof AbstractThemeableEditor)
		{
			AbstractThemeableEditor themeableEditor = (AbstractThemeableEditor) editor;

			// only supporting testing of one offset per file
			assertTrue(cursorOffsets.size() > 0);
			int offset = cursorOffsets.get(0);

			// grab the links for our offset
			JSHyperlinkDetector detector = new JSHyperlinkDetector();
			IHyperlink[] hyperlinks = detector.detectHyperlinks(themeableEditor, new Region(offset, 0), true);

			// make sure we got as many links as we expected. Note that we expect a null result when no links are
			// expected
			if (expectedHyperlinks == null || expectedHyperlinks.length == 0)
			{
				assertNull(hyperlinks);
			}
			else
			{
				assertNotNull(hyperlinks);
				assertEquals(expectedHyperlinks.length, hyperlinks.length);
			}

			for (int i = 0; i < expectedHyperlinks.length; i++)
			{
				// check type
				IHyperlink hyperlink = hyperlinks[i];
				assertTrue(hyperlink instanceof JSHyperlink);

				// check content
				// assertEquals(targetHyperlinks[i], hyperlink);
				assertHyperlink(expectedHyperlinks[i], (JSHyperlink) hyperlink);
			}
		}
	}

	protected void assertHyperlink(JSHyperlink a, JSHyperlink b)
	{
		// @formatter:off
		assertEquals("Hyperlink regions do not match", a.getHyperlinkRegion(), b.getHyperlinkRegion());
		assertTrue(
			"Labels do not match. Expected '" + a.getTypeLabel() + "', but found '" + b.getTypeLabel() + "'",
			StringUtil.areEqual(a.getTypeLabel(), b.getTypeLabel())
		);
		assertTrue("Text values do not match", b.getHyperlinkText().endsWith(a.getHyperlinkText()));
		assertTrue("File paths do not match", b.getTargetFilePath().endsWith(a.getTargetFilePath()));
		assertTrue(
			"Search strings do not match. Expected '" + a.getSearchString() + "', but found '" + b.getSearchString() + "'",
			StringUtil.areEqual(a.getSearchString(), b.getSearchString())
		);
		assertEquals("Target regions do not match", a.getTargetRegion(), b.getTargetRegion());
		// @formatter:on
	}

	public void testGlobal()
	{
		String resource = "hyperlinks/global.js";

		assertHyperlinks(resource, new JSHyperlink(new Region(28, 3), "variable", resource, resource, "abc"));
	}

	public void testPropertyIsFunctionDeclaration()
	{
		String resource = "hyperlinks/propertyIsFunctionDeclaration.js";

		// NOTE: we should not get any links here
		assertHyperlinks(resource);
	}

	public void testPropertyIsFunction()
	{
		String resource = "hyperlinks/propertyIsFunction.js";
		JSHyperlink link = new JSHyperlink(new Region(34, 3), "", resource, resource, "def");

		assertHyperlinks(resource, link);
	}

	public void testParameter()
	{
		String resource = "hyperlinks/parameter.js";
		JSHyperlink link = new JSHyperlink(new Region(39, 3), "parameter", resource, resource, new Region(13, 3));

		assertHyperlinks(resource, link);
	}

	public void testNestedParameter()
	{
		String resource = "hyperlinks/nestedParameter.js";
		JSHyperlink link = new JSHyperlink(new Region(71, 3), "parameter", resource, resource, new Region(13, 3));

		assertHyperlinks(resource, link);
	}

	public void testLocalDeclaration()
	{
		String resource = "hyperlinks/localDeclaration.js";
		JSHyperlink link = new JSHyperlink(new Region(42, 3), "local", resource, resource, new Region(22, 3));

		assertHyperlinks(resource, link);
	}

	public void testLocalAssignment()
	{
		String resource = "hyperlinks/localAssignment.js";
		JSHyperlink link = new JSHyperlink(new Region(42, 3), "local", resource, resource, new Region(18, 3));

		assertHyperlinks(resource, link);
	}
}
