/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;

import com.aptana.editor.common.IFoldingEditor;

public class AbstractFoldingEditor extends AbstractDecoratedTextEditor implements IFoldingEditor
{

	/**
	 * AbstractFoldingEditor
	 */
	public AbstractFoldingEditor()
	{
	}

	public void createPartControl(Composite parent)
	{
		super.createPartControl(parent);

		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
		ProjectionSupport projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
		projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$
		projectionSupport.install();

		viewer.doOperation(ProjectionViewer.TOGGLE);
	}

	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles)
	{
		ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.IFoldingEditor#updateFoldingStructure(java.util.Map)
	 */
	public synchronized void updateFoldingStructure(Map<ProjectionAnnotation, Position> annotations)
	{
		List<Annotation> deletions = new ArrayList<Annotation>();
		Collection<Position> additions = annotations.values();
		ProjectionAnnotationModel currentModel = getAnnotationModel();
		if (currentModel == null)
		{
			return;
		}
		for (@SuppressWarnings("rawtypes")
		Iterator iter = currentModel.getAnnotationIterator(); iter.hasNext();)
		{
			Object annotation = iter.next();
			if (annotation instanceof ProjectionAnnotation)
			{
				Position position = currentModel.getPosition((Annotation) annotation);
				if (additions.contains(position))
				{
					additions.remove(position);
				}
				else
				{
					deletions.add((Annotation) annotation);
				}
			}
		}
		if (annotations.size() != 0 || deletions.size() != 0)
		{
			currentModel.modifyAnnotations(deletions.toArray(new Annotation[deletions.size()]), annotations, null);
		}
	}

	protected ProjectionAnnotationModel getAnnotationModel()
	{
		ISourceViewer viewer = getSourceViewer();
		if (viewer instanceof ProjectionViewer)
		{
			return ((ProjectionViewer) viewer).getProjectionAnnotationModel();
		}
		return null;
	}

	/**
	 * This code auto-refreshes files that are out of synch when we first open them. This is a bit of a hack that looks
	 * to see if it seems we're out of sync and the file isn't open yet. If it is already open, we call super so it pops
	 * a dialog asking if you want to update the file contents.
	 */
	@Override
	protected void handleEditorInputChanged()
	{
		final IDocumentProvider provider = getDocumentProvider();
		if (provider == null)
		{
			// fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=15066
			close(false);
			return;
		}

		final IEditorInput input = getEditorInput();
		boolean wasActivated = true;
		try
		{
			Field f = AbstractTextEditor.class.getDeclaredField("fHasBeenActivated"); //$NON-NLS-1$
			f.setAccessible(true);
			wasActivated = (Boolean) f.get(this);
		}
		catch (Exception e1)
		{
			// ignore
		}
		if (!wasActivated && !provider.isDeleted(input))
		{
			try
			{
				if (provider instanceof IDocumentProviderExtension)
				{
					IDocumentProviderExtension extension = (IDocumentProviderExtension) provider;
					extension.synchronize(input);
				}
				else
				{
					doSetInput(input);
				}
				return;
			}
			catch (CoreException e)
			{
				// ignore
			}
		}
		super.handleEditorInputChanged();
	}
}
