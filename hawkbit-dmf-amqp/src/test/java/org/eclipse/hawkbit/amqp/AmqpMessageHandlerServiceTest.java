/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.ActionStatus;
import org.eclipse.hawkbit.dmf.json.model.ActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DownloadResponse;
import org.eclipse.hawkbit.dmf.json.model.TenantSecruityToken;
import org.eclipse.hawkbit.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.cache.Cache;
import org.springframework.http.HttpStatus;

import com.google.common.eventbus.EventBus;

@RunWith(MockitoJUnitRunner.class)


public class AmqpMessageHandlerServiceTest {

    private static final String TENANT = "DEFAULT";

    private AmqpMessageHandlerService amqpMessageHandlerService;

    private MessageConverter messageConverter;

    @Mock
    private ControllerManagement controllerManagementMock;

    @Mock
    private ArtifactManagement artifactManagementMock;

    @Mock
    private AmqpControllerAuthentfication authenticationManagerMock;

    @Mock
    private ArtifactRepository artifactRepositoryMock;

    @Mock
    private Cache cacheMock;

    @Mock
    private HostnameResolver hostnameResolverMock;

    @Mock
    private EventBus eventBus;

    @Before
    public void before() throws Exception {
        amqpMessageHandlerService = new AmqpMessageHandlerService();
        amqpMessageHandlerService.setControllerManagement(controllerManagementMock);
        messageConverter = new Jackson2JsonMessageConverter();
        final RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setMessageConverter(messageConverter);
        amqpMessageHandlerService.setRabbitTemplate(rabbitTemplate);
        amqpMessageHandlerService.setAuthenticationManager(authenticationManagerMock);
        amqpMessageHandlerService.setArtifactManagement(artifactManagementMock);
        amqpMessageHandlerService.setCache(cacheMock);
        amqpMessageHandlerService.setHostnameResolver(hostnameResolverMock);
        amqpMessageHandlerService.setEventBus(eventBus);

    }

    @Test(expected = IllegalArgumentException.class)
    
    public void testWrongContentType() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("xml");
        final Message message = new Message(new byte[0], messageProperties);
        amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT);
        fail();
    }

    @Test
    
    public void testCreateThing() {
        final String knownThingId = "1";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        // mock
        final ArgumentCaptor<String> targetIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        when(controllerManagementMock.findOrRegisterTargetIfItDoesNotexist(targetIdCaptor.capture(),
                uriCaptor.capture())).thenReturn(null);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT);

        // verify
        assertThat(targetIdCaptor.getValue()).isEqualTo(knownThingId);
        assertThat(uriCaptor.getValue().toString()).isEqualTo("amqp://MyTest");

    }

    @Test
    
    public void testCreateThingWitoutReplyTo() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED, null);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "1");
        final Message message = messageConverter.toMessage("", messageProperties);

        try {
            amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT);
            fail("IllegalArgumentException was excepeted since no replyTo header was set");
        } catch (final IllegalArgumentException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    
    public void testCreateThingWithoutID() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);
        try {
            amqpMessageHandlerService.onMessage(message, MessageType.THING_CREATED.name(), TENANT);
            fail("IllegalArgumentException was excepeted since no thingID was set");
        } catch (final IllegalArgumentException exception) {
            // test ok - exception was excepted
        }
    }

    @Test
    
    public void testUnknownMessageType() {
        final String type = "bumlux";
        final MessageProperties messageProperties = createMessageProperties(MessageType.THING_CREATED);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, "");
        final Message message = messageConverter.toMessage(new byte[0], messageProperties);

        try {
            amqpMessageHandlerService.onMessage(message, type, TENANT);
            fail("IllegalArgumentException was excepeted due to unknown message type");
        } catch (final IllegalArgumentException exception) {
            // test ok - exception was excepted
        }
    }

    @Test
    
    public void testInvalidEventTopic() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        final Message message = new Message(new byte[0], messageProperties);
        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT);
            fail();
        } catch (final IllegalArgumentException e) {
        }

        try {
            messageProperties.setHeader(MessageHeaderKey.TOPIC, "wrongTopic");
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT);
            fail();
        } catch (final IllegalArgumentException e) {
        }

        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.CANCEL_DOWNLOAD.name());
        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT);
            fail("IllegalArgumentException was excepeted because there was no event topic");
        } catch (final IllegalArgumentException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    
    public void testUpdateActionStatusWithoutActionId() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
        actionUpdateStatus.setActionStatus(ActionStatus.DOWNLOAD);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT);
            fail("IllegalArgumentException was excepeted since no action id was set");
        } catch (final IllegalArgumentException exception) {
            // test ok - exception was excepted
        }
    }

    @Test
    
    public void testUpdateActionStatusWithoutExistActionId() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final ActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(ActionStatus.DOWNLOAD);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        try {
            amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT);
            fail("IllegalArgumentException was excepeted since no action id was set");
        } catch (final IllegalArgumentException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    
    public void authenticationRequestDeniedForArtifactWhichDoesNotExists() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.AUTHENTIFICATION);
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, "123", "12345");
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onMessage(message, MessageType.AUTHENTIFICATION.name(),
                TENANT);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    
    public void authenticationRequestDeniedForArtifactWhichIsNotAssignedToTarget() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.AUTHENTIFICATION);
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, "123", "12345");
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        final LocalArtifact localArtifactMock = mock(LocalArtifact.class);
        when(artifactManagementMock.findFirstLocalArtifactsBySHA1(anyString())).thenReturn(localArtifactMock);
        when(controllerManagementMock.getActionForDownloadByTargetAndSoftwareModule(anyObject(), anyObject()))
                .thenThrow(EntityNotFoundException.class);

        // test
        final Message onMessage = amqpMessageHandlerService.onMessage(message, MessageType.AUTHENTIFICATION.name(),
                TENANT);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    
    public void authenticationRequestAllowedForArtifactWhichExistsAndAssignedToTarget() throws MalformedURLException {
        final MessageProperties messageProperties = createMessageProperties(MessageType.AUTHENTIFICATION);
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, "123", "12345");
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // mock
        final LocalArtifact localArtifactMock = mock(LocalArtifact.class);
        final Action actionMock = mock(Action.class);
        final DbArtifact dbArtifactMock = mock(DbArtifact.class);
        when(artifactManagementMock.findFirstLocalArtifactsBySHA1(anyString())).thenReturn(localArtifactMock);
        when(controllerManagementMock.getActionForDownloadByTargetAndSoftwareModule(anyObject(), anyObject()))
                .thenReturn(actionMock);
        when(artifactManagementMock.loadLocalArtifactBinary(localArtifactMock)).thenReturn(dbArtifactMock);
        when(dbArtifactMock.getArtifactId()).thenReturn("artifactId");
        when(dbArtifactMock.getSize()).thenReturn(1L);
        when(dbArtifactMock.getHashes()).thenReturn(new DbArtifactHash("sha1", "md5"));
        when(hostnameResolverMock.resolveHostname()).thenReturn(new URL("http://localhost"));

        // test
        final Message onMessage = amqpMessageHandlerService.onMessage(message, MessageType.AUTHENTIFICATION.name(),
                TENANT);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(downloadResponse.getArtifact().getSize()).isEqualTo(1L);
        assertThat(downloadResponse.getDownloadUrl()).startsWith("http://localhost/api/v1/downloadserver/downloadId/");
    }

    @Test
    
    public void lookupNextUpdateActionAfterFinished() throws IllegalArgumentException, IllegalAccessException {

        // Mock
        final Action action = createActionWithTarget(22L, Status.FINISHED);
        when(controllerManagementMock.findActionWithDetails(Matchers.any())).thenReturn(action);
        when(controllerManagementMock.addUpdateActionStatus(Matchers.any(), Matchers.any())).thenReturn(action);
        // for the test the same action can be used
        final List<Action> actionList = new ArrayList<Action>();
        actionList.add(action);
        when(controllerManagementMock.findActionByTargetAndActive(Matchers.any())).thenReturn(actionList);

        final List<SoftwareModule> softwareModuleList = createSoftwareModuleList();
        when(controllerManagementMock.findSoftwareModulesByDistributionSet(Matchers.any()))
                .thenReturn(softwareModuleList);

        final MessageProperties messageProperties = createMessageProperties(MessageType.EVENT);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        final ActionUpdateStatus actionUpdateStatus = createActionUpdateStatus(ActionStatus.FINISHED, 23L);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(actionUpdateStatus,
                messageProperties);

        // test
        amqpMessageHandlerService.onMessage(message, MessageType.EVENT.name(), TENANT);

        // verify
        final ArgumentCaptor<TargetAssignDistributionSetEvent> captorTargetAssignDistributionSetEvent = ArgumentCaptor
                .forClass(TargetAssignDistributionSetEvent.class);
        verify(eventBus, times(1)).post(captorTargetAssignDistributionSetEvent.capture());
        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = captorTargetAssignDistributionSetEvent
                .getValue();

        assertThat(targetAssignDistributionSetEvent.getControllerId()).isEqualTo("target1");
        assertThat(targetAssignDistributionSetEvent.getActionId()).isEqualTo(22L);
        assertThat(targetAssignDistributionSetEvent.getSoftwareModules()).isEqualTo(softwareModuleList);

    }

    private ActionUpdateStatus createActionUpdateStatus(final ActionStatus status) {
        return createActionUpdateStatus(status, 2l);
    }

    private ActionUpdateStatus createActionUpdateStatus(final ActionStatus status, final Long id) {
        final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
        actionUpdateStatus.setActionId(id);
        actionUpdateStatus.setSoftwareModuleId(Long.valueOf(2));
        actionUpdateStatus.setActionStatus(status);
        return actionUpdateStatus;
    }

    private MessageProperties createMessageProperties(final MessageType type) {
        return createMessageProperties(type, "MyTest");
    }

    private MessageProperties createMessageProperties(final MessageType type, final String replyTo) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader(MessageHeaderKey.TYPE, type.name());
        messageProperties.setHeader(MessageHeaderKey.TENANT, TENANT);
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setReplyTo(replyTo);
        return messageProperties;
    }

    private List<SoftwareModule> createSoftwareModuleList() {
        final List<SoftwareModule> softwareModuleList = new ArrayList<SoftwareModule>();
        final SoftwareModule softwareModule = new SoftwareModule();
        softwareModule.setId(777L);
        softwareModuleList.add(softwareModule);
        return softwareModuleList;
    }

    private Action createActionWithTarget(final Long targetId, final Status status)
            throws IllegalArgumentException, IllegalAccessException {
        // is needed for the creation of targets
        initalizeSecurityTokenGenerator();

        // Mock
        final Action action = new Action();
        action.setId(targetId);
        action.setStatus(status);
        action.setTenant("DEFAULT");
        final Target target = new Target("target1");
        action.setTarget(target);

        return action;
    }

    private void initalizeSecurityTokenGenerator() throws IllegalArgumentException, IllegalAccessException {
        final SecurityTokenGeneratorHolder instance = SecurityTokenGeneratorHolder.getInstance();
        final Field[] fields = instance.getClass().getDeclaredFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(SecurityTokenGenerator.class)) {
                field.setAccessible(true);
                field.set(instance, new SecurityTokenGenerator());
            }
        }
    }
}
