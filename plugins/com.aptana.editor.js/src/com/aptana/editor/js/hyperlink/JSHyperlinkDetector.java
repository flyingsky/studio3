/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.hyperlink;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.parsing.FileService;
import com.aptana.editor.js.parsing.ast.JSParseRootNode;
import com.aptana.parsing.ast.IParseNode;

public class JSHyperlinkDetector extends AbstractHyperlinkDetector
{

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer,
	 * org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
	{
		return detectHyperlinks(getEditor(textViewer), region, canShowMultipleHyperlinks);
	}

	/**
	 * detectHyperlinks
	 * 
	 * @param editor
	 * @param region
	 * @param canShowMultipleHyperlinks
	 * @return
	 */
	public IHyperlink[] detectHyperlinks(AbstractThemeableEditor editor, IRegion region,
			boolean canShowMultipleHyperlinks)
	{
		IHyperlink[] result = null;

		if (editor != null && region != null)
		{
			// grab file service
			FileService fileService = editor.getFileService();

			if (fileService != null)
			{
				// grab AST
				IParseNode ast = fileService.getParseResult();

				if (ast instanceof JSParseRootNode)
				{
					// gather links
					result = processAST(editor, (JSParseRootNode) ast, region.getOffset());
				}
			}
		}

		if (!canShowMultipleHyperlinks && result != null && result.length > 0)
		{
			result = new IHyperlink[] { result[0] };
		}

		return result;
	}

	/**
	 * @param editor
	 * @param ast
	 * @param offset
	 * @return
	 */
	private IHyperlink[] processAST(AbstractThemeableEditor editor, JSParseRootNode ast, int offset)
	{
		// walk AST to grab potential hyperlinks
		JSHyperlinkCollector collector = new JSHyperlinkCollector(editor, ast, offset);
		ast.accept(collector);
		List<IHyperlink> result = collector.getHyperlinks();

		// TEMP: for debugging
		if (result != null && result.size() > 1)
		{
			result.add(new JSHyperlink(new Region(0, 0), "empty", "empty"));
		}

		return (result == null || result.isEmpty()) ? null : result.toArray(new IHyperlink[result.size()]);
	}

	/**
	 * getEditor
	 * 
	 * @param textViewer
	 * @return
	 */
	protected AbstractThemeableEditor getEditor(ITextViewer textViewer)
	{
		AbstractThemeableEditor result = null;

		if (textViewer instanceof IAdaptable)
		{
			result = (AbstractThemeableEditor) ((IAdaptable) textViewer).getAdapter(AbstractThemeableEditor.class);
		}

		return result;
	}
}
