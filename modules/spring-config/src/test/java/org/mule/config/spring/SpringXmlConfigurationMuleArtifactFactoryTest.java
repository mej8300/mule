/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.config.XmlConfigurationCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DOMWriter;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;

public class SpringXmlConfigurationMuleArtifactFactoryTest
{

    protected static Element createPropertyPlaceholder(String location, String ignoreUnresolvable) throws DocumentException
    {
        Element propertyPlaceholder = createElement("property-placeholder", "http://www.springframework.org/schema/context", "context");
        if (location != null)
        {
            propertyPlaceholder.setAttribute("location", location);
        }
        if (ignoreUnresolvable != null)
        {
            propertyPlaceholder.setAttribute("ignore-unresolvable", ignoreUnresolvable);
        }
        return propertyPlaceholder;
    }

    public static Element createElement(String name, String namespace) throws DocumentException
    {
        return createElement(name, namespace, null);
    }

    public static Element createElement(String name, String namespace, String prefix) throws DocumentException
    {
        org.dom4j.Element dom4jElement = DocumentHelper.createElement(new QName(name, new Namespace(prefix, namespace)));
        Document document = dom4jElement.getDocument();
        if (document == null)
        {
            document = DocumentHelper.createDocument();
            document.setRootElement(dom4jElement);
        }

        final DOMWriter writer = new DOMWriter();
        org.w3c.dom.Document w3cDocument = writer.write(document);
        Element w3cElement = w3cDocument.getDocumentElement();


        return w3cElement;
    }

    @Test
    public void whenCallingGetArtifactForMessageProcessorSystemPropertiesShouldRemain() throws MuleArtifactFactoryException, DocumentException
    {
        Properties properties = System.getProperties();

        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("test", "test1");
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(new Element[] {});
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        factoryTest.getArtifactForMessageProcessor(element, callback);

        Assert.assertThat("System properties where modified", properties, CoreMatchers.is(System.getProperties()));

    }

    @Test
    public void whenMultiplePropertyPlaceholdersWithIgnoreUnresolvableInAllExceptOneDoNotFail() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Element[] propertyPlaceholders = new Element[3];
        propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", null);
        propertyPlaceholders[1] = createPropertyPlaceholder("test2.properties", "true");
        propertyPlaceholders[2] = createPropertyPlaceholder("test3.properties", "true");

        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
        Document muleConfig = DocumentHelper.parseText(muleConfigTxt);

        List result = DocumentHelper.selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and not(@ignore-unresolvable)]", muleConfig);
        Assert.assertThat("Property placeholder for test1.properties is present", result.size(), CoreMatchers.is(1));

        result = DocumentHelper.selectNodes("/mule/*[local-name()='property-placeholder' and @location='test2.properties' and @ignore-unresolvable='true']", muleConfig);
        Assert.assertThat("Property placeholder for test2.properties is present", result.size(), CoreMatchers.is(1));

        result = DocumentHelper.selectNodes("/mule/*[local-name()='property-placeholder' and @location='test3.properties' and @ignore-unresolvable='true']", muleConfig);
        Assert.assertThat("Property placeholder for test3.properties is present", result.size(), CoreMatchers.is(1));
    }

    @Test
    public void whenOnePropertyPlaceholdersWithoutIgnoreUnresolvableDoNotFail() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Element[] propertyPlaceholders = new Element[1];
        propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", null);

        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
        Document muleConfig = DocumentHelper.parseText(muleConfigTxt);

        List result = DocumentHelper.selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and not(@ignore-unresolvable)]", muleConfig);
        Assert.assertThat("Property placeholder for test1.properties is present", result.size(), CoreMatchers.is(1));
    }

    @Test
    public void whenOnePropertyPlaceholdersWithIgnoreUnresolvableTrueDoNotFail() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Element[] propertyPlaceholders = new Element[1];
        propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", "true");

        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
        Document muleConfig = DocumentHelper.parseText(muleConfigTxt);

        List result = DocumentHelper.selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and @ignore-unresolvable='true']", muleConfig);
        Assert.assertThat("Property placeholder for test1.properties is present", result.size(), CoreMatchers.is(1));
    }

    @Test
    public void whenOnePropertyPlaceholdersWithIgnoreUnresolvableFalseDoNotFail() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Element[] propertyPlaceholders = new Element[1];
        propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", "false");

        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
        Document muleConfig = DocumentHelper.parseText(muleConfigTxt);

        List result = DocumentHelper.selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and @ignore-unresolvable='false']", muleConfig);
        Assert.assertThat("Property placeholder for test1.properties is present", result.size(), CoreMatchers.is(1));
    }

    @Test(expected = MuleArtifactFactoryException.class)
    public void whenMultiplePropertyPlaceholdersWithMultipleIgnoreUnresolvablMissingAlt1ShouldFail() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Element[] propertyPlaceholders = new Element[3];
        propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", null);
        propertyPlaceholders[1] = createPropertyPlaceholder("test2.properties", "false");
        propertyPlaceholders[2] = createPropertyPlaceholder("test3.properties", "true");

        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    }

    @Test(expected = MuleArtifactFactoryException.class)
    public void whenMultiplePropertyPlaceholdersWithMultipleIgnoreUnresolvablMissingAlt2ShouldFail() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Element[] propertyPlaceholders = new Element[3];
        propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", null);
        propertyPlaceholders[1] = createPropertyPlaceholder("test2.properties", "false");
        propertyPlaceholders[2] = createPropertyPlaceholder("test3.properties", null);

        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    }

    @Test(expected = MuleArtifactFactoryException.class)
    public void whenMultiplePropertyPlaceholdersWithMultipleIgnoreUnresolvablMissingAlt3ShouldFail() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Element[] propertyPlaceholders = new Element[3];
        propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", "true");
        propertyPlaceholders[1] = createPropertyPlaceholder("test2.properties", null);
        propertyPlaceholders[2] = createPropertyPlaceholder("test3.properties", null);

        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

        factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    }
}
