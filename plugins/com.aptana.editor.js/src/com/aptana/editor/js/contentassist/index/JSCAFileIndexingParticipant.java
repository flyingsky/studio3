/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist.index;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.aptana.core.logging.IdeLog;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.JSTypeConstants;
import com.aptana.editor.js.contentassist.model.AliasElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.index.core.AbstractFileIndexingParticipant;
import com.aptana.index.core.Index;
import com.aptana.json.SchemaContext;

/**
 * JSCAFileIndexingParticipant
 */
public class JSCAFileIndexingParticipant extends AbstractFileIndexingParticipant
{
	/*
	 * (non-Javadoc)
	 * @see com.aptana.index.core.AbstractFileIndexingParticipant#indexFileStore(com.aptana.index.core.Index,
	 * org.eclipse.core.filesystem.IFileStore, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void indexFileStore(Index index, IFileStore file, IProgressMonitor monitor)
	{
		SubMonitor sub = SubMonitor.convert(monitor, 100);

		if (file == null)
		{
			return;
		}

		try
		{
			InputStreamReader isr = null;

			sub.subTask(getIndexingMessage(index, file));

			try
			{
				JSCAReader reader = new JSCAReader();
				SchemaContext context = new SchemaContext();
				JSCAHandler handler = new JSCAHandler();

				context.setHandler(handler);

				InputStream stream = file.openInputStream(EFS.NONE, sub.newChild(20));
				isr = new InputStreamReader(stream);

				// parse
				reader.read(isr, context);
				sub.worked(50);

				// create new Window type for this file
				JSIndexReader jsir = new JSIndexReader();
				List<TypeElement> windows = jsir.getType(index, JSTypeConstants.WINDOW_TYPE, true);
				TypeElement window;

				if (windows != null && !windows.isEmpty())
				{
					window = windows.get(windows.size() - 1);
				}
				else
				{
					window = new TypeElement();
					window.setName(JSTypeConstants.WINDOW_TYPE);
				}

				// process results
				JSIndexWriter indexer = new JSIndexWriter();
				TypeElement[] types = handler.getTypes();
				AliasElement[] aliases = handler.getAliases();
				URI location = file.toURI();

				for (TypeElement type : types)
				{
					indexer.writeType(index, type, location);

					String typeName = type.getName();

					if (!typeName.contains(".") && !typeName.startsWith(JSTypeConstants.GENERIC_CLASS_OPEN)) //$NON-NLS-1$
					{
						PropertyElement property = window.getProperty(typeName);

						if (property == null)
						{
							property = new PropertyElement();

							property.setName(typeName);
							property.addType(typeName);

							window.addProperty(property);
						}
					}
				}

				for (AliasElement alias : aliases)
				{
					PropertyElement property = new PropertyElement();

					property.setName(alias.getName());
					property.addType(alias.getType());

					window.addProperty(property);
				}

				// write global type info
				indexer.writeType(index, window, location);
			}
			catch (Throwable e)
			{
				IdeLog.logError(JSPlugin.getDefault(), e);
			}
			finally
			{
				if (isr != null)
				{
					try
					{
						isr.close();
					}
					catch (IOException e) // $codepro.audit.disable emptyCatchClause
					{
					}
				}
			}
		}
		finally
		{
			sub.done();
		}
	}
}
