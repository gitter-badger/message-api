package com.oneandone.consumer.messageapi.xstream;

import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * This creates a very rudimentary JAXBContext for XStream: Only marshalling and only a very limited
 * set of JAXB annotations is supported.
 */
public class XStreamJaxbContextFactory {

    public static JAXBContext createContext(String contextPath, ClassLoader classLoader,
            Map<String, Object> properties) throws JAXBException {
        return new XStreamJaxbContext(contextPath, classLoader, properties);
    }

    // TODO add: createContext
    // public static JAXBContext createContext(Class<?>[] classes, Map<String, Object> properties)
    // throws JAXBException {
    // return new XStreamJaxbContext(contextPath, classLoader, properties);
    // }
}