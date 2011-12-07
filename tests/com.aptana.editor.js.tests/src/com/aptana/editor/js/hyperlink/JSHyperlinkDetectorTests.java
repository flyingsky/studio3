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

	public void testGlobal()
	{
		String resource = "hyperlinks/global.js";

		setupTestContext(resource);

		if (editor instanceof AbstractThemeableEditor)
		{
			AbstractThemeableEditor themeableEditor = (AbstractThemeableEditor) editor;
			JSHyperlinkDetector detector = new JSHyperlinkDetector();

			for (int offset : cursorOffsets)
			{
				IHyperlink[] hyperlinks = detector.detectHyperlinks(themeableEditor, new Region(offset, 0), true);

				assertNotNull(hyperlinks);

				// check count
				assertEquals(1, hyperlinks.length);

				// check content
			}
		}
	}
}
