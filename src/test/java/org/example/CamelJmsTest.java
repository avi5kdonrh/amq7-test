package org.example;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class CamelJmsTest extends CamelTestSupport {
    private ActiveMQServer server;
    private ConnectionFactory connectionFactory;


    public void startServer() throws Exception{
        Configuration config = new ConfigurationImpl();
        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(false);
        config.addAcceptorConfiguration("in-vm", "vm://0");
        server = ActiveMQServers.newActiveMQServer(config);
        server.start();
        Properties properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, ActiveMQInitialContextFactory.class.getName());
        properties.setProperty("connectionFactory.ConnectionFactory","vm://0");
        connectionFactory = (ConnectionFactory)new InitialContext(properties).lookup("ConnectionFactory");
    }


    @Test
    public void camelJmsTest() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(2);
        template.sendBody("jms:queue:test","Hello");
        template.sendBody("jms:queue:test","Bye");
        assertMockEndpointsSatisfied(10, TimeUnit.SECONDS);
    }
    @Override
    protected CamelContext createCamelContext() throws Exception {
        startServer();
        CamelContext context = super.createCamelContext();
        context.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
        return context;
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:test")
                        .log("Testing consumer with this message: ${body}")
                        .to("mock:result");
            }
        };
    }
}
