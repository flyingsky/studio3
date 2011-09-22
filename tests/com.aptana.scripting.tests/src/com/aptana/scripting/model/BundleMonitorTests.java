/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.scripting.model;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aptana.scripting.TestUtils;

public abstract class BundleMonitorTests extends TestCase
{
	public interface FileSystemAction
	{
		void performAction() throws Exception;
	}
	
	private static final int POST_SLEEP = 5000;
	
	public static final String BUNDLE_NAME = "TestBundle";
	public static final String BUNDLE_FILE_NAME = "bundle.rb";
	public static final String COMMAND_NAME = "MyCommand";
	public static final String SNIPPET_NAME = "MySnippet";
	public static final String LIB_NAME = "my_lib";

	private BundleFileSystemService _fileSystemService;
	private BundleManager _manager;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		// setup test bundle
		this._fileSystemService = new BundleFileSystemService(this.createFileSystem());
		this._fileSystemService.createBundleDirectory();

		// store reference to bundle manager
		this._manager = BundleManager.getInstance();
		this._manager.reset();

		// setup application and user bundles paths
		List<String> applicationBundlesPaths = this._manager.getApplicationBundlesPaths();
		String userBundlesPath = new File(new File(System.getProperty("java.io.tmpdir")), "bundles").getAbsolutePath();
		BundleManager.getInstance(applicationBundlesPaths.get(0), userBundlesPath);

		// monitoring is turned on by an early startup job, so let's make sure it
		// has been turned off before turning it on; otherwise, we'll end up
		// monitoring the default application and user bundles directories
		// instead of our custom paths
		BundleMonitor.getInstance().endMonitoring();

		// turn on monitoring
		BundleMonitor.getInstance().beginMonitoring();
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.scripting.model.BundleTestBase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		try
		{
			BundleMonitor.getInstance().endMonitoring();
			this._fileSystemService.cleanUp();
		}
		finally
		{
			super.tearDown();
		}
	}

	/**
	 * createBundleDirectoryManager
	 * 
	 * @return
	 */
	protected abstract IBundleFileSystem createFileSystem();

	/**
	 * waitForAction
	 */
	protected abstract void waitForAction(FileSystemAction action) throws Exception;

	/**
	 * getFileContent
	 * 
	 * @param filename
	 * @return
	 */
	protected String getFileContent(String filename)
	{
		IPath path = new Path("monitor-files").append(filename);
		File file = TestUtils.getFile(path);

		return TestUtils.getContent(file);
	}

	/**
	 * addBundleFile
	 * 
	 * @param filename
	 * @throws Exception
	 */
	protected void addBundleFile(String filename) throws Exception
	{
		final String content = this.getFileContent(filename);

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.createBundleFile(content);

				assertTrue(_fileSystemService.bundleFileExists());
			}
		});
	}

	/**
	 * addCommand
	 * 
	 * @param filename
	 * @throws Exception
	 */
	protected void addCommand(String filename) throws Exception
	{
		final String content = this.getFileContent(filename);

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.createCommand(content);

				assertTrue(_fileSystemService.commandExists());
			}
		});
	}

	/**
	 * addLib
	 * 
	 * @param filename
	 * @throws Exception
	 */
	protected void addLib(String filename) throws Exception
	{
		final String content = this.getFileContent(filename);

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.createLib(content);

				assertTrue(_fileSystemService.libExists());
			}
		});
	}

	/**
	 * addSnippet
	 * 
	 * @param filename
	 * @throws Exception
	 */
	protected void addSnippet(String filename) throws Exception
	{
		final String content = this.getFileContent(filename);

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.createSnippet(content);

				assertTrue(_fileSystemService.snippetExists());
			}
		});
	}

	/**
	 * removeBundleDirectory
	 * 
	 * @throws Exception
	 */
	protected void removeBundleDirectory() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteBundleDirectory();

				assertFalse(_fileSystemService.bundleDirectoryExists());
			}
		});
	}

	/**
	 * removeBundleFile
	 * 
	 * @throws Exception
	 */
	protected void removeBundleFile() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteBundleFile();

				assertFalse(_fileSystemService.bundleFileExists());
			}
		});
	}

	/**
	 * removeCommand
	 * 
	 * @throws Exception
	 */
	protected void removeCommand() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteCommand();

				assertFalse(_fileSystemService.commandExists());
			}
		});
	}

	/**
	 * removeCommandsDirectory
	 * 
	 * @throws Exception
	 */
	protected void removeCommandsDirectory() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteCommandsDirectory();

				assertFalse(_fileSystemService.commandsDirectoryExists());
			}
		});
	}

	/**
	 * removeLib
	 * 
	 * @throws Exception
	 */
	protected void removeLib() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteLib();

				assertFalse(_fileSystemService.libExists());
			}
		});
	}

	/**
	 * removeLibDirectory
	 * 
	 * @throws Exception
	 */
	protected void removeLibDirectory() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteLibDirectory();

				assertFalse(_fileSystemService.libDirectoryExists());
			}
		});
	}

	/**
	 * removeSnippet
	 * 
	 * @throws Exception
	 */
	protected void removeSnippet() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteSnippet();

				assertFalse(_fileSystemService.snippetExists());
			}
		});
	}

	/**
	 * removeSnippetsDirectory
	 * 
	 * @throws Exception
	 */
	protected void removeSnippetsDirectory() throws Exception
	{
		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.deleteSnippetsDirectory();

				assertFalse(_fileSystemService.snippetsDirectoryExists());
			}
		});
	}

	/**
	 * testAddBundleFile
	 * 
	 * @throws Exception
	 */
	public void testAddBundleFile() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
	}

	/**
	 * testAddCommandAfterBundleFile
	 * 
	 * @throws Exception
	 */
	public void testAddCommandAfterBundleFile() throws Exception
	{
		// create bundle
		this.addBundleFile("simple-bundle.rb");

		// make sure it created a bundle entry
		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		// now add a command
		this.addCommand("simple-command.rb");

		// and make sure that shows up
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
	}

	/**
	 * testAddCommandBeforeBundleFile
	 * 
	 * @throws Exception
	 */
	public void testAddCommandBeforeBundleFile() throws Exception
	{
		// add a command to an invalid bundle directory
		this.addCommand("simple-command.rb");

		// there should be no entry
		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);

		// now add the bundle file to make it valid
		this.addBundleFile("simple-bundle.rb");

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		// we should see the command now too
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
	}

	/**
	 * testAddSnippetAfterBundleFile
	 * 
	 * @throws Exception
	 */
	public void testAddSnippetAfterBundleFile() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");

		// make sure it created a bundle entry
		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		this.addSnippet("simple-snippet.rb");

		// and make sure that shows up
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
	}

	/**
	 * testAddSnippetBeforeBundleFile
	 * 
	 * @throws Exception
	 */
	public void testAddSnippetBeforeBundleFile() throws Exception
	{
		this.addSnippet("simple-snippet.rb");

		// make sure it created a bundle entry
		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);

		this.addBundleFile("simple-bundle.rb");

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		// we should see the snippet now too
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
	}

	/**
	 * testRemoveCommand
	 * 
	 * @throws Exception
	 */
	public void testRemoveCommand() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");

		// make sure it created a bundle entry an a command
		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());

		this.removeCommand();

		// we should no commands now
		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(0, commands.size());
	}

	/**
	 * testRemoveCommandNoBundleFile
	 * 
	 * @throws Exception
	 */
	public void testRemoveCommandNoBundleFile() throws Exception
	{
		this.addCommand("simple-command.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);

		this.removeCommand();

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}

	/**
	 * testRemoveSnippet
	 * 
	 * @throws Exception
	 */
	public void testRemoveSnippet() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addSnippet("simple-snippet.rb");

		// make sure it created a bundle entry an a command
		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());

		this.removeSnippet();

		// we should no commands now
		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(0, commands.size());
	}

	/**
	 * testRemoveSnippetNoBundleFile
	 * 
	 * @throws Exception
	 */
	public void testRemoveSnippetNoBundleFile() throws Exception
	{
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);

		this.removeSnippet();

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}

	/**
	 * testRemoveBundleFile
	 * 
	 * @throws Exception
	 */
	public void testRemoveBundleFileWithoutMembers() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		this.removeBundleFile();

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}

	/**
	 * testRemoveBundleFileBeforeMembers
	 * 
	 * @throws Exception
	 */
	public void testRemoveBundleFileBeforeMembers() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		this.removeBundleFile();

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}

	/**
	 * testRemoveBundleFileAfterMembers
	 * 
	 * @throws Exception
	 */
	public void testRemoveBundleFileAfterMembers() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		this.removeCommand();
		this.removeSnippet();
		this.removeBundleFile();

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}

	/**
	 * testRemoveBundleDirectory
	 * 
	 * @throws Exception
	 */
	public void testRemoveBundleDirectory() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);

		this.removeBundleDirectory();

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}

	/**
	 * testRemoveCommandsDirectory
	 * 
	 * @throws Exception
	 */
	public void testRemoveCommandsDirectory() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());

		this.removeCommandsDirectory();
		
		Thread.sleep(POST_SLEEP);

		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(0, commands.size());
	}

	/**
	 * testRemoveSnippetsDirectory
	 * 
	 * @throws Exception
	 */
	public void testRemoveSnippetsDirectory() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());

		this.removeSnippetsDirectory();
		
		Thread.sleep(POST_SLEEP);

		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(0, commands.size());
	}

	/**
	 * testRenameCommand
	 * 
	 * @throws Exception
	 */
	public void testRenameCommand() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.moveCommand(COMMAND_NAME + "2.rb");
			}
		});

		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals(COMMAND_NAME + "2.rb", new File(commands.get(0).getPath()).getName());
	}

	/**
	 * testRenameSnippet
	 * 
	 * @throws Exception
	 */
	public void testRenameSnippet() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.moveSnippet(SNIPPET_NAME + "2.rb");
			}
		});

		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals(SNIPPET_NAME + "2.rb", new File(commands.get(0).getPath()).getName());
	}

	/**
	 * testRenameBundleFile
	 * 
	 * @throws Exception
	 */
	public void testRenameBundleFile() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(2, commands.size());

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.moveBundleFile("someName.rb");
			}
		});

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}

	/**
	 * testRenameBundleDirectory
	 * 
	 * @throws Exception
	 */
	public void testRenameBundleDirectory() throws Exception
	{
		this.addBundleFile("simple-bundle.rb");
		this.addCommand("simple-command.rb");
		this.addSnippet("simple-snippet.rb");

		BundleEntry entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(2, commands.size());

		this.waitForAction(new FileSystemAction()
		{
			public void performAction() throws Exception
			{
				_fileSystemService.moveBundleDirectory(BUNDLE_NAME + "2");
			}
		});

		entry = this._manager.getBundleEntry(BUNDLE_NAME);
		assertNull(entry);
	}
}
