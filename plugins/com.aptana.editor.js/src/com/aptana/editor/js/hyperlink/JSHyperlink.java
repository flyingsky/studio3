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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.util.EditorUtil;

/**
 * JSHyperlink
 */
public class JSHyperlink implements IHyperlink
{
	private IRegion region;
	private String type;
	private String text;

	private String targetFilePath;
	private IRegion targetFileRegion;
	private String searchString;

	/**
	 * JSHyperlink
	 * 
	 * @param region
	 * @param type
	 * @param text
	 */
	public JSHyperlink(IRegion region, String type, String text)
	{
		this.region = region;
		this.type = type;
		this.text = text;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
	 */
	public IRegion getHyperlinkRegion()
	{
		return region;
	}

	/**
	 * getOffset
	 * 
	 * @return
	 */
	public IRegion getTargetFileRegion()
	{
		return targetFileRegion;
	}

	/**
	 * getPath
	 * 
	 * @return
	 */
	public String getPath()
	{
		return targetFilePath;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel()
	{
		return type;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
	 */
	public String getHyperlinkText()
	{
		return text;
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
					AbstractTextEditor editor = (AbstractTextEditor) part;

					if (targetFileRegion != null)
					{
						editor.selectAndReveal(targetFileRegion.getOffset(), targetFileRegion.getLength());
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
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * setFilePath
	 * 
	 * @param path
	 */
	public void setFilePath(String path)
	{
		targetFilePath = path;
	}

	/**
	 * setSearchString
	 * 
	 * @param string
	 */
	public void setSearchString(String string)
	{
		searchString = string;
	}

	/**
	 * setFileOffset
	 * 
	 * @param offset
	 */
	public void setTargetFileRegion(IRegion region)
	{
		targetFileRegion = region;
	}
}
