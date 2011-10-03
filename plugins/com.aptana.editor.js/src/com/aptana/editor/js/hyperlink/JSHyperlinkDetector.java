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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.parsing.FileService;
import com.aptana.editor.js.contentassist.ASTUtil;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.JSLocationIdentifier;
import com.aptana.editor.js.contentassist.LocationType;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.editor.js.parsing.ast.JSGetPropertyNode;
import com.aptana.editor.js.parsing.ast.JSParseRootNode;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;
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

		// grab file service
		FileService fileService = this.getFileService(editor);

		if (fileService != null)
		{
			// grab AST
			IParseNode ast = fileService.getParseResult();

			if (ast instanceof JSParseRootNode)
			{
				int offset = region.getOffset();

				// walk AST to grab potential hyperlinks
				JSHyperlinkCollector collector = new JSHyperlinkCollector(offset);
				((JSParseRootNode) ast).accept(collector);
				result = collector.getHyperlink();

				if (result != null)
				{
					IParseNode node = ast.getNodeAtOffset(offset);
					JSLocationIdentifier identifier = new JSLocationIdentifier(offset, node);
					((JSParseRootNode) ast).accept(identifier);
					LocationType type = identifier.getType();

					switch (type)
					{
						case IN_PROPERTY_NAME:
						{
							JSGetPropertyNode getPropertyNode = ASTUtil.getGetPropertyNode(identifier.getTargetNode(),
									identifier.getStatementNode());
							Index index = getIndex(editor);
							URI location = getURI(editor);
							List<String> types = ASTUtil.getParentObjectTypes(index, location, node, getPropertyNode,
									offset);

							if (types != null && !types.isEmpty())
							{
								String typeName = types.get(0);
								JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();
								TypeElement typeElement = queryHelper.getType(index, typeName, true);

								System.out.println(typeElement.toSource());

								@SuppressWarnings("unchecked")
								List<String> documents = typeElement.getDocuments();
								if (documents != null && !documents.isEmpty())
								{
									((JSHyperlink) result).setFilePath(documents.get(0));
									System.out.println(documents);
								}
							}
							break;
						}

						case IN_VARIABLE_NAME:
						{
							String name = node.getText();
							Index index = getIndex(editor);
							JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();
							PropertyElement global = queryHelper.getGlobal(index, name);

							System.out.println(global.toSource());

							List<String> documents = global.getDocuments();
							if (documents != null && !documents.isEmpty())
							{
								((JSHyperlink) result).setFilePath(documents.get(0));
								System.out.println(documents);
							}
							break;
						}

						default:
							break;
					}
				}
			}
		}

		return (result == null) ? null : new IHyperlink[] { result };
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

	/**
	 * getIndex
	 * 
	 * @param editor
	 * @return
	 */
	protected Index getIndex(AbstractThemeableEditor editor)
	{
		Index result = null;

		if (editor != null)
		{
			IEditorInput editorInput = editor.getEditorInput();

			if (editorInput instanceof IFileEditorInput)
			{
				IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
				IFile file = fileEditorInput.getFile();
				IProject project = file.getProject();
				result = IndexManager.getInstance().getIndex(project.getLocationURI());
			}
			else if (editorInput instanceof IURIEditorInput)
			{
				IURIEditorInput uriEditorInput = (IURIEditorInput) editorInput;
				URI uri = uriEditorInput.getURI();
				// FIXME This file may be a child, we need to check to see if there's an index with a parent URI.
				result = IndexManager.getInstance().getIndex(uri);
			}
		}

		return result;
	}

	/**
	 * getURI
	 * 
	 * @param editor
	 * @return
	 */
	protected URI getURI(AbstractThemeableEditor editor)
	{
		URI result = null;

		if (editor != null)
		{
			IEditorInput editorInput = editor.getEditorInput();

			if (editorInput instanceof IURIEditorInput)
			{
				IURIEditorInput fileEditorInput = (IURIEditorInput) editorInput;

				result = fileEditorInput.getURI();
			}
		}

		return result;
	}

	/**
	 * getFileService
	 * 
	 * @param textViewer
	 * @return
	 */
	protected FileService getFileService(AbstractThemeableEditor editor)
	{
		FileService result = null;

		if (editor != null)
		{
			result = editor.getFileService();
		}

		return result;
	}
}
