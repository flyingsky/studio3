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
import com.aptana.editor.js.parsing.ast.IJSNodeTypes;
import com.aptana.editor.js.parsing.ast.JSIdentifierNode;
import com.aptana.editor.js.parsing.ast.JSNode;
import com.aptana.editor.js.parsing.ast.JSParseRootNode;
import com.aptana.editor.js.parsing.ast.JSTreeWalker;
import com.aptana.parsing.ast.IParseNode;

/**
 * JSHyperlinkCollector
 */
public class JSHyperlinkCollector extends JSTreeWalker
{
	private int offset;
	private IHyperlink hyperlink;

	/**
	 * JSHyperlinkCollector
	 */
	public JSHyperlinkCollector(int offset)
	{
		this.offset = offset;
	}

	/**
	 * addHyperlink
	 * 
	 * @param start
	 * @param length
	 * @param type
	 * @param text
	 */
	protected void setHyperlink(int start, int length, String type, String text)
	{
		hyperlink = new JSHyperlink(new Region(start, length), type, text);
	}

	/**
	 * getHyperlink
	 * 
	 * @return
	 */
	public IHyperlink getHyperlink()
	{
		return hyperlink;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSIdentifierNode)
	 */
	@Override
	public void visit(JSIdentifierNode node)
	{
		if (node.contains(offset))
		{
			IParseNode parent = node.getParent();
			boolean valid = false;

			if (parent instanceof JSParseRootNode)
			{
				valid = true;
			}
			else if (parent instanceof JSNode)
			{
				switch (parent.getNodeType())
				{
					case IJSNodeTypes.CONSTRUCT:
					case IJSNodeTypes.GET_PROPERTY:
					case IJSNodeTypes.INVOKE:
					case IJSNodeTypes.RETURN:
					case IJSNodeTypes.STATEMENTS:
						valid = true;
						break;
				}
			}

			if (valid)
			{
				int start = node.getStart();
				int length = node.getLength();

				if (node.getSemicolonIncluded())
				{
					--length;
				}

				setHyperlink(start, length, StringUtil.EMPTY, StringUtil.EMPTY);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visitChildren(com.aptana.editor.js.parsing.ast.JSNode)
	 */
	@Override
	protected void visitChildren(JSNode node)
	{
		if (node.contains(offset))
		{
			for (IParseNode child : node)
			{
				if (child.contains(offset))
				{
					if (child instanceof JSNode)
					{
						((JSNode) child).accept(this);
					}

					break;
				}
			}
		}
	}
}
