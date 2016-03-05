/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.dmf.json.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 *
 */


public class PropertyBasedArtifactUrlHandlerTest extends AbstractIntegrationTestWithMongoDB {

    @Autowired
    private ArtifactUrlHandler urlHandlerProperties;
    @Autowired
    private TenantAware tenantAware;
    private LocalArtifact localArtifact;
    private final String controllerId = "Test";

    @Before
    public void setup() {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final SoftwareModule module = dsA.getModules().iterator().next();
        localArtifact = (LocalArtifact) TestDataUtil.generateArtifacts(artifactManagement, module.getId()).stream()
                .findAny().get();
    }

    @Test
    
    public void testHttpUrl() {
        final String url = urlHandlerProperties.getUrl(controllerId, localArtifact, Artifact.UrlProtocol.HTTP);
        assertEquals("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                + "/softwaremodules/" + localArtifact.getSoftwareModule().getId() + "/artifacts/"
                + localArtifact.getFilename(), url);
    }

    @Test
    
    public void testHttpsUrl() {
        final String url = urlHandlerProperties.getUrl(controllerId, localArtifact, Artifact.UrlProtocol.HTTPS);
        assertEquals("https://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                + "/softwaremodules/" + localArtifact.getSoftwareModule().getId() + "/artifacts/"
                + localArtifact.getFilename(), url);
    }

    @Test
    
    public void testCoapUrl() {
        final String url = urlHandlerProperties.getUrl(controllerId, localArtifact, Artifact.UrlProtocol.COAP);

        assertEquals("coap://127.0.0.1:5683/fw/" + tenantAware.getCurrentTenant() + "/" + controllerId + "/sha1/"
                + localArtifact.getSha1Hash(), url);
    }
}
