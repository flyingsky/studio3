/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.hyperlink;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;

import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.parsing.FileService;
import com.aptana.editor.common.util.EditorUtil;
import com.aptana.editor.js.parsing.ast.IJSNodeTypes;
import com.aptana.editor.js.parsing.ast.JSIdentifierNode;
import com.aptana.editor.js.parsing.ast.JSParseRootNode;
import com.aptana.parsing.ast.IParseNode;

/**
 * JSHyperlink
 */
public class JSHyperlink implements IHyperlink
{
	private IRegion hyperlinkRegion;
	private String typeLabel;
	private String hyperlinkText;

	private String targetFilePath;
	private String searchString;
	private IRegion targetRegion;

	/**
	 * JSHyperlink
	 * 
	 * @param hyperlinkRegion
	 * @param typeLabel
	 * @param hyperlinkText
	 * @param targetFilePath
	 * @param searchString
	 */
	public JSHyperlink(IRegion hyperlinkRegion, String typeLabel, String hyperlinkText, String targetFilePath,
			String searchString)
	{
		this.hyperlinkRegion = hyperlinkRegion;
		this.typeLabel = typeLabel;
		this.hyperlinkText = hyperlinkText;
		this.targetFilePath = targetFilePath;
		this.searchString = searchString;
	}

	/**
	 * JSHyperlink
	 * 
	 * @param hyperlinkRegion
	 * @param typeLabel
	 * @param hyperlinkText
	 * @param targetFilePath
	 * @param searchString
	 */
	public JSHyperlink(IRegion hyperlinkRegion, String typeLabel, String hyperlinkText, String targetFilePath,
			IRegion targetRegion)
	{
		this.hyperlinkRegion = hyperlinkRegion;
		this.typeLabel = typeLabel;
		this.hyperlinkText = hyperlinkText;
		this.targetFilePath = targetFilePath;
		this.targetRegion = targetRegion;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		boolean result = false;

		if (obj instanceof JSHyperlink)
		{
			JSHyperlink link = (JSHyperlink) obj;

			result = getHyperlinkRegion().equals(link.getHyperlinkRegion())
					&& StringUtil.areEqual(getTypeLabel(), link.getTypeLabel())
					&& StringUtil.areEqual(getHyperlinkText(), link.getHyperlinkText())
					&& StringUtil.areEqual(getTargetFilePath(), link.getTargetFilePath())
					&& StringUtil.areEqual(getSearchString(), link.getSearchString());
		}
		else
		{
			result = super.equals(obj);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
	 */
	public IRegion getHyperlinkRegion()
	{
		return hyperlinkRegion;
	}

	/**
	 * getSearchString
	 * 
	 * @return
	 */
	public String getSearchString()
	{
		return searchString;
	}

	/**
	 * getTargetFilePath
	 * 
	 * @return
	 */
	public String getTargetFilePath()
	{
		return targetFilePath;
	}

	/**
	 * getTargetRegion
	 * 
	 * @return
	 */
	public IRegion getTargetRegion()
	{
		return targetRegion;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel()
	{
		return typeLabel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
	 */
	public String getHyperlinkText()
	{
		return hyperlinkText;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
	 */
	public void open()
	{
		if (targetFilePath != null)
		{
			try
			{
				File file = new File(new URI(targetFilePath));
				IEditorPart part = null;

				if (file.exists())
				{
					part = EditorUtil.openInEditor(file);
				}
				else
				{
					IResource findMember = ResourcesPlugin.getWorkspace().getRoot().findMember(targetFilePath);

					if (findMember != null && findMember.exists() && findMember instanceof IFile)
					{
						part = EditorUtil.openInEditor(new File(((IFile) findMember).getLocationURI()));
					}
				}

				if (part instanceof AbstractThemeableEditor)
				{
					AbstractThemeableEditor editor = (AbstractThemeableEditor) part;

					if (targetRegion != null)
					{
						editor.selectAndReveal(targetRegion.getOffset(), targetRegion.getLength());
					}
					else
					{
						FileService fileService = editor.getFileService();

						// make sure the file has been parsed
						fileService.parse(new NullProgressMonitor());

						// grab AST
						IParseNode ast = fileService.getParseResult();

						if (ast instanceof JSParseRootNode)
						{
							JSIdentifierCollector collector = new JSIdentifierCollector(searchString);
							((JSParseRootNode) ast).accept(collector);
							List<JSIdentifierNode> identifiers = collector.getIdentifiers();
							JSIdentifierNode targetIdentifier = null;

							// assume first item in list
							if (identifiers.size() > 0)
							{
								targetIdentifier = identifiers.get(0);
							}

							// try to refine the selection
							for (JSIdentifierNode identifier : identifiers)
							{
								if ("invocation".equals(typeLabel))
								{
									if (identifier.getParent().getNodeType() == IJSNodeTypes.FUNCTION)
									{
										targetIdentifier = identifier;
										break;
									}
								}
							}

							// show what we ended up with
							if (targetIdentifier != null)
							{
								editor.selectAndReveal(targetIdentifier.getStart(), targetIdentifier.getLength());
							}
						}
						else
						{
							IFindReplaceTarget target = (IFindReplaceTarget) part.getAdapter(IFindReplaceTarget.class);

							if (target != null && target.canPerformFind())
							{
								target.findAndSelect(0, searchString, true, true, true);
							}
							else
							{
								editor.selectAndReveal(0, 0);
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				// TODO: log
				e.printStackTrace();
			}
		}
	}
}
