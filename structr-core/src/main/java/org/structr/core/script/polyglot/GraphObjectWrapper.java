/*
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.script.polyglot;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.schema.action.ActionContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class GraphObjectWrapper<T extends GraphObject> implements ProxyObject {
	private static final Logger logger = LoggerFactory.getLogger(GraphObjectWrapper.class);
	private final T node;
	private ActionContext actionContext;

	public GraphObjectWrapper(ActionContext actionContext, final T node) {
		this.node = node;
		this.actionContext = actionContext;
	}

	public T getOriginalObject() {

		return node;
	}

	@Override
	public Object getMember(String key) {

		if (getOriginalObject() instanceof GraphObjectMap) {

			return PolyglotWrapper.wrap(actionContext, ((GraphObjectMap) getOriginalObject()).get(new GenericProperty<>(key)));
		} else {

			Map<String, Method> methods = StructrApp.getConfiguration().getAnnotatedMethods(node.getClass(), Export.class);
			if (methods.containsKey(key)) {
				Method method = methods.get(key);

				return (ProxyExecutable) arguments -> {

					Map<String, Object> params = null;
					if (arguments.length >= 1) {
						Object arg0 = PolyglotWrapper.unwrap(actionContext, arguments[0]);
						if (arg0 instanceof Map) {
							params = (Map<String, Object>) arg0;
						}
					} else {

						params = new HashMap<>();
					}
					try {

						return PolyglotWrapper.wrap(actionContext, method.invoke(node, actionContext.getSecurityContext(), params));
					} catch (IllegalAccessException | InvocationTargetException ex) {

						logger.error("Could not invoke method on graph object.", ex);
					}
					return null;
				};
			}

			PropertyKey propKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);
			if (propKey instanceof RelationProperty || propKey instanceof ArrayProperty) {
				// RelationshipProperty needs special binding
				// ArrayProperty values need synchronized ProxyArrays as well
				return new PolyglotProxyArray(actionContext, node, propKey);
			}

			return PolyglotWrapper.wrap(actionContext, node.getProperty(key));
		}
	}

	@Override
	public Object getMemberKeys() {
		if (getOriginalObject() instanceof GraphObjectMap) {

			return ((GraphObjectMap) getOriginalObject()).toMap().keySet().toArray();
		}
		return null;
	}

	@Override
	public boolean hasMember(String key) {
		if (getOriginalObject() instanceof GraphObjectMap) {

			return ((GraphObjectMap) getOriginalObject()).containsKey(new GenericProperty<>(key));
		} else {

			return StructrApp.getConfiguration().getAnnotatedMethods(node.getClass(), Export.class).containsKey(key) || StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key) != null;
		}
	}

	@Override
	public void putMember(String key, Value value) {
		Object unwrappedValue = PolyglotWrapper.unwrap(actionContext, value);

		if (getOriginalObject() instanceof GraphObjectMap) {

			((GraphObjectMap) getOriginalObject()).put(new GenericProperty<>(key), unwrappedValue);
		} else {

			try {

				final PropertyKey propKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);
				Object convertedValue = propKey.inputConverter(actionContext.getSecurityContext()).convert(unwrappedValue);
				node.setProperty(propKey, convertedValue);
			} catch (FrameworkException ex) {
				logger.error("Could not set property on graph object within scripting context.", ex);
			}
		}
	}
}
