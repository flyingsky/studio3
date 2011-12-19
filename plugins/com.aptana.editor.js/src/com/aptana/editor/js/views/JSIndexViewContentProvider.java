/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.views;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.aptana.core.util.CollectionsUtil;
import com.aptana.editor.js.contentassist.model.ClassElement;
import com.aptana.editor.js.contentassist.model.ClassGroupElement;
import com.aptana.editor.js.contentassist.model.JSElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;

/**
 * JSIndexViewContentProvider
 */
public class JSIndexViewContentProvider implements ITreeContentProvider
{
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose()
	{
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement)
	{
		List<? extends Object> result = Collections.emptyList();

		if (parentElement instanceof JSElement)
		{
			JSElement root = (JSElement) parentElement;

			// @formatter:off
			result = CollectionsUtil.newList(
				new ClassGroupElement("Workspace Globals", root.getWorkspaceGlobalClasses()),
				new ClassGroupElement("Project Globals", root.getProjectGlobalClasses())
			);
			// @formatter:on
		}
		else if (parentElement instanceof ClassGroupElement)
		{
			ClassGroupElement group = (ClassGroupElement) parentElement;

			result = group.getClasses();
		}
		else if (parentElement instanceof ClassElement)
		{
			TypeElement type = (ClassElement) parentElement;

			result = type.getProperties();
		}

		return result.toArray(new Object[result.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement)
	{
		Object[] result;

		if (inputElement instanceof IProject)
		{
			IProject project = (IProject) inputElement;
			Index index = IndexManager.getInstance().getIndex(project.getLocationURI());

			result = new Object[] { new JSElement(index) };
		}
		else
		{
			result = new Object[0];
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element)
	{
		return getChildren(element).length > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object,
	 * java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		// do nothing
	}
}
