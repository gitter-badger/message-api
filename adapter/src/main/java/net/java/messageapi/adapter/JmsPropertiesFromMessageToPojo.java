package net.java.messageapi.adapter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;

import javax.jms.*;

class JmsPropertiesFromMessageToPojo implements JmsPropertyScanner.Visitor {
    public static void scan(Message message, Object pojo) {
        new JmsPropertyScanner(new JmsPropertiesFromMessageToPojo(message)).scan(pojo);
    }

    private final Message message;

    public JmsPropertiesFromMessageToPojo(Message message) {
        this.message = message;
    }

    @Override
    public void visit(String propertyName, Object container, Field field) throws JMSException, IllegalAccessException {
        Class<?> type = field.getType();
        if (String.class.equals(type)) {
            String value = message.getStringProperty(propertyName);
            field.set(container, value);
        } else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            boolean value = message.getBooleanProperty(propertyName);
            field.set(container, value);
        } else if (Byte.class.equals(type) || byte.class.equals(type)) {
            byte value = message.getByteProperty(propertyName);
            field.set(container, value);
            // there is no getCharacterProperty
        } else if (Short.class.equals(type) || short.class.equals(type)) {
            short value = message.getShortProperty(propertyName);
            field.set(container, value);
        } else if (Integer.class.equals(type) || int.class.equals(type)) {
            int value = message.getIntProperty(propertyName);
            field.set(container, value);
        } else if (Long.class.equals(type) || long.class.equals(type)) {
            long value = message.getLongProperty(propertyName);
            field.set(container, value);
        } else if (Float.class.equals(type) || float.class.equals(type)) {
            float value = message.getFloatProperty(propertyName);
            field.set(container, value);
        } else if (Double.class.equals(type) || double.class.equals(type)) {
            double value = message.getDoubleProperty(propertyName);
            field.set(container, value);
        } else if (List.class.equals(type)) {
            List<String> list = getListProperty(propertyName);
            field.set(container, list);
        } else if (type.isArray()) {
            List<String> list = getListProperty(propertyName);
            field.set(container, list.toArray(new String[0]));
        } else if (Set.class.equals(type)) {
            Set<String> set = getSetProperty(propertyName);
            field.set(container, set);
        } else if (Map.class.equals(type)) {
            Map<String, String> map = getMapProperty(propertyName);
            field.set(container, map);
        } else {
            throw new RuntimeException("don't know how to set " + field + " to the header-only jms propery "
                    + propertyName);
        }
    }

    private List<String> getListProperty(String propertyName) throws JMSException {
        List<String> list = new ArrayList<String>();
        Pattern pattern = Pattern.compile("^" + propertyName + "\\[([0-9]+)\\]$");
        @SuppressWarnings("unchecked")
        Enumeration<String> propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = propertyNames.nextElement();
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1));
                String value = message.getStringProperty(name);
                while (list.size() <= index)
                    list.add(null);
                list.set(index, value);
            }
        }
        return list;
    }

    private Set<String> getSetProperty(String propertyName) throws JMSException {
        Set<String> set = new HashSet<String>();
        Pattern pattern = Pattern.compile("^" + propertyName + "\\[([0-9]+)\\]$");
        @SuppressWarnings("unchecked")
        Enumeration<String> propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = propertyNames.nextElement();
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                String value = message.getStringProperty(name);
                set.add(value);
            }
        }
        return set;
    }

    private Map<String, String> getMapProperty(String propertyName) throws JMSException {
        Map<String, String> map = new HashMap<String, String>();
        Pattern pattern = Pattern.compile("^" + propertyName + "\\[([^\\]]+)\\]$");
        @SuppressWarnings("unchecked")
        Enumeration<String> propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = propertyNames.nextElement();
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                String key = matcher.group(1);
                String value = message.getStringProperty(name);
                map.put(key, value);
            }
        }
        return map;
    }
}