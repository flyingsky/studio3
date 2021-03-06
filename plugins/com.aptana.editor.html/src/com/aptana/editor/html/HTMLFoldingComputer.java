/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html;

import org.eclipse.jface.text.IDocument;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.text.AbstractFoldingComputer;
import com.aptana.editor.css.ICSSConstants;
import com.aptana.editor.css.internal.text.CSSFoldingComputer;
import com.aptana.editor.html.parsing.ast.HTMLNode;
import com.aptana.editor.html.parsing.ast.HTMLTextNode;
import com.aptana.editor.js.IJSConstants;
import com.aptana.editor.js.internal.text.JSFoldingComputer;
import com.aptana.parsing.ast.IParseNode;

public class HTMLFoldingComputer extends AbstractFoldingComputer
{

	public HTMLFoldingComputer(AbstractThemeableEditor editor, IDocument document)
	{
		super(editor, document);
	}

	@Override
	public boolean isFoldable(IParseNode child)
	{
		if (IJSConstants.CONTENT_TYPE_JS.equals(child.getLanguage()))
		{
			return getJSFoldingComputer().isFoldable(child);
		}
		if (ICSSConstants.CONTENT_TYPE_CSS.equals(child.getLanguage()))
		{
			return getCSSFoldingComputer().isFoldable(child);
		}
		return child instanceof HTMLNode && !(child instanceof HTMLTextNode);
	}

	@Override
	public boolean isCollapsed(IParseNode child)
	{
		if (IJSConstants.CONTENT_TYPE_JS.equals(child.getLanguage()))
		{
			return getJSFoldingComputer().isCollapsed(child);
		}
		if (ICSSConstants.CONTENT_TYPE_CSS.equals(child.getLanguage()))
		{
			return getCSSFoldingComputer().isCollapsed(child);
		}
		return false;
	}

	private AbstractFoldingComputer getJSFoldingComputer()
	{
		return new JSFoldingComputer(getEditor(), getDocument());
	}

	private AbstractFoldingComputer getCSSFoldingComputer()
	{
		return new CSSFoldingComputer(getEditor(), getDocument());
	}

}
