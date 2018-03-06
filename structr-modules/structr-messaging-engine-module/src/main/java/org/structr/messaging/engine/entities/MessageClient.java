/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.messaging.engine.entities;

import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MessageClient extends NodeInterface {

    class Impl {
        static {

			final JsonSchema schema     = SchemaService.getDynamicSchema();
			final JsonObjectType type   = schema.addType("MessageClient");
			final JsonObjectType sub 	= schema.addType("MessageSubscriber");

			type.setImplements(URI.create("https://structr.org/v1.1/definitions/MessageClient"));

			type.addMethod("sendMessage")
				.setReturnType(RestMethodResult.class.getName())
				.addParameter("topic", String.class.getName())
				.addParameter("message", String.class.getName())
				.setSource("return " + MessageClient.class.getName() + ".sendMessage(this, topic, message);")
				.addException(FrameworkException.class.getName());

			type.addMethod("subscribeTopic")
					.setReturnType(RestMethodResult.class.getName())
					.addParameter("topic", String.class.getName())
					.setSource("return " + MessageClient.class.getName() + ".subscribeTopic(this, topic);")
					.addException(FrameworkException.class.getName());

			type.addMethod("unsubscribeTopic")
					.setReturnType(RestMethodResult.class.getName())
					.addParameter("topic", String.class.getName())
					.setSource("return " + MessageClient.class.getName() + ".unsubscribeTopic(this, topic);")
					.addException(FrameworkException.class.getName());

			type.relate(sub, "HAS_SUBSCRIBER", Relation.Cardinality.ManyToMany,"subscribers","clients");

			type.addViewProperty(PropertyView.Public, "subscribers");
			type.addViewProperty(PropertyView.Ui,     "subscribers");

        }
    }

    static RestMethodResult sendMessage(MessageClient thisClient, final String topic, final String message) throws FrameworkException {

        final App app = StructrApp.getInstance();
        try (final Tx tx = app.tx()) {

            List<MessageSubscriber> subscribers = thisClient.getProperty(StructrApp.key(MessageSubscriber.class,"subscribers") );
            if (subscribers != null) {
                subscribers.forEach(sub -> {
					String subTopic = sub.getProperty(StructrApp.key(MessageSubscriber.class,"topic"));
                    if ( subTopic != null && (subTopic.equals(topic) || subTopic.equals("*"))) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("topic", topic);
                        params.put("message", message);
                        try {
                            sub.invokeMethod("onMessage", params, false);
                        } catch (FrameworkException e) {
                            logger.warn("Could not invoke 'onMessage' method on MessageSubscriber: " + e.getMessage());
                        }
                    }
                });
            }

            tx.success();
        }

        return new RestMethodResult(200);
    }

    static RestMethodResult subscribeTopic(final String topic) throws FrameworkException {

        return new RestMethodResult(200);
    }

    static RestMethodResult unsubscribeTopic(final String topic) throws FrameworkException {

        return new RestMethodResult(200);
    }

}
