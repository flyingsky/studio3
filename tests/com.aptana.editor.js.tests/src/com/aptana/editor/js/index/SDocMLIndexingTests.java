/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.index;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.aptana.editor.js.contentassist.index.SDocMLFileIndexingParticipant;
import com.aptana.editor.js.tests.JSEditorBasedTests;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;

/**
 * SDocMLIndexingTests
 */
public class SDocMLIndexingTests extends JSEditorBasedTests
{
	class Indexer extends SDocMLFileIndexingParticipant
	{
		public void index(Index index, IFileStore file)
		{
			indexFileStore(index, file, new NullProgressMonitor());
		}
	}

	protected void indexAndCheckProposals(String indexResource, String fileResource, String... proposals)
	{
		URI uri = null;

		try
		{
			// create IFileStore for indexing
			IFileStore indexFile = getFileStore(indexResource);

			// grab source file URI
			IFileStore sourceFile = getFileStore(fileResource);
			uri = sourceFile.toURI();

			// create index for file
			Index index = IndexManager.getInstance().getIndex(uri);
			Indexer indexer = new Indexer();

			// index file
			indexer.index(index, indexFile);

			// check proposals
			checkProposals(fileResource, proposals);
		}
		catch (Throwable t)
		{
			fail(t.getMessage());
		}
		finally
		{
			if (uri != null)
			{
				IndexManager.getInstance().removeIndex(uri);
			}
		}
	}

	public void assertStaticProperties_1_6_2(String fileResource)
	{
		// @formatter:off
		indexAndCheckProposals(
			"sdocml/jquery.1.6.2.sdocml",
			fileResource,
			"ajax",
			"ajaxPrefilter",
			"ajaxSetup",
			"boxModel",
			"browser",
			"contains",
			"cssHooks",
			"data",
			"dequeue",
			"each",
			"error",
			"extend",
			"get",
			"getJSON",
			"getScript",
			"globalEval",
			"grep",
			"hasData",
			"holdReady",
			"inArray",
			"isArray",
			"isEmptyObject",
			"isFunction",
			"isPlainObject",
			"isWindow",
			"isXMLDoc",
			"makeArray",
			"map",
			"merge",
			"noConflict",
			"noop",
			"now",
			"param",
			"parseJSON",
			"parseXML",
			"post",
			"proxy",
			"queue",
			"removeData",
			"sub",
			"support",
			"trim",
			"type",
			"unique",
			"when"
		);
		// @formatter:on
	}

	public void assertInstanceProperties_1_6_2(String fileResource)
	{
		// @formatter:off
		indexAndCheckProposals(
			"sdocml/jquery.1.6.2.sdocml",
			fileResource,
			"add",
			"addClass",
			"after",
			"ajaxComplete",
			"ajaxError",
			"ajaxSend",
			"ajaxStart",
			"ajaxStop",
			"ajaxSuccess",
			"andSelf",
			"animate",
			"append",
			"appendTo",
			"attr",
			"before",
			"bind",
			"blur",
			"change",
			"children",
			"clearQueue",
			"click",
			"clone",
			"closest",
			"contents",
			"context",
			"css",
			"data",
			"dblclick",
			"delay",
			"delegate",
			"dequeue",
			"detach",
			"die",
			"each",
			"empty",
			"end",
			"eq",
			"error",
			"fadeIn",
			"fadeOut",
			"fadeTo",
			"fadeToggle",
			"filter",
			"find",
			"first",
			"focus",
			"focusin",
			"focusout",
			"get",
			"has",
			"hasClass",
			"height",
			"hide",
			"hover",
			"html",
			"index",
			"innerHeight",
			"innerWidth",
			"insertAfter",
			"insertBefore",
			"is",
			"jquery",
			"keydown",
			"keypress",
			"keyup",
			"last",
			"length",
			"live",
			"load",
			"map",
			"mousedown",
			"mouseenter",
			"mouseleave",
			"mousemove",
			"mouseout",
			"mouseover",
			"mouseup",
			"next",
			"nextAll",
			"nextUntil",
			"not",
			"offset",
			"offsetParent",
			"one",
			"outerHeight",
			"outerWidth",
			"parent",
			"parents",
			"parentsUntil",
			"position",
			"prepend",
			"prependTo",
			"prev",
			"prevAll",
			"prevUntil",
			"promise",
			"prop",
			"pushStack",
			"queue",
			"ready",
			"remove",
			"removeAttr",
			"removeClass",
			"removeData",
			"removeProp",
			"replaceAll",
			"replaceWith",
			"resize",
			"scroll",
			"scrollLeft",
			"scrollTop",
			"select",
			"serialize",
			"serializeArray",
			"show",
			"siblings",
			"size",
			"slice",
			"slideDown",
			"slideToggle",
			"slideUp",
			"stop",
			"submit",
			"text",
			"toArray",
			"toggle",
			"toggleClass",
			"trigger",
			"triggerHandler",
			"unbind",
			"undelegate",
			"unload",
			"unwrap",
			"val",
			"width",
			"wrap",
			"wrapAll",
			"wrapInner"
		);
		// @formatter:on
	}

	public void testJQuerySymbolStatics_1_6_2()
	{
		assertStaticProperties_1_6_2("sdocml/jQuery-statics.js");
	}

	public void testDollarSymbolStatics_1_6_2()
	{
		assertStaticProperties_1_6_2("sdocml/$-statics.js");
	}

	public void testJQueryAddReturnValueProperties()
	{
		assertInstanceProperties_1_6_2("sdocml/jQuery-add-properties.js");
	}

	public void testDollarAddReturnValueProperties()
	{
		assertInstanceProperties_1_6_2("sdocml/$-add-properties.js");
	}

	// @formatter:off
	// Commented out ATM, as the following test fail. Attached to ticket APSTUD-3389
	/*
	public void testDollarJQXHR()
	{
		indexAndCheckProposals(
			"sdocml/jquery.1.6.2.sdocml",
			"sdocml/$-jqXHR.js",
			"readyState", // Properties
			"responseText",
			"responseXML",
			"status",
			"statusText",
			"overrideMimeType", //jqXHR methods
			"abort",
			"getAllResponseHeaders",
			"getResponseHeader",
			"setRequestHeader",
			"pipe", // Deferred methods
			"always",
			"promise",
			"fail",
			"done",
			"then",
			"isRejected",
			"isResolved"
		);
	}

	public void testDollarDeferred()
	{
		indexAndCheckProposals(
			"sdocml/jquery.1.6.2.sdocml",
			"sdocml/$-Deferred.js",
			"pipe", // Deferred methods
			"always",
			"promise",
			"resolveWith",
			"rejectWith",
			"fail",
			"done",
			"then",
			"reject",
			"isRejected",
			"isResolved",
			"resolve"
		);
	}

	public void testPromise()
	{
		// @formatter:off
		indexAndCheckProposals(
			"sdocml/jquery.1.6.2.sdocml",
			"sdocml/Promise.js",
			"pipe", // Promise methods
			"always",
			"promise",
			"fail",
			"done",
			"then",
			"isRejected",
			"isResolved"
		);
	}
	*/
	// @formatter:on

}
