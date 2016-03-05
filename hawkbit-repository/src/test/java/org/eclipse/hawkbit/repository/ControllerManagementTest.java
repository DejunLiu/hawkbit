/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;



public class ControllerManagementTest extends AbstractIntegrationTest {

    @Test
    
    public void controllerAddsActionStatus() {
        final Target target = new Target("4712");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        Target savedTarget = targetManagement.createTarget(target);

        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);

        assertThat(savedTarget.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);

        savedTarget = deploymentManagement.assignDistributionSet(ds, toAssign).getAssignedTargets().iterator().next();
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        assertThat(targetManagement.findTargetByControllerID(savedTarget.getControllerId()).getTargetInfo()
                .getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        ActionStatus actionStatusMessage = new ActionStatus(savedAction, Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage.addMessage("foobar");
        savedAction.setStatus(Status.RUNNING);
        controllerManagament.addUpdateActionStatus(actionStatusMessage, savedAction);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        actionStatusMessage = new ActionStatus(savedAction, Action.Status.FINISHED, System.currentTimeMillis());
        actionStatusMessage.addMessage(RandomStringUtils.randomAscii(512));
        savedAction.setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(actionStatusMessage, savedAction);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        assertThat(actionStatusRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(3);
        assertThat(deploymentManagement.findActionStatusMessagesByActionInDescOrder(pageReq, savedAction, false)
                .getNumberOfElements()).isEqualTo(3);
    }

    @Test
    
    public void tryToFinishUpdateProcessMoreThenOnce() {

        // mock
        final Target target = new Target("Rabbit");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        Target savedTarget = targetManagement.createTarget(target);
        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);
        savedTarget = deploymentManagement.assignDistributionSet(ds, toAssign).getAssignedTargets().iterator().next();
        Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        // test and verify
        final ActionStatus actionStatusMessage = new ActionStatus(savedAction, Action.Status.RUNNING,
                System.currentTimeMillis());
        actionStatusMessage.addMessage("running");
        savedAction = controllerManagament.addUpdateActionStatus(actionStatusMessage, savedAction);
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final ActionStatus actionStatusMessage2 = new ActionStatus(savedAction, Action.Status.ERROR,
                System.currentTimeMillis());
        actionStatusMessage2.addMessage("error");
        savedAction = controllerManagament.addUpdateActionStatus(actionStatusMessage2, savedAction);
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);

        final ActionStatus actionStatusMessage3 = new ActionStatus(savedAction, Action.Status.FINISHED,
                System.currentTimeMillis());
        actionStatusMessage3.addMessage("finish");
        savedAction = controllerManagament.addUpdateActionStatus(actionStatusMessage3, savedAction);

        targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus();

        // test
        assertThat(targetManagement.findTargetByControllerID("Rabbit").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);

    }
}
