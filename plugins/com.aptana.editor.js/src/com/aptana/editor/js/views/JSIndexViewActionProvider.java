/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.views;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.model.ClassElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.editor.js.inferencing.JSTypeUtil;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;
import com.aptana.index.core.ui.views.IActionProvider;
import com.aptana.index.core.ui.views.IndexView;

/**
 * JSIndexViewActionProvider
 */
public class JSIndexViewActionProvider implements IActionProvider
{
	/*
	 * (non-Javadoc)
	 * @see com.aptana.index.core.ui.views.IActionProvider#getActions(java.lang.Object)
	 */
	public IAction[] getActions(final IndexView view, Object object)
	{
		if (object instanceof PropertyElement)
		{
			final List<String> typeNames = ((PropertyElement) object).getTypeNames();

			if (typeNames != null && !typeNames.isEmpty())
			{
				return new IAction[] { createAction(view, typeNames) };
			}
		}

		return null;
	}

	/**
	 * @param view
	 * @param typeNames
	 * @return
	 */
	protected IAction createAction(final IndexView view, final List<String> typeNames)
	{
		IAction action = new Action()
		{
			@Override
			public void run()
			{
				TreeViewer treeViewer = view.getTreeViewer();

				if (treeViewer != null)
				{
					Object input = treeViewer.getInput();

					if (input instanceof IProject)
					{
						IProject project = (IProject) input;

						Index index = IndexManager.getInstance().getIndex(project.getLocationURI());
						JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();
						List<TypeElement> types = queryHelper.getTypes(index, typeNames.get(0), true);
						List<ClassElement> classes = JSTypeUtil.typesToClasses(types);

						if (classes != null && !classes.isEmpty())
						{
							ClassElement c = classes.get(0);

							treeViewer.setSelection(new StructuredSelection(c), true);
						}
					}
				}
			}
		};

		action.setText("Jump to type");

		return action;
	}
}
