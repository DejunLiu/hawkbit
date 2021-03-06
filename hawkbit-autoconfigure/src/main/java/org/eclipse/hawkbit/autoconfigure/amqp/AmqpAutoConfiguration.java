/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.amqp;

import org.eclipse.hawkbit.amqp.AmqpConfiguration;
import org.eclipse.hawkbit.amqp.annotation.EnableAmqp;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;

/**
 * The amqp autoconfiguration.
 *
 *
 *
 */
@Configuration
@ConditionalOnClass(value = AmqpConfiguration.class)
@EnableAmqp
public class AmqpAutoConfiguration {

    /**
     * Create default error handler bean.
     * 
     * @return the default error handler bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorHandler errorHandler() {
        return new ConditionalRejectingErrorHandler();
    }

}
