/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license-epl.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.index.core;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.jobs.Job;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.EclipseUtil;
import com.aptana.core.util.IConfigurationElementProcessor;

abstract class IndexRequestJob extends Job
{
	/**
	 * Constants for dealing with file indexers through the extension point.
	 */
	private static final String CONTENT_TYPE_ID = "contentTypeId"; //$NON-NLS-1$
	private static final String CONTENT_TYPE_BINDING = "contentTypeBinding"; //$NON-NLS-1$
	private static final String FILE_INDEXING_PARTICIPANTS_ID = "fileIndexingParticipants"; //$NON-NLS-1$
	private static final String TAG_FILE_INDEXING_PARTICIPANT = "fileIndexingParticipant"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTR_PRIORITY = "priority"; //$NON-NLS-1$
	private static final int DEFAULT_PRIORITY = 50;

	private static final String INDEX_FILTER_PARTICIPANTS_ID = "indexFilterParticipants"; //$NON-NLS-1$
	private static final String ELEMENT_FILTER = "filter"; //$NON-NLS-1$

	private static final String FILE_CONTRIBUTORS_ID = "fileContributors"; //$NON-NLS-1$
	private static final String ELEMENT_CONTRIBUTOR = "contributor"; //$NON-NLS-1$

	private URI containerURI;
	private List<IIndexFilterParticipant> filterParticipants;
	private List<IIndexFileContributor> fileContributors;

	/**
	 * IndexRequestJob
	 * 
	 * @param name
	 * @param containerURI
	 */
	protected IndexRequestJob(String name, URI containerURI)
	{
		super(name);
		this.containerURI = containerURI;
		setRule(IndexManager.MUTEX_RULE);
		setPriority(Job.BUILD);
		// setSystem(true);
	}

	/**
	 * IndexRequestJob
	 * 
	 * @param containerURI
	 */
	protected IndexRequestJob(URI containerURI)
	{
		this(MessageFormat.format(Messages.IndexRequestJob_Name, containerURI.toString()), containerURI);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
	 */
	@Override
	public boolean belongsTo(Object family)
	{
		if (getContainerURI() == null)
		{
			return family == null;
		}
		if (family == null)
		{
			return false;
		}
		return getContainerURI().equals(family);
	}

	/**
	 * Instantiate a {@link IFileStoreIndexingParticipant} from the {@link IConfigurationElement} pointing to it via an
	 * extension.
	 * 
	 * @param key
	 * @return
	 */
	private IFileStoreIndexingParticipant createParticipant(IConfigurationElement key)
	{
		try
		{
			String priorityString = key.getAttribute(ATTR_PRIORITY);
			int priority = DEFAULT_PRIORITY;

			try
			{
				priority = Integer.parseInt(priorityString);
			}
			catch (NumberFormatException e)
			{
				IdeLog.logError(IndexPlugin.getDefault(), e);
			}

			IFileStoreIndexingParticipant result = (IFileStoreIndexingParticipant) key
					.createExecutableExtension(ATTR_CLASS);

			result.setPriority(priority);

			return result;
		}
		catch (CoreException e)
		{
			IdeLog.logError(IndexPlugin.getDefault(), e);
		}

		return null;
	}

	/**
	 * filterFileStores
	 * 
	 * @return
	 */
	protected Set<IFileStore> filterFileStores(Set<IFileStore> fileStores)
	{
		if (fileStores != null && fileStores.isEmpty() == false)
		{
			for (IIndexFilterParticipant filterParticipant : this.getFilterParticipants())
			{
				fileStores = filterParticipant.applyFilter(fileStores);
			}
		}

		return fileStores;
	}

	/**
	 * getContainerURI
	 * 
	 * @return
	 */
	protected URI getContainerURI()
	{
		return containerURI;
	}

	/**
	 * getContributedFiles
	 * 
	 * @param container
	 * @return
	 */
	protected Set<IFileStore> getContributedFiles(URI container)
	{
		Set<IFileStore> result = new HashSet<IFileStore>();

		for (IIndexFileContributor contributor : this.getFileContributors())
		{
			Set<IFileStore> files = contributor.getFiles(container);

			if (files != null && !files.isEmpty())
			{
				result.addAll(files);
			}
		}

		return result;
	}

	/**
	 * getFileContributors
	 * 
	 * @return
	 */
	private List<IIndexFileContributor> getFileContributors()
	{
		if (fileContributors == null)
		{
			fileContributors = new ArrayList<IIndexFileContributor>();

			EclipseUtil.processConfigurationElements(IndexPlugin.PLUGIN_ID, FILE_CONTRIBUTORS_ID,
					new IConfigurationElementProcessor()
					{

						public void processElement(IConfigurationElement element)
						{
							try
							{
								IIndexFileContributor participant = (IIndexFileContributor) element
										.createExecutableExtension(ATTR_CLASS);
								fileContributors.add(participant);
							}
							catch (CoreException e)
							{
								IdeLog.logError(IndexPlugin.getDefault(), e);
							}
						}

						public Set<String> getSupportElementNames()
						{
							return CollectionsUtil.newSet(ELEMENT_CONTRIBUTOR);
						}
					});
		}

		return fileContributors;
	}

	/**
	 * Return a map from classname of the participant to a set of strings for the content type ids it applies to.
	 * 
	 * @return
	 */
	private Map<IConfigurationElement, Set<IContentType>> getFileIndexingParticipants()
	{
		final Map<IConfigurationElement, Set<IContentType>> map = new HashMap<IConfigurationElement, Set<IContentType>>();
		final IContentTypeManager manager = Platform.getContentTypeManager();

		EclipseUtil.processConfigurationElements(IndexPlugin.PLUGIN_ID, FILE_INDEXING_PARTICIPANTS_ID,
				new IConfigurationElementProcessor()
				{

					public void processElement(IConfigurationElement element)
					{
						Set<IContentType> types = new HashSet<IContentType>();

						IConfigurationElement[] contentTypes = element.getChildren(CONTENT_TYPE_BINDING);
						for (IConfigurationElement contentTypeBinding : contentTypes)
						{
							String contentTypeId = contentTypeBinding.getAttribute(CONTENT_TYPE_ID);
							IContentType type = manager.getContentType(contentTypeId);
							types.add(type);
						}
						map.put(element, types);
					}

					public Set<String> getSupportElementNames()
					{
						return CollectionsUtil.newSet(TAG_FILE_INDEXING_PARTICIPANT);
					}
				});

		return map;
	}

	/**
	 * getFilterParticipants
	 * 
	 * @return
	 */
	private List<IIndexFilterParticipant> getFilterParticipants()
	{
		if (filterParticipants == null)
		{
			filterParticipants = new ArrayList<IIndexFilterParticipant>();
			EclipseUtil.processConfigurationElements(IndexPlugin.PLUGIN_ID, INDEX_FILTER_PARTICIPANTS_ID,
					new IConfigurationElementProcessor()
					{

						public void processElement(IConfigurationElement element)
						{
							try
							{
								IIndexFilterParticipant participant = (IIndexFilterParticipant) element
										.createExecutableExtension(ATTR_CLASS);
								filterParticipants.add(participant);
							}
							catch (CoreException e)
							{
								IdeLog.logError(IndexPlugin.getDefault(), e);
							}
						}

						public Set<String> getSupportElementNames()
						{
							return CollectionsUtil.newSet(ELEMENT_FILTER);
						}
					});
		}

		return filterParticipants;
	}

	/**
	 * getIndex
	 * 
	 * @return
	 */
	protected Index getIndex()
	{
		return IndexManager.getInstance().getIndex(getContainerURI());
	}

	/**
	 * hasTypes
	 * 
	 * @param store
	 * @param types
	 * @return
	 */
	protected boolean hasType(IFileStore store, Set<IContentType> types)
	{
		if (types == null || types.isEmpty())
		{
			return false;
		}
		for (IContentType type : types)
		{
			if (type == null)
			{
				continue;
			}
			if (type.isAssociatedWith(store.getName()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Indexes a set of {@link IFileStore}s with the appropriate {@link IFileStoreIndexingParticipant}s that apply to
	 * the content types (matching is done via filename/extension).
	 * 
	 * @param index
	 * @param fileStores
	 * @param monitor
	 * @throws CoreException
	 */
	protected void indexFileStores(Index index, Set<IFileStore> fileStores, IProgressMonitor monitor)
			throws CoreException
	{
		fileStores = this.filterFileStores(fileStores);

		if (index == null || fileStores == null || fileStores.isEmpty())
		{
			return;
		}

		SubMonitor sub = SubMonitor.convert(monitor, fileStores.size() * 10);
		try
		{
			// First cleanup old index entries for files
			for (IFileStore file : fileStores)
			{
				if (sub.isCanceled())
				{
					throw new CoreException(Status.CANCEL_STATUS);
				}
				index.remove(file.toURI());
				sub.worked(1);
			}

			// Now map the indexers to the files they need to/can index
			List<Map.Entry<IFileStoreIndexingParticipant, Set<IFileStore>>> toDo = mapParticipantsToFiles(fileStores);
			sub.worked(fileStores.size());

			if (!toDo.isEmpty())
			{
				// Determine work remaining
				int sum = 0;
				for (Map.Entry<IFileStoreIndexingParticipant, Set<IFileStore>> entry : toDo)
				{
					sum += entry.getValue().size();
				}
				sub.setWorkRemaining(sum);

				// Now do the indexing...
				for (Map.Entry<IFileStoreIndexingParticipant, Set<IFileStore>> entry : toDo)
				{
					IFileStoreIndexingParticipant indexer = entry.getKey();
					Set<IFileStore> files = entry.getValue();

					if (sub.isCanceled())
					{
						throw new CoreException(Status.CANCEL_STATUS);
					}
					try
					{
						indexer.index(files, index, sub.newChild(files.size()));
					}
					catch (CoreException e)
					{
						IdeLog.logError(IndexPlugin.getDefault(), e);
					}
				}
			}
		}
		finally
		{
			sub.done();
		}
	}

	/**
	 * Take the set of {@link IFileStore}s that need to be indexed, and then generate a mapping from the index
	 * participants to the IFileStores they need to index.
	 * 
	 * @param fileStores
	 * @return
	 */
	protected List<Map.Entry<IFileStoreIndexingParticipant, Set<IFileStore>>> mapParticipantsToFiles(
			Set<IFileStore> fileStores)
	{
		Map<IFileStoreIndexingParticipant, Set<IFileStore>> participantMap = new HashMap<IFileStoreIndexingParticipant, Set<IFileStore>>();
		Map<IConfigurationElement, Set<IContentType>> participants = getFileIndexingParticipants();

		for (Map.Entry<IConfigurationElement, Set<IContentType>> entry : participants.entrySet())
		{
			Set<IFileStore> filesForParticipant = new HashSet<IFileStore>();

			for (IFileStore store : fileStores)
			{
				if (hasType(store, entry.getValue()))
				{
					filesForParticipant.add(store);
				}
			}

			if (filesForParticipant.isEmpty())
			{
				continue;
			}

			IFileStoreIndexingParticipant participant = createParticipant(entry.getKey());

			if (participant != null)
			{
				participantMap.put(participant, filesForParticipant);
			}
		}

		List<Map.Entry<IFileStoreIndexingParticipant, Set<IFileStore>>> result = new ArrayList<Map.Entry<IFileStoreIndexingParticipant, Set<IFileStore>>>(
				participantMap.entrySet());

		Collections.sort(result, new Comparator<Map.Entry<IFileStoreIndexingParticipant, Set<IFileStore>>>()
		{
			public int compare(Entry<IFileStoreIndexingParticipant, Set<IFileStore>> arg0,
					Entry<IFileStoreIndexingParticipant, Set<IFileStore>> arg1)
			{
				// sort higher first
				return arg1.getKey().getPriority() - arg0.getKey().getPriority();
			}
		});

		return result;
	}
}
