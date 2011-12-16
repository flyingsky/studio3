/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.inferencing;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.aptana.core.util.IOUtil;
import com.aptana.editor.js.JSTypeConstants;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.ReturnTypeElement;
import com.aptana.editor.js.parsing.ast.JSParseRootNode;
import com.aptana.index.core.Index;
import com.aptana.parsing.ast.IParseNode;

/**
 * DynamicTypeInferencingTests
 */
public class DynamicTypeInferencingTests extends InferencingTestsBase
{
	public void testDynamicTypes() throws IOException, CoreException
	{
		String source = "one.two = 10;";

		File file = File.createTempFile("infer", ".js");
		file.deleteOnExit();
		IFileStore store = EFS.getLocalFileSystem().fromLocalFile(file);
		IOUtil.write(store.openOutputStream(EFS.NONE, new NullProgressMonitor()), source);

		IParseNode root = getParseRootNode(source);
		assertNotNull(root);
		assertTrue(root instanceof JSParseRootNode);

		Index index = getIndex();
		Indexer indexer = new Indexer();
		indexer.indexTree(store, source, index, (JSParseRootNode) root);

		JSIndexQueryHelper helper = new JSIndexQueryHelper();
		List<PropertyElement> globals = helper.getGlobal(index, "one");
		assertEquals(1, globals.size());
		PropertyElement global = globals.get(0);

		List<ReturnTypeElement> returnTypes = global.getTypes();
		assertEquals(1, returnTypes.size());
		ReturnTypeElement returnType = returnTypes.get(0);
		assertTrue(returnType.getType().startsWith(JSTypeConstants.DYNAMIC_CLASS_PREFIX));
	}
}
