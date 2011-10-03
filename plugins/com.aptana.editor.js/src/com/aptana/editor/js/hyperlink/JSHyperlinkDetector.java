/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.hyperlink;

import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.parsing.FileService;
import com.aptana.editor.common.util.EditorUtil;
import com.aptana.editor.js.IDebugScopes;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.contentassist.ASTUtil;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.JSLocationIdentifier;
import com.aptana.editor.js.contentassist.LocationType;
import com.aptana.editor.js.contentassist.model.BaseElement;
import com.aptana.editor.js.parsing.ast.JSGetPropertyNode;
import com.aptana.editor.js.parsing.ast.JSParseRootNode;
import com.aptana.index.core.Index;
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
		IHyperlink result = null;

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

		return (result == null) ? null : new IHyperlink[] { result };
	}

	/**
	 * @param editor
	 * @param ast
	 * @param offset
	 * @return
	 */
	private IHyperlink processAST(AbstractThemeableEditor editor, JSParseRootNode ast, int offset)
	{
		// walk AST to grab potential hyperlinks
		JSHyperlinkCollector collector = new JSHyperlinkCollector(offset);
		ast.accept(collector);
		IHyperlink result = collector.getHyperlink();

		if (result != null)
		{
			IParseNode node = ast.getNodeAtOffset(offset);

			JSLocationIdentifier identifier = new JSLocationIdentifier(offset, node);
			((JSParseRootNode) ast).accept(identifier);
			LocationType type = identifier.getType();

			BaseElement<?> element = null;

			switch (type)
			{
				case IN_PROPERTY_NAME:
				{
					JSGetPropertyNode getPropertyNode = ASTUtil.getGetPropertyNode(identifier.getTargetNode(),
							identifier.getStatementNode());
					Index index = EditorUtil.getIndex(editor);
					URI location = EditorUtil.getURI(editor);
					List<String> types = ASTUtil.getParentObjectTypes(index, location, node, getPropertyNode, offset);

					if (types != null && !types.isEmpty())
					{
						String typeName = types.get(0);
						IdeLog.logInfo(
								JSPlugin.getDefault(),
								"Hyperlink property types: " + StringUtil.join(", ", types), IDebugScopes.OPEN_DECLARATION_TYPES); //$NON-NLS-1$ //$NON-NLS-2$
						JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();

						element = queryHelper.getType(index, typeName, true);
					}
					break;
				}

				case IN_VARIABLE_NAME:
				{
					String name = node.getText();
					Index index = EditorUtil.getIndex(editor);
					JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();

					element = queryHelper.getGlobal(index, name);
					break;
				}

				default:
					break;
			}

			if (element != null)
			{
				IdeLog.logInfo(JSPlugin.getDefault(),
						"Hyperlink type model element: " + element.toSource(), IDebugScopes.OPEN_DECLARATION_TYPES); //$NON-NLS-1$

				System.out.println(element.toSource());

				List<String> documents = element.getDocuments();

				if (documents != null && !documents.isEmpty())
				{
					IdeLog.logInfo(
							JSPlugin.getDefault(),
							"Hyperlink type model documents: " + StringUtil.join(", ", documents), IDebugScopes.OPEN_DECLARATION_TYPES); //$NON-NLS-1$

					((JSHyperlink) result).setFilePath(documents.get(0));
				}
			}
		}

		return result;
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
