/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.extension.error;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.RouteDefinition;
import org.assertj.core.api.Condition;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ErrorHandlingActionTest {

    @Test
    public void shouldResetErrorHandeledFlagIfNoStatusCodeIsPresent() {
        final Exchange exchange = mock(Exchange.class);
        final Message message = mock(Message.class);
        when(exchange.getIn()).thenReturn(message);

        ErrorHandlingAction.handleErrors(exchange);

        verify(exchange).getIn();
        verify(message).getHeader(ErrorHandlingAction.STATUS_CODE, Integer.class);
        verify(exchange).setProperty(Exchange.ERRORHANDLER_HANDLED, Boolean.FALSE);

        verifyNoMoreInteractions(exchange, message);
    }

    @Test
    public void shouldSetupErrorHandlersOnRouteDefinitions() throws Exception {
        final CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:a").log("a");

                from("direct:b").log("b");
            }
        });

        new ErrorHandlingAction(context);

        assertThat(context.getRouteDefinitions()).have(new Condition<RouteDefinition>() {
            @Override
            public boolean matches(final RouteDefinition routeDefinition) {
                final DefaultErrorHandlerBuilder errorHandlerBuilder = (DefaultErrorHandlerBuilder) routeDefinition.getErrorHandlerBuilder();

                final boolean itLogsHandeled = errorHandlerBuilder.getRedeliveryPolicy().isLogHandled();

                final boolean hasOnExceptionOccurredProcessor = errorHandlerBuilder.getOnExceptionOccurred() != null;

                final OnExceptionDefinition exceptionDefinition = errorHandlerBuilder.getExceptionPolicyStrategy().getExceptionPolicy(null, null, null);
                final Predicate handledPolicy = exceptionDefinition.getHandledPolicy();

                final boolean handeledPolicyIsTrue = handledPolicy.matches(null);

                return itLogsHandeled && hasOnExceptionOccurredProcessor && handeledPolicyIsTrue;
            }
        });
    }

    @Test
    public void shouldSetupErrorHandling() {
        final ErrorHandlingAction errorHandlingAction = new ErrorHandlingAction(mock(CamelContext.class));
        errorHandlingAction.setStatusCode(123);

        final Exchange exchange = mock(Exchange.class);
        final Message message = mock(Message.class);
        when(exchange.getIn()).thenReturn(message);
        errorHandlingAction.setupErrorHandling(exchange);

        verify(message).setHeader(ErrorHandlingAction.STATUS_CODE, Integer.valueOf(123));
    }

    @Test
    public void shouldSetupHttpResponse() {
        final Exchange exchange = mock(Exchange.class);
        final Message message = mock(Message.class);
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(ErrorHandlingAction.STATUS_CODE, Integer.class)).thenReturn(401);

        ErrorHandlingAction.handleErrors(exchange);

        verify(message).setHeader(Exchange.HTTP_RESPONSE_CODE, Integer.valueOf(401));
        verify(message).setBody(null);
    }
}
