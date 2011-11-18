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

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.util.EditorUtil;
import com.aptana.editor.js.IDebugScopes;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.JSLocationIdentifier;
import com.aptana.editor.js.contentassist.LocationType;
import com.aptana.editor.js.contentassist.ParseUtil;
import com.aptana.editor.js.contentassist.model.BaseElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.editor.js.parsing.ast.IJSNodeTypes;
import com.aptana.editor.js.parsing.ast.JSIdentifierNode;
import com.aptana.editor.js.parsing.ast.JSNode;
import com.aptana.editor.js.parsing.ast.JSParseRootNode;
import com.aptana.editor.js.parsing.ast.JSTreeWalker;
import com.aptana.index.core.Index;
import com.aptana.parsing.ast.IParseNode;

/**
 * JSHyperlinkCollector
 */
public class JSHyperlinkCollector extends JSTreeWalker
{
	private AbstractThemeableEditor editor;
	private JSParseRootNode ast;
	private int offset;
	private List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>();

	/**
	 * JSHyperlinkCollector
	 */
	public JSHyperlinkCollector(AbstractThemeableEditor editor, JSParseRootNode ast, int offset)
	{
		this.editor = editor;
		this.ast = ast;
		this.offset = offset;
	}

	/**
	 * addHyperlink
	 * 
	 * @param link
	 */
	protected void addHyperlink(IHyperlink link)
	{
		if (link != null)
		{
			hyperlinks.add(link);
		}
	}

	/**
	 * getHyperlink
	 * 
	 * @return
	 */
	public List<IHyperlink> getHyperlinks()
	{
		return hyperlinks;
	}

	/**
	 * processLink
	 * 
	 * @param start
	 * @param length
	 */
	protected void processLink(String linkText, int start, int length)
	{
		String linkType = "<unknown>";

		// grab node at the offset
		IParseNode node = ast.getNodeAtOffset(offset);

		// determine location type at the offset
		JSLocationIdentifier identifier = new JSLocationIdentifier(offset, node);
		((JSParseRootNode) ast).accept(identifier);
		LocationType type = identifier.getType();

		// container for elements to possible process later
		List<? extends BaseElement<?>> elements = null;

		switch (type)
		{
			case IN_PROPERTY_NAME:
			{
				linkType = "property";

				Index index = EditorUtil.getIndex(editor);

				// @formatter:off
				List<String> types = ParseUtil.getParentObjectTypes(
					index,
					EditorUtil.getURI(editor),
					node,
					ParseUtil.getGetPropertyNode(identifier.getTargetNode(), identifier.getStatementNode()),
					offset
				);
				// @formatter:on

				if (types != null && !types.isEmpty())
				{
					// @formatter:off
					IdeLog.logInfo(
						JSPlugin.getDefault(),
						"Hyperlink property types: " + StringUtil.join(", ", types), //$NON-NLS-1$ //$NON-NLS-2$
						IDebugScopes.OPEN_DECLARATION_TYPES
					);
					// @formatter:on

					JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();
					// NOTE: I can't instantiate and add to elements directly, so we have to do this temporary list
					// dance with a later assignment to make the compiler happy
					List<TypeElement> typeList = new ArrayList<TypeElement>();

					for (String typeName : types)
					{
						typeList.addAll(queryHelper.getTypes(index, typeName, true));
					}

					elements = typeList;
				}
				break;
			}

			case IN_VARIABLE_NAME:
			{
				linkType = "variable";

				elements = new JSIndexQueryHelper().getGlobal(EditorUtil.getIndex(editor), node.getText());
				break;
			}

			default:
				break;
		}

		if (elements != null && !elements.isEmpty())
		{
			for (BaseElement<?> element : elements)
			{
				// @formatter:off
				IdeLog.logInfo(
					JSPlugin.getDefault(),
					"Hyperlink type model element: " + element.toSource(), //$NON-NLS-1$
					IDebugScopes.OPEN_DECLARATION_TYPES
				);
				// @formatter:on

				List<String> documents = element.getDocuments();

				if (documents != null && !documents.isEmpty())
				{
					String documentList = StringUtil.join(", ", documents);

					// @formatter:off
					IdeLog.logInfo(
						JSPlugin.getDefault(),
						"Hyperlink type model documents: " + documentList, //$NON-NLS-1$ //$NON-NLS-2$
						IDebugScopes.OPEN_DECLARATION_TYPES
					);
					// @formatter:on

					JSHyperlink jsLink = new JSHyperlink(new Region(start, length), linkType, linkText + ": "
							+ documentList);
					jsLink.setFilePath(documents.get(0));
					jsLink.setSearchString(element.getName());

					addHyperlink(jsLink);
				}
			}
		}
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

				processLink(node.getText(), start, length);
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
