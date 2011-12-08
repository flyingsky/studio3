/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.hyperlink;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.StringUtil;
import com.aptana.core.util.URIUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.util.EditorUtil;
import com.aptana.editor.js.IDebugScopes;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.JSLocationIdentifier;
import com.aptana.editor.js.contentassist.LocationType;
import com.aptana.editor.js.contentassist.ParseUtil;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.inferencing.JSPropertyCollection;
import com.aptana.editor.js.inferencing.JSScope;
import com.aptana.editor.js.parsing.ast.IJSNodeTypes;
import com.aptana.editor.js.parsing.ast.JSGetPropertyNode;
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
	protected void processLink(JSIdentifierNode node)
	{
		// determine location type at the offset
		JSLocationIdentifier identifier = new JSLocationIdentifier(offset, node);
		((JSParseRootNode) ast).accept(identifier);
		LocationType type = identifier.getType();

		switch (type)
		{
			case IN_PROPERTY_NAME:
			{
				JSGetPropertyNode propertyNode = ParseUtil.getGetPropertyNode(identifier.getTargetNode(),
						identifier.getStatementNode());
				processProperty(node, propertyNode);
				break;
			}

			case IN_VARIABLE_NAME:
			{
				processVariable(node);
				break;
			}

			default:
				break;
		}
	}

	/**
	 * @param node
	 * @param identifier
	 */
	protected void processProperty(JSIdentifierNode node, JSGetPropertyNode propertyNode)
	{
		List<PropertyElement> elements = new ArrayList<PropertyElement>();
		Index index = EditorUtil.getIndex(editor);
		URI editorURI = EditorUtil.getURI(editor);
		List<String> types = ParseUtil.getParentObjectTypes(index, editorURI, node, propertyNode, offset);

		if (types != null && !types.isEmpty())
		{
			JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();

			for (String typeName : types)
			{
				elements.addAll(queryHelper.getTypeMembers(index, typeName, node.getText()));
			}
		}

		processPropertyElements(elements, node);
	}

	/**
	 * processVariable
	 * 
	 * @param start
	 * @param length
	 * @param node
	 */
	protected void processVariable(JSIdentifierNode node)
	{
		JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();
		List<PropertyElement> elements = queryHelper.getGlobal(EditorUtil.getIndex(editor), node.getText());

		processPropertyElements(elements, node);
	}

	protected IRegion getNodeRegion(JSNode node)
	{
		int start = node.getStart();
		int length = node.getLength();

		if (node.getSemicolonIncluded())
		{
			--length;
		}

		return new Region(start, length);
	}

	/**
	 * processPropertyElements
	 * 
	 * @param elements
	 * @param node
	 */
	protected void processPropertyElements(List<PropertyElement> elements, JSIdentifierNode node)
	{
		URI projectURI = EditorUtil.getProjectURI(editor);
		IRegion region = getNodeRegion(node);
		String linkType = getLinkType(node);

		for (PropertyElement element : elements)
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

				String elementName = element.getName();

				for (String document : documents)
				{
					// NOTE: projectURI is null during unit testing
					if (projectURI == null || isInCurrentProject(projectURI, document))
					{
						String text = getDocumentDisplayName(projectURI, document);

						addHyperlink(new JSHyperlink(region, linkType, text, document, elementName));
					}
				}
			}
		}
	}

	/**
	 * getLinkType
	 * 
	 * @param node
	 * @return
	 */
	protected String getLinkType(JSIdentifierNode node)
	{
		String result;
		IParseNode parent = node.getParent();

		if (parent.getNodeType() == IJSNodeTypes.GET_PROPERTY)
		{
			if (parent.getFirstChild().getNodeType() == IJSNodeTypes.INVOKE)
			{
				result = "invocation";
			}
			else
			{
				result = "";
			}
		}
		else
		{
			result = "variable";
		}

		return result;
	}

	/**
	 * Determine if the specified document is within the specified project
	 * 
	 * @param projectURI
	 * @param document
	 * @return
	 */
	protected boolean isInCurrentProject(URI projectURI, String document)
	{
		String prefix = (projectURI != null) ? URIUtil.decodeURI(projectURI.toString()) : null;
		boolean result = false;

		String path = URIUtil.decodeURI(document);

		if (prefix != null && path.startsWith(prefix))
		{
			result = true;
		}

		return result;
	}

	/**
	 * Format the document to a relative path within the project, including the project name in the result
	 * 
	 * @param projectURI
	 * @param document
	 * @return
	 */
	protected String getDocumentDisplayName(URI projectURI, String document)
	{
		String prefix = (projectURI != null) ? URIUtil.decodeURI(projectURI.toString()) : null;

		// back up one segment so we include the project name in the document
		if (prefix != null && prefix.length() > 2)
		{
			int index = prefix.lastIndexOf('/', prefix.length() - 2);

			if (index != -1 && index > 0)
			{
				prefix = prefix.substring(0, index - 1);
			}
		}

		String result = URIUtil.decodeURI(document);

		if (prefix != null && result.startsWith(prefix))
		{
			result = result.substring(prefix.length() + 1);
		}

		return result;
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
			JSScope globalScope = ast.getGlobals();
			JSScope activeScope = globalScope.getScopeAtOffset(offset);
			JSPropertyCollection properties = activeScope.getSymbol(node.getText());

			if (properties != null)
			{
				for (JSNode value : properties.getValues())
				{
					IParseNode parent = value.getParent();

					switch (parent.getNodeType())
					{
						case IJSNodeTypes.PARAMETERS:
						{
							IRegion hyperlinkRegion = getNodeRegion(node);
							String linkType = "parameter";
							URI projectURI = EditorUtil.getProjectURI(editor);
							String editorURI = EditorUtil.getURI(editor).toString();
							String hyperlinkText = getDocumentDisplayName(projectURI, editorURI);
							IRegion targetRegion = getNodeRegion(value);

							addHyperlink(new JSHyperlink(hyperlinkRegion, linkType, hyperlinkText, editorURI,
									targetRegion));
							break;
						}

						case IJSNodeTypes.DECLARATION:
						{
							JSNode targetIdentifier = (JSNode) value.getParent().getFirstChild();

							// NOTE: don't jump to self
							if (targetIdentifier != node)
							{
								IRegion hyperlinkRegion = getNodeRegion(node);
								String linkType = "local";
								URI projectURI = EditorUtil.getProjectURI(editor);
								String editorURI = EditorUtil.getURI(editor).toString();
								String hyperlinkText = getDocumentDisplayName(projectURI, editorURI);
								IRegion targetRegion = getNodeRegion(targetIdentifier);

								addHyperlink(new JSHyperlink(hyperlinkRegion, linkType, hyperlinkText, editorURI,
										targetRegion));
							}
							break;
						}

						default:
							if (value.getNodeType() == IJSNodeTypes.ASSIGN)
							{
								JSNode targetIdentifier = (JSNode) value.getFirstChild();

								// NOTE: don't jump to self
								if (targetIdentifier != node)
								{
									IRegion hyperlinkRegion = getNodeRegion(node);
									String linkType = "local";
									URI projectURI = EditorUtil.getProjectURI(editor);
									String editorURI = EditorUtil.getURI(editor).toString();
									String hyperlinkText = getDocumentDisplayName(projectURI, editorURI);
									IRegion targetRegion = getNodeRegion(targetIdentifier);

									addHyperlink(new JSHyperlink(hyperlinkRegion, linkType, hyperlinkText, editorURI,
											targetRegion));
								}
							}
					}
				}
			}

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
					case IJSNodeTypes.ARGUMENTS:
					case IJSNodeTypes.CONSTRUCT:
					case IJSNodeTypes.INVOKE:
					case IJSNodeTypes.RETURN:
					case IJSNodeTypes.STATEMENTS:
						valid = true;
						break;

					case IJSNodeTypes.GET_PROPERTY:
						// walk up tree until we find the first node that is not part of a series of get-properties
						while (parent != null && parent.getNodeType() == IJSNodeTypes.GET_PROPERTY)
						{
							parent = parent.getParent();
						}

						// don't create links on properties that are on LHS of assignments
						if (parent == null || parent.getNodeType() != IJSNodeTypes.ASSIGN)
						{
							valid = true;
						}
						break;
				}
			}

			if (valid)
			{
				processLink(node);
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
