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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.aptana.editor.common.AbstractThemeableEditor;

/**
 * JSHyperlink
 */
public class JSHyperlink implements IHyperlink
{
	private IRegion region;
	private String type;
	private String text;
	private String filePath;
	private int fileOffset;

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
	public int getOffset()
	{
		return fileOffset;
	}

	/**
	 * getPath
	 * 
	 * @return
	 */
	public String getPath()
	{
		return filePath;
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

	/**
	 * getEditorDescriptor
	 * 
	 * @param uri
	 * @return
	 */
	private IEditorDescriptor getEditorDescriptor(URI uri)
	{
		IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();

		if (uri.getPath() == null || uri.getPath().equals("/") || uri.getPath().trim().equals("")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return null;
		}

		IPath path = new Path(uri.getPath());

		return editorReg.getDefaultEditor(path.lastSegment());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
	 */
	public void open()
	{
		if (filePath != null)
		{
			try
			{
				File file = new File(new URI(filePath));
				IEditorPart part = null;

				if (file.exists())
				{
					part = openInEditor(file);
				}
				else
				{
					IResource findMember = ResourcesPlugin.getWorkspace().getRoot().findMember(filePath);

					if (findMember != null && findMember.exists() && findMember instanceof IFile)
					{
						part = openInEditor(new File(((IFile) findMember).getLocationURI()));
					}
				}

				if (part instanceof AbstractThemeableEditor)
				{
					AbstractTextEditor editor = (AbstractTextEditor) part;

					editor.selectAndReveal(fileOffset, 0);
				}
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private IEditorPart openInEditor(File file)
	{
		try
		{
			URI uri = file.toURI();
			IEditorDescriptor desc = getEditorDescriptor(uri);
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			if (desc == null)
			{
				return IDE.openEditor(page, uri, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
			}
			else
			{
				return IDE.openEditor(page, uri, desc.getId(), true);
			}
		}
		catch (Exception e)
		{
			//IdeLog.logError(PHPEditorPlugin.getDefault(), "Error open a file in the editor", e); //$NON-NLS-1$
		}

		return null;
	}

	/**
	 * setFilePath
	 * 
	 * @param path
	 */
	public void setFilePath(String path)
	{
		filePath = path;
	}

	/**
	 * setFileOffset
	 * 
	 * @param offset
	 */
	public void setFileOffset(int offset)
	{
		fileOffset = offset;
	}
}
