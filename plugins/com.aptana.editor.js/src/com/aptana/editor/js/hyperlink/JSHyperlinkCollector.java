/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.hyperlink;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.aptana.editor.js.parsing.ast.JSIdentifierNode;
import com.aptana.editor.js.parsing.ast.JSTreeWalker;

/**
 * JSHyperlinkCollector
 */
public class JSHyperlinkCollector extends JSTreeWalker
{
	List<IHyperlink> _hyperlinks;

	/**
	 * JSHyperlinkCollector
	 */
	public JSHyperlinkCollector()
	{
		this._hyperlinks = new ArrayList<IHyperlink>();
	}

	/**
	 * addHyperlink
	 * 
	 * @param start
	 * @param length
	 * @param type
	 * @param text
	 */
	protected void addHyperlink(int start, int length, String type, String text)
	{
		this._hyperlinks.add(new JSHyperlink(new Region(start, length), type, text));
	}

	/**
	 * getHyperlinks
	 * 
	 * @return
	 */
	public List<IHyperlink> getHyperlinks()
	{
		return this._hyperlinks;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSIdentifierNode)
	 */
	@Override
	public void visit(JSIdentifierNode node)
	{
		this.addHyperlink(node.getStart(), node.getLength(), "A type", "Some text");
	}
}
