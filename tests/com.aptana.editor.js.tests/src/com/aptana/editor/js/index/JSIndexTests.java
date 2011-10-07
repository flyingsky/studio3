/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.index;

import java.net.URI;
import java.util.List;

import junit.framework.TestCase;

import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.index.IJSIndexConstants;
import com.aptana.editor.js.contentassist.index.JSIndexReader;
import com.aptana.editor.js.contentassist.index.JSIndexWriter;
import com.aptana.editor.js.contentassist.model.FunctionElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;

public class JSIndexTests extends TestCase
{
	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		IndexManager.getInstance().removeIndex(URI.create(IJSIndexConstants.METADATA_INDEX_LOCATION));

		super.tearDown();
	}

	/**
	 * getIndex
	 * 
	 * @return
	 */
	private Index getIndex()
	{
		return JSIndexQueryHelper.getIndex();
	}

	/**
	 * getType
	 * 
	 * @param typeName
	 * @return
	 */
	private List<TypeElement> getType(String typeName)
	{
		JSIndexReader reader = new JSIndexReader();

		return reader.getType(this.getIndex(), typeName, true);
	}

	/**
	 * writeType
	 * 
	 * @param type
	 */
	private void writeType(TypeElement type)
	{
		JSIndexWriter writer = new JSIndexWriter();

		writer.writeType(this.getIndex(), type);
	}

	/**
	 * testType
	 */
	public void testType()
	{
		String typeName = "MyClass";

		TypeElement type = new TypeElement();
		type.setName(typeName);
		this.writeType(type);

		List<TypeElement> retrievedTypes = this.getType(typeName);
		TypeElement retrievedType = retrievedTypes.get(0);

		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());
	}

	/**
	 * testMethod
	 */
	public void testMethod()
	{
		String typeName = "MyClass";
		String methodName = "myMethod";

		// create type
		TypeElement type = new TypeElement();
		type.setName(typeName);

		// create method within type
		FunctionElement method = new FunctionElement();
		method.setName(methodName);
		type.addProperty(method);

		// write type to index
		this.writeType(type);

		// then retrieve it
		List<TypeElement> retrievedTypes = this.getType(typeName);
		TypeElement retrievedType = retrievedTypes.get(0);

		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());

		// make sure we have one property
		List<PropertyElement> properties = retrievedType.getProperties();
		assertNotNull(properties);
		assertTrue(properties.size() == 1);

		// make sure it is a function
		PropertyElement property = properties.get(0);
		assertTrue(property instanceof FunctionElement);

		// make sure it is the function we added earlier
		FunctionElement retrievedMethod = (FunctionElement) property;
		assertEquals(methodName, retrievedMethod.getName());
	}

	/**
	 * testProperty
	 */
	public void testProperty()
	{
		String typeName = "MyClass";
		String propertyName = "myProperty";

		// create type
		TypeElement type = new TypeElement();
		type.setName(typeName);

		// create property within type
		PropertyElement property = new PropertyElement();
		property.setName(propertyName);
		type.addProperty(property);

		// write type to index
		this.writeType(type);

		// then retrieve it
		List<TypeElement> retrievedTypes = this.getType(typeName);
		TypeElement retrievedType = retrievedTypes.get(0);

		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());

		// make sure we have one property
		List<PropertyElement> properties = retrievedType.getProperties();
		assertNotNull(properties);
		assertTrue(properties.size() == 1);

		// make sure the name is correct
		PropertyElement retrievedProperty = properties.get(0);
		assertEquals(propertyName, retrievedProperty.getName());
	}
}
