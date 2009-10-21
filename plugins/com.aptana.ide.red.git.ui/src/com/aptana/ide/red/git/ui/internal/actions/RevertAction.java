package com.aptana.ide.red.git.ui.internal.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import com.aptana.ide.red.git.model.ChangedFile;
import com.aptana.ide.red.git.model.GitRepository;

public class RevertAction extends StagingAction
{
	@Override
	protected void doOperation(GitRepository repo, final List<ChangedFile> changedFiles)
	{
		final Set<IResource> changedResources = new HashSet<IResource>();
		for (IResource resource : getSelectedResources())
		{
			for (ChangedFile changedFile : changedFiles)
			{
				if (resource.getLocationURI().getPath().endsWith(changedFile.getPath()))
				{
					changedResources.add(resource);
					break;
				}
			}
		}
		repo.index().discardChangesForFiles(changedFiles);
		WorkspaceJob job = new WorkspaceJob("Refresh reverted resources")
		{

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				int work = 100 * changedResources.size();
				SubMonitor sub = SubMonitor.convert(monitor, work);
				for (IResource resource : changedResources)
				{
					if (sub.isCanceled())
						return Status.CANCEL_STATUS;
					resource.refreshLocal(IResource.DEPTH_INFINITE, sub.newChild(100));
				}
				sub.done();
				return Status.OK_STATUS;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setUser(true);
		job.schedule();
	}

	@Override
	protected boolean changedFileIsValid(ChangedFile correspondingChangedFile)
	{
		return correspondingChangedFile != null && correspondingChangedFile.hasUnstagedChanges()
				&& correspondingChangedFile.getStatus() == ChangedFile.Status.MODIFIED;
	}
}
