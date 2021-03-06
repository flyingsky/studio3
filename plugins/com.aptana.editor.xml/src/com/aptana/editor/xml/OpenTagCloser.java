/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.xml;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;

import com.aptana.core.logging.IdeLog;

@SuppressWarnings("nls")
public class OpenTagCloser implements VerifyKeyListener
{

	private ITextViewer textViewer;

	public OpenTagCloser(ITextViewer textViewer)
	{
		this.textViewer = textViewer;
	}

	public void install()
	{
		textViewer.getTextWidget().addVerifyKeyListener(this);
	}

	public void verifyKey(VerifyEvent event)
	{
		// early pruning to slow down normal typing as little as possible
		if (!isAutoInsertEnabled() || !isAutoInsertCharacter(event.character))
		{
			return;
		}

		IDocument document = textViewer.getDocument();
		final Point selection = textViewer.getSelectedRange();
		int offset = selection.x;
		final int length = selection.y;

		try
		{
			// Special logic for "overtyping" '>' character
			boolean nextIsLessThan = false;
			if (offset < document.getLength())
			{
				// if char at cursor is '>'...
				char c = document.getChar(offset);
				if (c == '>')
				{
					char b = document.getChar(offset - 1);
					if (b != '%' && b != '?')
					{
						// And we're not closing an ERB/PHP tag, overwrite the '>'
						nextIsLessThan = true;
						event.doit = false;
						textViewer.setSelectedRange(offset + 1, 0);
					}
					else
					{
						// And we are closing an ERB/PHP tag, check to see if it's "?|>>", if so overwrite '>'
						if (offset + 1 < document.getLength())
						{
							char d = document.getChar(offset + 1);
							if (d == '>')
							{
								nextIsLessThan = true;
								event.doit = false;
								textViewer.setSelectedRange(offset + 1, 0);
							}
						}
					}
				}
			}

			// give a chance to exit early
			if (!shouldAutoClose(document, offset, event))
			{
				return;
			}

			String openTag = getOpenTag(document, offset);
			if (openTag == null || skipOpenTag(openTag))
			{
				return;
			}

			String closeTag = getMatchingCloseTag(openTag);
			if (closeTag == null)
			{
				return;
			}

			final StringBuffer buffer = new StringBuffer();
			// check if the char already exists next in doc! This is the special case of when we auto-paired the '<>' in
			// linked mode...
			boolean overwrite = openTag.endsWith(">>");
			if (nextIsLessThan)
			{
				overwrite = true;
				offset++;
			}
			if (!overwrite)
			{
				buffer.append(event.character);
			}

			// Check to see if this tag is already closed...
			IDocument copy = new Document(document.get());
			copy.replace(offset, length, buffer.toString());
			if (TagUtil.tagClosed(copy, openTag))
			{
				return;
			}

			buffer.append(closeTag);
			document.replace(offset, length, buffer.toString());
			if (nextIsLessThan || overwrite)
			{
				// move cursor one?
				textViewer.setSelectedRange(offset, 0);
			}
			else
			{
				textViewer.setSelectedRange(offset + 1, 0);
			}
			event.doit = false;
		}
		catch (BadLocationException e)
		{
			XMLPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, XMLPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	/**
	 * Allows quick return if we happen to be in a partition where we don't want auto-closed tags. Currently will only
	 * auto-close when in HTML partitions.
	 * 
	 * @param document
	 * @param offset
	 * @param event
	 * @return
	 */
	protected boolean shouldAutoClose(IDocument document, int offset, VerifyEvent event)
	{
		// Only auto-close XML Tags
		ITypedRegion partition = document.getDocumentPartitioner().getPartition(offset - 1);
		if (partition != null)
		{
			if (!validPartition(partition))
			{
				return false;
			}
			try
			{
				int length = Math.min(partition.getLength(), offset - partition.getOffset());
				String tagContents = document.get(partition.getOffset(), length);
				return !inString(tagContents, length);
			}
			catch (BadLocationException e)
			{
				IdeLog.logError(XMLPlugin.getDefault(), e);
			}
		}
		return false;
	}

	protected boolean validPartition(ITypedRegion partition)
	{
		return XMLSourceConfiguration.TAG.equals(partition.getType());
	}

	private boolean inString(String tagContents, int length)
	{
		boolean inString = false;
		for (int i = 0; i < length; i++)
		{
			char c = tagContents.charAt(i);
			switch (c)
			{
				case '\\':
					i++; // skip next char
					break;
				case '\'':
				case '"':
					// FIXME Handle storing opening string char and only toggle this when it makes sense!
					inString = !inString;
					break;
				default:
					break;
			}
		}
		return inString;
	}

	protected boolean skipOpenTag(String openTag)
	{
		return openTag == null || openTag.startsWith("<!") || openTag.startsWith("<?") || openTag.startsWith("<%");
	}

	private String getMatchingCloseTag(String openTag)
	{
		int index = openTag.indexOf(' ');
		if (index == -1)
		{
			index = openTag.indexOf('>');
		}
		String closeTag = "</" + openTag.substring(1, index);
		if (!closeTag.endsWith(">"))
		{
			closeTag += ">";
		}
		return closeTag;
	}

	private String getOpenTag(IDocument document, int offset) throws BadLocationException
	{
		ITypedRegion partition = document.getPartition(offset - 1);
		int length = Math.min(partition.getLength(), offset - partition.getOffset());
		String tagContents = document.get(partition.getOffset(), length);

		// Find last '<' not in a string
		int lessThanIndex = 0;
		boolean inString = false;
		for (int i = 0; i < length; i++)
		{
			char c = tagContents.charAt(i);
			switch (c)
			{
				case '<':
					if (!inString)
					{
						lessThanIndex = i;
					}
					break;
				case '\\':
					if (inString)
					{
						i++; // skip next char
					}
					break;
				case '\'':
				case '"':
					// FIXME Handle storing opening string char and only toggle this when it makes sense!
					inString = !inString;
					break;
				default:
					break;
			}
		}
		tagContents = tagContents.substring(lessThanIndex);

		if (tagContents.length() > 0 && tagContents.charAt(0) == '<')
		{
			tagContents = tagContents.substring(1);
		}
		// If it ends in a slash, we probably can just return null because it's self-closed!
		if (tagContents.length() > 0 && tagContents.charAt(tagContents.length() - 1) == '/')
		{
			return null;
		}
		String tagName = tagContents.trim();
		// Modify tag for some tag name checks
		int spaceIndex = tagName.indexOf(' ');
		if (spaceIndex != -1)
		{
			tagName = tagName.substring(0, spaceIndex);
		}
		String toCheck = tagName;
		if (toCheck.endsWith(">"))
		{
			toCheck = toCheck.substring(0, toCheck.length() - 1);
		}
		// Don't close self-closing tags, or tags with no tag name in them
		if (tagContents.length() == 0 || toCheck.charAt(0) == '/')
		{
			return null;
		}
		// Check to see if this tag type is one that self-closes by HTML definition based on doctype.
		if (isEmptyTagType(document, toCheck))
		{
			return null;
		}
		// Return a not necessarily good tag. May contain attrs and an additional ">", but we rely on that later...
		return new String("<" + tagName + ">");
	}

	protected boolean isEmptyTagType(IDocument doc, String tagName)
	{
		// FIXME Check DTD or whatever to see if tag is a self-closing one for this XML doc!
		return false;
	}

	protected boolean isAutoInsertCharacter(char character)
	{
		return character == '>';
	}

	protected boolean isAutoInsertEnabled()
	{
		return true;
	}

	protected ITextViewer getTextViewer()
	{
		return textViewer;
	}
}
