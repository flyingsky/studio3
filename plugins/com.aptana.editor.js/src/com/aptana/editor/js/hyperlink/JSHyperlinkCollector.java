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

import org.eclipse.jface.text.hyperlink.IHyperlink;

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
	 * @param link
	 */
	protected void addHyperlink(IHyperlink link)
	{
		this._hyperlinks.add(link);
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
}
