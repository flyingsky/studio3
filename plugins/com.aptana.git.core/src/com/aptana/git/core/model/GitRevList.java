/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.git.core.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import com.aptana.core.logging.IdeLog;
import com.aptana.git.core.GitPlugin;
import com.aptana.git.core.IDebugScopes;

public class GitRevList
{
	private GitRepository repository;
	private List<GitCommit> commits;

	private static final int NO_LIMIT = -1;

	public GitRevList(GitRepository repo)
	{
		repository = repo;
	}

	// TODO This seems an odd model. Maybe we hide it and hang a method off the repo?
	/**
	 * Walks a revision to collect all the commits in reverse chronological order.
	 * 
	 * @param gitRevSpecifier
	 */
	public IStatus walkRevisionListWithSpecifier(GitRevSpecifier gitRevSpecifier, IProgressMonitor monitor)
	{
		return walkRevisionListWithSpecifier(gitRevSpecifier, NO_LIMIT, monitor);
	}

	/**
	 * Walks a revision to collect commits in reverse chronological order, limited to value of max results.
	 * 
	 * @param rev
	 * @param max
	 *            Maximum number of results to return. {@link #NO_LIMIT} represent no limit.
	 */
	public IStatus walkRevisionListWithSpecifier(GitRevSpecifier rev, int max, IProgressMonitor monitor)
	{
		int units = max;
		if (units == -1)
		{
			// If unknown, just use some huge fake number so that some progress is shown...
			units = 100000;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, units);
		long start = System.currentTimeMillis();
		List<GitCommit> revisions = new ArrayList<GitCommit>();

		String formatString = "--pretty=format:%H\01%e\01%an\01%ae\01%s\01%b\01%P\01%at"; //$NON-NLS-1$
		boolean showSign = ((rev == null) ? false : rev.hasLeftRight());
		if (showSign)
			formatString += "\01%m"; //$NON-NLS-1$

		List<String> arguments = new ArrayList<String>();
		arguments.add("log"); //$NON-NLS-1$
		arguments.add("-z"); //$NON-NLS-1$
		arguments.add("--early-output"); //$NON-NLS-1$
		arguments.add("--topo-order"); //$NON-NLS-1$
		arguments.add("--children"); //$NON-NLS-1$
		if (max > 0)
		{
			arguments.add("-" + max); // only last N revs //$NON-NLS-1$
		}
		arguments.add(formatString);

		if (rev == null)
		{
			arguments.add(GitRepository.HEAD);
		}
		else
		{
			arguments.addAll(rev.parameters());
		}

		IPath directory = repository.workingDirectory();

		if (subMonitor.isCanceled())
		{
			return Status.CANCEL_STATUS;
		}
		try
		{
			// FIXME Move this into GitRepository, so we can set up lock/monitor on it!
			repository.enterRead();
			Process p = GitExecutable.instance().run(directory, arguments.toArray(new String[arguments.size()]));
			InputStream stream = p.getInputStream();

			int num = 0;
			while (true)
			{
				if (subMonitor.isCanceled())
				{
					return Status.CANCEL_STATUS;
				}

				String sha = getline(stream, '\1');
				if (sha == null)
				{
					break;
				}

				// We reached the end of some temporary output. Show what we have
				// until now, and then start again. The sha of the next thing is still
				// in this buffer. So, we use a substring of current input.
				if (sha.charAt(1) == 'i') // Matches 'Final output'
				{
					num = 0;
					setCommits(revisions);
					revisions = new ArrayList<GitCommit>();

					// If the length is < 40, then there are no commits.. quit now
					if (sha.length() < 40)
					{
						break;
					}

					int startIndex = sha.length() - 40;
					sha = sha.substring(startIndex, startIndex + 40);
				}

				String encoding = getline(stream, '\1', "UTF-8"); //$NON-NLS-1$
				GitCommit newCommit = new GitCommit(repository, sha);

				String author = getline(stream, '\1', encoding);
				String authorEmail = getline(stream, '\1', encoding);
				String subject = getline(stream, '\1', encoding);
				String body = getline(stream, '\1', encoding);
				String parentString = getline(stream, '\1');
				if (parentString != null && parentString.length() != 0)
				{
					if (((parentString.length() + 1) % 41) != 0)
					{
						IdeLog.logError(GitPlugin.getDefault(),
								MessageFormat.format("invalid parents: {0}", parentString.length()), IDebugScopes.DEBUG); //$NON-NLS-1$
						continue;
					}
					int nParents = (parentString.length() + 1) / 41;
					List<String> parents = new ArrayList<String>(nParents);
					for (int parentIndex = 0; parentIndex < nParents; ++parentIndex)
					{
						int stringIndex = parentIndex * 41;
						parents.add(parentString.substring(stringIndex, stringIndex + 40));
					}

					newCommit.setParents(parents);
				}

				long time = readLong(stream); // read 10 chars as a string and parse into a long

				newCommit.setSubject(subject);
				newCommit.setComment(body);
				newCommit.setAuthor(author);
				newCommit.setAuthorEmail(authorEmail);
				newCommit.setTimestamp(time);

				if (showSign)
				{
					stream.read(); // Remove separator
					char c = (char) stream.read();
					if (c != '>' && c != '<' && c != '^' && c != '-')
					{
						IdeLog.logError(GitPlugin.getDefault(),
								"Error loading commits: sign not correct", IDebugScopes.DEBUG); //$NON-NLS-1$
						// newCommit.setSign(c);
					}

				}

				int read = stream.read();
				if (read != 0 && read != -1)
				{
					IdeLog.logError(GitPlugin.getDefault(), "Error", IDebugScopes.DEBUG); //$NON-NLS-1$
				}

				revisions.add(newCommit);

				subMonitor.worked(1);

				if (read == -1)
				{
					break;
				}

				if (++num % 1000 == 0)
				{
					setCommits(revisions);
				}
			}

			long duration = System.currentTimeMillis() - start;
			logInfo(MessageFormat.format("Loaded {0} commits in {1} ms", num, duration)); //$NON-NLS-1$
			// Make sure the commits are stored before exiting.
			setCommits(revisions, true);
			p.waitFor();
		}
		catch (Exception e)
		{
			return new Status(IStatus.ERROR, GitPlugin.getPluginId(), e.getMessage(), e);
		}
		finally
		{
			repository.exitRead();
			subMonitor.done();
		}
		return Status.OK_STATUS;
	}

	private void logInfo(String string)
	{
		if (GitPlugin.getDefault() != null)
		{
			IdeLog.logInfo(GitPlugin.getDefault(), string);
		}
		else
		{
			System.out.println(string);
		}
	}

	private long readLong(InputStream stream)
	{
		StringBuilder builder = new StringBuilder();
		while (true)
		{
			try
			{
				int read = stream.read();
				if (read == -1)
				{
					break;
				}
				builder.append((char) read);
				if (builder.length() == 10)
				{
					break;
				}
			}
			catch (IOException e)
			{
				break;
			}
		}
		// Since we get time in seconds since epoch, not ms we need to multiply by 1000
		long time = Long.parseLong(builder.toString()) * 1000;
		// HACK for some reason my times are 5 minutes off the console/GitX. Adjust 5 mins
		return time + (5 * 60 * 1000);
	}

	private void setCommits(List<GitCommit> revisions)
	{
		setCommits(revisions, false);
	}

	private void setCommits(List<GitCommit> revisions, boolean trimToSize)
	{
		if (trimToSize)
		{
			if (revisions instanceof ArrayList<?>)
			{
				((ArrayList<?>) revisions).trimToSize();
			}
			this.commits = new ArrayList<GitCommit>(revisions);
			revisions.clear();
			revisions = null;
		}
		else
		{
			this.commits = revisions;
		}
	}

	private String getline(InputStream stream, char c)
	{
		byte[] bytes = read(stream, c);
		if (bytes == null || bytes.length == 0)
		{
			return null;
		}
		return new String(bytes);
	}

	private String getline(InputStream stream, char c, String encoding) throws UnsupportedEncodingException
	{
		if (encoding == null || encoding.length() == 0)
		{
			return getline(stream, c);
		}
		byte[] bytes = read(stream, c);
		return new String(bytes, encoding);
	}

	private byte[] read(InputStream stream, char c)
	{
		List<Byte> list = new ArrayList<Byte>();
		while (true)
		{
			try
			{
				int read = stream.read();
				if (read == -1)
				{
					break;
				}
				char readC = (char) read;
				if (readC == c)
				{
					break;
				}
				list.add((byte) read);
			}
			catch (IOException e)
			{
				break;
			}
		}
		byte[] bytes = new byte[list.size()];
		int i = 0;
		for (Byte by : list)
		{
			bytes[i++] = by.byteValue();
		}
		return bytes;
	}

	public List<GitCommit> getCommits()
	{
		return Collections.unmodifiableList(this.commits);
	}
}
