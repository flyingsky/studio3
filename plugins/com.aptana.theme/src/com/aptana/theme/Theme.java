/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.theme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.EclipseUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.scope.IScopeSelector;
import com.aptana.scope.ScopeSelector;
import com.aptana.theme.internal.OrderedProperties;
import com.aptana.theme.internal.ThemeManager;

/**
 * Reads in the theme from a java properties file. Intentionally similar to the Textmate themes. keys are token types,
 * values are comma delimited with hex colors and font style keywords. First hex color becomes FG, second becomes BG (if
 * there).
 * 
 * @author cwilliams
 */
public class Theme
{

	/**
	 * Delimiter used to append the scope selector after the fg/bg/fontStyle for a rule.
	 */
	static final String SELECTOR_DELIMITER = "^"; //$NON-NLS-1$

	static final String DELIMETER = ","; //$NON-NLS-1$

	private static final String UNDERLINE = "underline"; //$NON-NLS-1$
	private static final String BOLD = "bold"; //$NON-NLS-1$
	private static final String ITALIC = "italic"; //$NON-NLS-1$

	public static final String THEME_NAME_PROP_KEY = "name"; //$NON-NLS-1$
	public static final String THEME_EXTENDS_PROP_KEY = "extends_theme"; //$NON-NLS-1$
	static final String FOREGROUND_PROP_KEY = "foreground"; //$NON-NLS-1$
	private static final String BACKGROUND_PROP_KEY = "background"; //$NON-NLS-1$
	private static final String SELECTION_PROP_KEY = "selection"; //$NON-NLS-1$
	private static final String LINE_HIGHLIGHT_PROP_KEY = "lineHighlight"; //$NON-NLS-1$
	private static final String CARET_PROP_KEY = "caret"; //$NON-NLS-1$

	private List<ThemeRule> coloringRules;
	private ColorManager colorManager;
	private RGB defaultFG;
	private RGBa lineHighlight;
	private RGB defaultBG;
	private RGBa selection;
	private RGB caret;
	private String name;

	private RGB searchResultBG;

	/**
	 * Used for recursion in getDelayedTextAttribute to avoid matching same rule on scope twice
	 */
	private IScopeSelector lastSelectorMatch;

	/**
	 * A cache to memoize the ultimate TextAttribute generated for a given fully qualified scope.
	 */
	private Map<String, TextAttribute> cache;

	public Theme(ColorManager colormanager, Properties props)
	{
		this.colorManager = colormanager;
		coloringRules = new ArrayList<ThemeRule>();
		cache = new HashMap<String, TextAttribute>();
		parseProps(props);
		storeDefaults();
	}

	private void parseProps(Properties props)
	{
		name = (String) props.remove(THEME_NAME_PROP_KEY);
		if (name == null)
		{
			// Log the properties
			String properties = StringUtil.EMPTY;
			PrintWriter pw = null;
			try
			{
				StringWriter sw = new StringWriter(); // $codepro.audit.disable closeWhereCreated
				pw = new PrintWriter(sw);
				props.list(pw);
				properties = sw.toString();
			}
			catch (Exception e)
			{
				// ignore
			}
			finally
			{
				if (pw != null)
				{
					pw.close();
				}
			}
			throw new IllegalStateException(
					"Invalid theme properties. No theme 'name' provided. Properties may be corrupted: " + properties); //$NON-NLS-1$
		}
		// The general editor colors
		// FIXME Add fallback rgb colors to use! black on white, etc.
		defaultFG = parseHexRGB((String) props.remove(FOREGROUND_PROP_KEY));
		defaultBG = parseHexRGB((String) props.remove(BACKGROUND_PROP_KEY));
		lineHighlight = parseHexRGBa((String) props.remove(LINE_HIGHLIGHT_PROP_KEY));
		selection = parseHexRGBa((String) props.remove(SELECTION_PROP_KEY));
		caret = parseHexRGB((String) props.remove(CARET_PROP_KEY), true);

		Set<Object> propertyNames = props.keySet();
		for (Object key : propertyNames)
		{
			String displayName = (String) key;
			int style = SWT.NORMAL;
			RGBa foreground = null;
			RGBa background = null;
			String value = props.getProperty(displayName);
			String scopeSelector = displayName;
			int selectorIndex = value.indexOf(SELECTOR_DELIMITER);
			if (selectorIndex != -1)
			{
				scopeSelector = value.substring(selectorIndex + 1);
				if ("null".equals(scopeSelector)) //$NON-NLS-1$
				{
					scopeSelector = null;
				}
				value = value.substring(0, selectorIndex);
			}
			List<String> values = tokenize(value);
			// Handle empty fg with a bg color! If first token is just an empty value followed by a comma
			int num = 0;
			boolean skipFG = false;
			for (String token : values)
			{
				token = token.trim();
				if (token.length() == 0 && num == 0)
				{
					// empty fg!
					skipFG = true;
				}
				else if (token.length() > 0 && token.charAt(0) == '#')
				{
					// it's a color!
					if (foreground == null && !skipFG)
					{
						foreground = parseHexRGBa(token);
					}
					else
					{
						background = parseHexRGBa(token);
					}
				}
				else
				{
					if (token.equalsIgnoreCase(ITALIC))
					{
						style |= SWT.ITALIC;
					}
					else if (token.equalsIgnoreCase(UNDERLINE))
					{
						style |= TextAttribute.UNDERLINE;
					}
					else if (token.equalsIgnoreCase(BOLD))
					{
						style |= SWT.BOLD;
					}
				}
				num++;
			}
			DelayedTextAttribute attribute = new DelayedTextAttribute(foreground, background, style);
			coloringRules.add(new ThemeRule(displayName, scopeSelector == null ? null
					: new ScopeSelector(scopeSelector), attribute));
		}
	}

	private List<String> tokenize(String value)
	{
		List<String> tokens = new ArrayList<String>();
		if (!value.contains(DELIMETER))
		{
			tokens.add(value);
			return tokens;
		}
		return Arrays.asList(value.split(DELIMETER));
	}

	private RGB parseHexRGB(String hex)
	{
		return parseHexRGB(hex, false);
	}

	private RGB parseHexRGB(String hex, boolean alphaMergeWithBG)
	{
		RGBa a = parseHexRGBa(hex);
		RGB rgb = a.toRGB();
		if (alphaMergeWithBG)
		{
			// Handle RGBa values by mixing against BG, etc
			return alphaBlend(defaultBG, rgb, a.getAlpha());
		}
		return rgb;
	}

	private RGBa parseHexRGBa(String hex)
	{
		if (hex == null)
			return new RGBa(0, 0, 0);
		if (hex.length() != 7 && hex.length() != 9)
		{
			IdeLog.logError(ThemePlugin.getDefault(),
					MessageFormat.format("Received RGBa Hex value with invalid length: {0}", hex)); //$NON-NLS-1$
			if (defaultFG != null)
			{
				return new RGBa(defaultFG);
			}
			return new RGBa(0, 0, 0);
		}
		String s = hex.substring(1, 3);
		int r = Integer.parseInt(s, 16);
		s = hex.substring(3, 5);
		int g = Integer.parseInt(s, 16);
		s = hex.substring(5, 7);
		int b = Integer.parseInt(s, 16);
		if (hex.length() == 9)
		{
			s = hex.substring(7, 9);
			int a = Integer.parseInt(s, 16);
			return new RGBa(r, g, b, a);
		}
		return new RGBa(r, g, b);
	}

	public static RGB alphaBlend(RGB base, RGB top, int alpha)
	{
		int newRed = alphaBlend(base.red, top.red, alpha);
		int newGreen = alphaBlend(base.green, top.green, alpha);
		int newBlue = alphaBlend(base.blue, top.blue, alpha);
		return new RGB(newRed, newGreen, newBlue);
	}

	private static int alphaBlend(int base, int top, int alpha)
	{
		int oneMinusAlpha = 255 - alpha;
		int r = oneMinusAlpha * base + alpha * top + 128;
		return ((r + (r >> 8)) >> 8);
	}

	public TextAttribute getTextAttribute(String scope)
	{
		if (cache.containsKey(scope))
		{
			return cache.get(scope);
		}
		lastSelectorMatch = null;
		TextAttribute ta = toTextAttribute(getDelayedTextAttribute(scope), true);
		cache.put(scope, ta);
		return ta;
	}

	ThemeRule winningRule(String scope)
	{
		IScopeSelector match = findMatch(scope);
		if (match == null)
		{
			return null;
		}
		return getRuleForSelector(match);
	}

	private DelayedTextAttribute getDelayedTextAttribute(String scope)
	{
		IScopeSelector match = findMatch(scope);
		if (match != null)
		{
			// This is to avoid matching the same selector multiple times when recursing up the scope! Basically our
			// match may have been many steps up our scope, not at the end!
			if (lastSelectorMatch != null && lastSelectorMatch.equals(match))
			{
				// We just matched the same rule! We need to recurse from parent scope!
				return getParent(scope);
			}
			lastSelectorMatch = match;
			ThemeRule rule = getRuleForSelector(match);
			DelayedTextAttribute attr = rule.getTextAttribute();

			// if our coloring has no background, we should use parent's. If it has some opacity (alpha != 255), we
			// need to alpha blend
			if (attr.getBackground() == null || !attr.getBackground().isFullyOpaque())
			{
				// Need to merge bg color up the scope!
				DelayedTextAttribute parentAttr = getParent(scope);
				// Now do actual merge
				attr = merge(attr, parentAttr);
			}
			return attr;
		}

		// Some tokens are special. They have fallbacks even if not in the theme! Looks like bundles can contribute
		// them?
		if (new ScopeSelector("markup.changed").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(248, 205, 14), SWT.NORMAL);
		}
		if (new ScopeSelector("markup.deleted").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(255, 86, 77), SWT.NORMAL);
		}
		if (new ScopeSelector("markup.inserted").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(new RGBa(0, 0, 0), new RGBa(128, 250, 120), SWT.NORMAL);
		}
		if (new ScopeSelector("markup.underline").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(null, null, TextAttribute.UNDERLINE);
		}
		if (new ScopeSelector("markup.bold").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(null, null, SWT.BOLD);
		}
		if (new ScopeSelector("markup.italic").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(null, null, SWT.ITALIC);
		}
		if (new ScopeSelector("meta.diff.index").matches(scope) || new ScopeSelector("meta.diff.range").matches(scope) || new ScopeSelector("meta.separator.diff").matches(scope)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		{
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(65, 126, 218), SWT.ITALIC);
		}
		if (new ScopeSelector("meta.diff.header").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(103, 154, 233), SWT.NORMAL);
		}
		if (new ScopeSelector("meta.separator").matches(scope)) //$NON-NLS-1$
		{
			return new DelayedTextAttribute(new RGBa(255, 255, 255), new RGBa(52, 103, 209), SWT.NORMAL);
		}
		if (hasDarkBG())
		{
			if (new ScopeSelector("console.error").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(255, 0, 0), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.input").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(95, 175, 176), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.prompt").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(131, 132, 161), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.warning").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(255, 215, 0), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.debug").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(255, 236, 139), null, SWT.NORMAL);
			}
			if (new ScopeSelector("hyperlink").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(84, 143, 160), null, SWT.NORMAL);
			}
		}
		else
		{
			if (new ScopeSelector("console.error").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(255, 0, 0), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.input").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(63, 127, 95), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.prompt").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(42, 0, 255), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.warning").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(205, 102, 0), null, SWT.NORMAL);
			}
			if (new ScopeSelector("console.debug").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(93, 102, 102), null, SWT.NORMAL);
			}
			if (new ScopeSelector("hyperlink").matches(scope)) //$NON-NLS-1$
			{
				return new DelayedTextAttribute(new RGBa(13, 17, 113), null, SWT.NORMAL);
			}
		}
		return new DelayedTextAttribute(new RGBa(defaultFG));
	}

	public ThemeRule getRuleForSelector(IScopeSelector match)
	{
		// See APSTUD-2790. In Textmate the last matching rule wins, so to get that behavior we reverse the rule list
		// before matching.
		List<ThemeRule> reversed = new ArrayList<ThemeRule>(coloringRules);
		Collections.reverse(reversed);
		for (ThemeRule rule : reversed)
		{
			if (rule.isSeparator())
			{
				continue;
			}
			if (rule.getScopeSelector().equals(match))
			{
				return rule;
			}
		}
		return null;
	}

	protected DelayedTextAttribute getParent(String scope)
	{
		DelayedTextAttribute parentAttr = null;
		int index = scope.lastIndexOf(' ');
		if (index != -1)
		{
			String subType = scope.substring(0, index);
			parentAttr = getDelayedTextAttribute(subType);
		}
		if (parentAttr == null)
		{
			// If we never find a parent, use default bg
			parentAttr = new DelayedTextAttribute(new RGBa(defaultFG), new RGBa(defaultBG), 0);
		}
		return parentAttr;
	}

	private IScopeSelector findMatch(String scope)
	{
		Collection<IScopeSelector> selectors = new ArrayList<IScopeSelector>();
		for (ThemeRule rule : coloringRules)
		{
			if (rule.isSeparator())
			{
				continue;
			}
			selectors.add(rule.getScopeSelector());
		}
		return ScopeSelector.bestMatch(selectors, scope);
	}

	private DelayedTextAttribute merge(DelayedTextAttribute childAttr, DelayedTextAttribute parentAttr)
	{
		return new DelayedTextAttribute(merge(childAttr.getForeground(), parentAttr.getForeground(), defaultFG), merge(
				childAttr.getBackground(), parentAttr.getBackground(), defaultBG), childAttr.getStyle()
				| parentAttr.getStyle());
	}

	private RGBa merge(RGBa top, RGBa bottom, RGB defaultParent)
	{
		if (top == null && bottom == null)
		{
			return new RGBa(defaultParent);
		}
		if (top == null) // for some reason there is no top.
		{
			return bottom;
		}
		if (top.isFullyOpaque()) // top has no transparency, just return it
		{
			return top;
		}
		if (bottom == null) // there is no parent, merge onto default FG/BG for theme
		{
			return new RGBa(alphaBlend(defaultParent, top.toRGB(), top.getAlpha()));
		}
		return new RGBa(alphaBlend(bottom.toRGB(), top.toRGB(), top.getAlpha()));
	}

	private TextAttribute toTextAttribute(DelayedTextAttribute attr, boolean forceColor)
	{
		Color fg = null;
		if (attr.getForeground() != null || forceColor)
		{
			fg = colorManager.getColor(merge(attr.getForeground(), null, defaultFG).toRGB());
		}
		Color bg = null;
		if (attr.getBackground() != null || forceColor)
		{
			bg = colorManager.getColor(merge(attr.getBackground(), null, defaultBG).toRGB());
		}
		return new TextAttribute(fg, bg, attr.getStyle());
	}

	/**
	 * The background color to use for the editor and any themed views.
	 * 
	 * @return
	 */
	public RGB getBackground()
	{
		return defaultBG;
	}

	/**
	 * Return the RGBa values for the selection color bg.
	 * 
	 * @return
	 */
	public RGBa getSelection()
	{
		return selection;
	}

	/**
	 * The foreground color for editor text and any themed views.
	 * 
	 * @return
	 */
	public RGB getForeground()
	{
		return defaultFG;
	}

	/**
	 * Color to be used to highlight the current line in the editor.
	 * 
	 * @return
	 */
	public RGBa getLineHighlight()
	{
		return lineHighlight;
	}

	/**
	 * Color used for the caret/cursor in the text editor.
	 * 
	 * @return
	 */
	public RGB getCaret()
	{
		return caret;
	}

	/**
	 * Color that should be used to highlight character pairs.
	 * 
	 * @return
	 */
	public RGB getCharacterPairColor()
	{
		return alphaBlend(defaultBG, getCaret(), 128);
	}

	/**
	 * Color that should be used for occurrence indications (i.e. html/xml tag pairs). Same as
	 * {@link #getCharacterPairColor()} for now.
	 * 
	 * @return
	 */
	public RGB getOccurenceHighlightColor()
	{
		return getCharacterPairColor();
	}

	/**
	 * The unique name for this theme.
	 * 
	 * @return
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * The Map from scope selectors (as strings) to the FG/BG/Font styles (TextAttributes) that should be applied for
	 * them. Clients should never need to use this, this is meant for the preference page and testing!
	 * 
	 * @return
	 */
	public List<ThemeRule> getTokens()
	{
		return Collections.unmodifiableList(coloringRules);
	}

	/**
	 * A Java Properties file serialization of this theme.
	 * 
	 * @return
	 */
	public Properties toProps()
	{
		Properties props = new OrderedProperties();
		props.put(THEME_NAME_PROP_KEY, getName());
		props.put(SELECTION_PROP_KEY, toHex(getSelection()));
		props.put(LINE_HIGHLIGHT_PROP_KEY, toHex(getLineHighlight()));
		props.put(FOREGROUND_PROP_KEY, toHex(getForeground()));
		props.put(BACKGROUND_PROP_KEY, toHex(getBackground()));
		props.put(CARET_PROP_KEY, toHex(caret));
		for (ThemeRule rule : coloringRules)
		{
			if (rule == null)
			{
				continue;
			}
			StringBuilder value = new StringBuilder();
			DelayedTextAttribute attr = rule.getTextAttribute();
			RGBa color = attr.getForeground();
			if (color != null)
			{
				value.append(toHex(color)).append(DELIMETER);
			}
			color = attr.getBackground();
			if (color != null)
			{
				value.append(toHex(color)).append(DELIMETER);
			}
			int style = attr.getStyle();
			if ((style & SWT.ITALIC) != 0)
			{
				value.append(ITALIC).append(DELIMETER);
			}
			if ((style & TextAttribute.UNDERLINE) != 0)
			{
				value.append(UNDERLINE).append(DELIMETER);
			}
			if ((style & SWT.BOLD) != 0)
			{
				value.append(BOLD).append(DELIMETER);
			}
			if (value.length() > 0)
			{
				value.deleteCharAt(value.length() - 1);
			}
			// Append the scope selector
			value.append(SELECTOR_DELIMITER);
			value.append(rule.getScopeSelector().toString());
			props.put(rule.getName(), value.toString());
		}
		return props;
	}

	static String toHex(RGBa color)
	{
		String rgbString = toHex(color.toRGB());
		if (color.getAlpha() == 0)
		{
			return rgbString;
		}
		return rgbString + pad(Integer.toHexString(color.getAlpha()), 2, '0');
	}

	public static String toHex(RGB rgb)
	{
		return MessageFormat.format("#{0}{1}{2}", pad(Integer.toHexString(rgb.red), 2, '0'), pad(Integer //$NON-NLS-1$
				.toHexString(rgb.green), 2, '0'), pad(Integer.toHexString(rgb.blue), 2, '0'));
	}

	private static String pad(String string, int desiredLength, char padChar)
	{
		while (string.length() < desiredLength)
		{
			string = padChar + string;
		}
		return string;
	}

	protected void storeDefaults()
	{
		// Don't store builtin themes default copy in prefs!
		if (getThemeManager().isBuiltinTheme(getName()))
		{
			return;
		}
		// Only save to defaults if it has never been saved there. Basically take a snapshot of first version and
		// use that as the "default"
		IEclipsePreferences prefs = EclipseUtil.defaultScope().getNode(ThemePlugin.PLUGIN_ID);
		if (prefs == null)
		{
			return; // TODO Log something?
		}
		Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
		if (preferences == null)
		{
			return;
		}
		String value = preferences.get(getName(), null);
		if (value == null)
		{
			save(EclipseUtil.defaultScope());
		}
	}

	public void save()
	{
		save(EclipseUtil.instanceScope());
		if (getThemeManager().getCurrentTheme().equals(this))
		{
			getThemeManager().setCurrentTheme(this);
		}
	}

	protected IThemeManager getThemeManager()
	{
		return ThemePlugin.getDefault().getThemeManager();
	}

	private void save(IScopeContext scope)
	{
		ByteArrayOutputStream os = null;
		try
		{
			os = new ByteArrayOutputStream();
			toProps().store(os, null);
			IEclipsePreferences prefs = scope.getNode(ThemePlugin.PLUGIN_ID);
			Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
			preferences.putByteArray(getName(), os.toByteArray());
			prefs.flush();
		}
		catch (Exception e)
		{
			IdeLog.logError(ThemePlugin.getDefault(), e);
		}
		finally
		{
			if (os != null)
			{
				try
				{
					os.close();
				}
				catch (IOException e)
				{
					// ignore
				}
			}
		}
	}

	public void loadFromDefaults() throws InvalidPropertiesFormatException, UnsupportedEncodingException, IOException
	{
		Properties props = null;
		if (getThemeManager().isBuiltinTheme(getName()))
		{
			Theme builtin = ((ThemeManager) getThemeManager()).loadBuiltinTheme(getName());
			props = builtin.toProps();
		}
		else
		{
			IEclipsePreferences prefs = EclipseUtil.defaultScope().getNode(ThemePlugin.PLUGIN_ID);
			Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
			ByteArrayInputStream byteStream = null;
			try
			{
				byte[] array = preferences.getByteArray(getName(), null);
				if (array == null)
				{
					return;
				}
				props = new OrderedProperties();
				byteStream = new ByteArrayInputStream(array);
				props.load(byteStream);
			}
			catch (IllegalArgumentException iae)
			{
				// Fallback to load theme that was saved in prefs as XML string
				String xml = preferences.get(getName(), null);
				if (xml == null)
				{
					return;
				}
				ByteArrayInputStream xmlStream = null;
				try
				{
					xmlStream = new ByteArrayInputStream(xml.getBytes("UTF-8")); //$NON-NLS-1$
					props = new OrderedProperties();
					props.loadFromXML(xmlStream);
					save(EclipseUtil.defaultScope());
				}
				finally
				{
					if (xmlStream != null)
					{
						try
						{
							xmlStream.close();
						}
						catch (Exception e)
						{
							// ignore
						}
					}
				}
			}
			finally
			{
				if (byteStream != null)
				{
					try
					{
						byteStream.close();
					}
					catch (Exception e)
					{
						// ignore
					}
				}
			}
		}
		coloringRules.clear();
		wipeCache();
		parseProps(props);
		deleteCustomVersion();
	}

	/**
	 * Removes the saved instance version of theme.
	 */
	private void deleteCustomVersion()
	{
		delete(EclipseUtil.instanceScope());
	}

	private void deleteDefaultVersion()
	{
		delete(EclipseUtil.defaultScope());
	}

	private void delete(IScopeContext context)
	{
		try
		{
			IEclipsePreferences prefs = context.getNode(ThemePlugin.PLUGIN_ID);
			Preferences preferences = prefs.node(ThemeManager.THEMES_NODE);
			preferences.remove(getName());
			preferences.flush();
		}
		catch (BackingStoreException e)
		{
			IdeLog.logError(ThemePlugin.getDefault(), e);
		}
	}

	public void reorderRule(int startIndex, int endIndex)
	{
		if (endIndex > startIndex)
		{
			endIndex--;
		}
		ThemeRule selected = coloringRules.remove(startIndex);
		coloringRules.add(endIndex, selected);
		save();
	}

	public void addNewDefaultToken(int index, String newTokenName)
	{
		addNewRule(index, newTokenName, null, new DelayedTextAttribute(null));
	}

	public void addNewRule(int index, String ruleName, ScopeSelector selector, DelayedTextAttribute attr)
	{
		coloringRules.add(index, new ThemeRule(ruleName, selector, attr));
		wipeCache();
		save();
	}

	public void updateRule(int index, ThemeRule newRule)
	{
		coloringRules.remove(index);
		if (index >= coloringRules.size())
		{
			coloringRules.add(newRule);
		}
		else
		{
			coloringRules.add(index, newRule);
		}
		wipeCache();
		save();
	}

	public void updateCaret(RGB newColor)
	{
		if (newColor == null || (caret != null && caret.equals(newColor)))
		{
			return;
		}
		caret = newColor;
		save();
	}

	public void updateFG(RGB newColor)
	{
		if (newColor == null || (defaultFG != null && defaultFG.equals(newColor)))
		{
			return;
		}
		wipeCache();
		defaultFG = newColor;
		save();
	}

	public void updateBG(RGB newColor)
	{
		if (newColor == null || (defaultBG != null && defaultBG.equals(newColor)))
		{
			return;
		}
		wipeCache();
		defaultBG = newColor;
		save();
	}

	private void wipeCache()
	{
		cache.clear();
	}

	public void updateLineHighlight(RGB newColor)
	{
		if (newColor == null || (lineHighlight != null && lineHighlight.toRGB().equals(newColor)))
		{
			return;
		}
		lineHighlight = new RGBa(newColor);
		save();
	}

	public void updateSelection(RGB newColor)
	{
		if (newColor == null || (selection != null && selection.toRGB().equals(newColor)))
		{
			return;
		}
		selection = new RGBa(newColor);
		searchResultBG = null;
		save();
	}

	public Theme copy(String value)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			Properties props = toProps();
			props.setProperty(THEME_NAME_PROP_KEY, value);
			Theme newTheme = new Theme(colorManager, props);
			addTheme(newTheme);
			return newTheme;
		}
		catch (Exception e)
		{
			IdeLog.logError(ThemePlugin.getDefault(), e);
			return null;
		}
	}

	protected void addTheme(Theme newTheme)
	{
		getThemeManager().addTheme(newTheme);
	}

	public void delete()
	{
		removeTheme();
		deleteCustomVersion();
		deleteDefaultVersion();
	}

	protected void removeTheme()
	{
		getThemeManager().removeTheme(this);
	}

	/**
	 * Determines if the theme defines this exact token type (not checking parents by dropping periods).
	 * 
	 * @param scopeSelector
	 * @return
	 */
	public boolean hasEntry(String scopeSelector)
	{
		IScopeSelector selector = new ScopeSelector(scopeSelector);
		ThemeRule rule = getRuleForSelector(selector);
		return rule != null;
	}

	public Color getForeground(String scope)
	{
		TextAttribute attr = getTextAttribute(scope);
		if (attr == null)
		{
			return null;
		}
		return attr.getForeground();
	}

	/**
	 * Returns the RGB value for the foreground of a specific token.
	 * 
	 * @param string
	 * @return
	 */
	public RGB getForegroundAsRGB(String scope)
	{
		Color fg = getForeground(scope);
		if (fg == null)
		{
			return null;
		}
		return fg.getRGB();
	}

	public Color getBackground(String scope)
	{
		TextAttribute attr = getTextAttribute(scope);
		if (attr == null)
		{
			return null;
		}
		return attr.getBackground();
	}

	/**
	 * Returns the RGB value for the background of a specific token.
	 * 
	 * @param string
	 * @return
	 */
	public RGB getBackgroundAsRGB(String scope)
	{
		Color bg = getBackground(scope);
		if (bg == null)
		{
			return null;
		}
		return bg.getRGB();
	}

	/**
	 * Based on the selection color. If the selection color is found to be "dark", we lighten it some, otherwise we
	 * darken it some.
	 * 
	 * @return
	 */
	public RGB getSearchResultColor()
	{
		if (searchResultBG == null)
		{
			searchResultBG = isDark(getSelectionAgainstBG()) ? lighten(getSelectionAgainstBG())
					: darken(getSelectionAgainstBG());
		}
		return searchResultBG;
	}

	RGB lighten(RGB color)
	{
		float[] hsb = color.getHSB();
		return new RGB(hsb[0], hsb[1], Math.min(1, (float) (hsb[2] + 0.15)));
	}

	RGB darken(RGB color)
	{
		float[] hsb = color.getHSB();
		return new RGB(hsb[0], hsb[1], Math.max(0, (float) (hsb[2] - 0.15)));
	}

	public boolean hasDarkBG()
	{
		return isDark(getBackground());
	}

	public boolean hasLightFG()
	{
		return !isDark(getForeground());
	}

	private boolean isDark(RGB color)
	{
		// Convert to grayscale
		double grey = 0.3 * color.red + 0.59 * color.green + 0.11 * color.blue;
		return grey <= 128;
	}

	/**
	 * Returns the selection color alpha blended with the theme bg to give a good estimate of correct RGB value
	 * 
	 * @return
	 */
	public RGB getSelectionAgainstBG()
	{
		return alphaBlend(defaultBG, selection.toRGB(), selection.getAlpha());
	}

	/**
	 * Returns the line highlight color alpha blended with the theme bg to give a good estimate of correct RGB value
	 * 
	 * @return
	 */
	public RGB getLineHighlightAgainstBG()
	{
		return alphaBlend(defaultBG, lineHighlight.toRGB(), lineHighlight.getAlpha());
	}

	public void remove(ThemeRule entry)
	{
		coloringRules.remove(entry);
		wipeCache();
		save();
	}

	/**
	 * Does the user have invasive themes turned on?
	 * 
	 * @return
	 */
	public boolean isInvasive()
	{
		return ThemePlugin.invasiveThemesEnabled();
	}

	public Color getForegroundColor()
	{
		return getColorManager().getColor(getForeground());
	}

	public Color getBackgroundColor()
	{
		return getColorManager().getColor(getBackground());
	}

	protected ColorManager getColorManager()
	{
		return ThemePlugin.getDefault().getColorManager();
	}

}
