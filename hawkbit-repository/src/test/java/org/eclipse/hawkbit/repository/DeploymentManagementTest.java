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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.Constants;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ActionWithStatusCount;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Test class testing the functionality of triggering a deployment of
 * {@link DistributionSet}s to {@link Target}s.
 *
 */

public class DeploymentManagementTest extends AbstractIntegrationTest {

    @Autowired
    private EventBus eventBus;

    @Test

    public void findActionsWithStatusCountByTarget() {
        final DistributionSet testDs = TestDataUtil.generateDistributionSet("TestDs", "1.0", softwareManagement,
                distributionSetManagement, new ArrayList<DistributionSetTag>());
        final List<Target> testTarget = targetManagement.createTargets(TestDataUtil.generateTargets(1));
        // one action with one action status is generated
        final Action action = deploymentManagement.assignDistributionSet(testDs, testTarget).getActions().get(0);
        // save 2 action status
        actionStatusRepository.save(new ActionStatus(action, Status.RETRIEVED, System.currentTimeMillis()));
        actionStatusRepository.save(new ActionStatus(action, Status.RUNNING, System.currentTimeMillis()));

        final List<ActionWithStatusCount> findActionsWithStatusCountByTarget = deploymentManagement
                .findActionsWithStatusCountByTargetOrderByIdDesc(testTarget.get(0));

        assertThat(findActionsWithStatusCountByTarget).hasSize(1);
        assertThat(findActionsWithStatusCountByTarget.get(0).getActionStatusCount()).isEqualTo(3L);
    }

    @Test

    public void assignAndUnassignDistributionSetToTag() {
        final List<Long> assignDS = new ArrayList<Long>();
        for (int i = 0; i < 4; i++) {
            assignDS.add(TestDataUtil.generateDistributionSet("DS" + i, "1.0", softwareManagement,
                    distributionSetManagement, new ArrayList<DistributionSetTag>()).getId());
        }
        // not exists
        assignDS.add(Long.valueOf(100));
        final DistributionSetTag tag = tagManagement.createDistributionSetTag(new DistributionSetTag("Tag1"));

        final List<DistributionSet> assignedDS = distributionSetManagement.assignTag(assignDS, tag);
        assertThat(assignedDS.size()).isEqualTo(4);
        assignedDS.forEach(ds -> assertThat(ds.getTags().size()).isEqualTo(1));

        DistributionSetTag findDistributionSetTag = tagManagement.findDistributionSetTag("Tag1");
        assertThat(assignedDS.size()).isEqualTo(findDistributionSetTag.getAssignedToDistributionSet().size());

        assertThat(distributionSetManagement.unAssignTag(Long.valueOf(100), findDistributionSetTag)).isNull();

        final DistributionSet unAssignDS = distributionSetManagement.unAssignTag(assignDS.get(0),
                findDistributionSetTag);
        assertThat(unAssignDS.getId()).isEqualTo(assignDS.get(0));
        assertThat(unAssignDS.getTags().size()).isEqualTo(0);
        findDistributionSetTag = tagManagement.findDistributionSetTag("Tag1");
        assertThat(findDistributionSetTag.getAssignedToDistributionSet().size()).isEqualTo(3);

        final List<DistributionSet> unAssignTargets = distributionSetManagement
                .unAssignAllDistributionSetsByTag(findDistributionSetTag);
        findDistributionSetTag = tagManagement.findDistributionSetTag("Tag1");
        assertThat(findDistributionSetTag.getAssignedToDistributionSet().size()).isEqualTo(0);
        assertThat(unAssignTargets.size()).isEqualTo(3);
        unAssignTargets.forEach(target -> assertThat(target.getTags().size()).isEqualTo(0));
    }

    @Test
    public void multiAssigmentHistoryOverMultiplePagesResultsInTwoActiveAction() {

        final DistributionSet cancelDs = TestDataUtil.generateDistributionSet("Canceled DS", "1.0", softwareManagement,
                distributionSetManagement, new ArrayList<DistributionSetTag>());

        final DistributionSet cancelDs2 = TestDataUtil.generateDistributionSet("Canceled DS", "1.2", softwareManagement,
                distributionSetManagement, new ArrayList<DistributionSetTag>());

        List<Target> targets = targetManagement
                .createTargets(TestDataUtil.generateTargets(Constants.MAX_ENTRIES_IN_STATEMENT + 10));

        targets = deploymentManagement.assignDistributionSet(cancelDs, targets).getAssignedTargets();
        targets = deploymentManagement.assignDistributionSet(cancelDs2, targets).getAssignedTargets();

        targetManagement.findAllTargetIds().forEach(targetIdName -> {
            assertThat(deploymentManagement.findActiveActionsByTarget(
                    targetManagement.findTargetByControllerID(targetIdName.getControllerId()))).hasSize(2);
        });
    }

    @Test
    public void manualCancelWithMultipleAssignmentsCancelLastOneFirst() {
        Target target = new Target("4712");
        final DistributionSet dsFirst = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement, true);
        dsFirst.setRequiredMigrationStep(true);
        final DistributionSet dsSecond = TestDataUtil.generateDistributionSet("2", softwareManagement,
                distributionSetManagement, true);
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);

        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        // check initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);

        // assign the two sets in a row
        Action firstAction = assignSet(target, dsFirst);
        Action secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).hasSize(2);
        assertThat(actionStatusRepository.findAll()).hasSize(2);

        // we cancel second -> back to first
        deploymentManagement.cancelAction(secondAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        secondAction = deploymentManagement.findActionWithDetails(secondAction.getId());
        // confirm cancellation
        secondAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(secondAction, Status.CANCELED, System.currentTimeMillis()), secondAction);
        assertThat(actionStatusRepository.findAll()).hasSize(4);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet()).isEqualTo(dsFirst);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel first -> back to installed
        deploymentManagement.cancelAction(firstAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        firstAction = deploymentManagement.findActionWithDetails(firstAction.getId());
        // confirm cancellation
        firstAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(firstAction, Status.CANCELED, System.currentTimeMillis()), firstAction);
        assertThat(actionStatusRepository.findAll()).hasSize(6);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .isEqualTo(dsInstalled);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test
    public void manualCancelWithMultipleAssignmentsCancelMiddleOneFirst() {
        Target target = new Target("4712");
        final DistributionSet dsFirst = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement, true);
        dsFirst.setRequiredMigrationStep(true);
        final DistributionSet dsSecond = TestDataUtil.generateDistributionSet("2", softwareManagement,
                distributionSetManagement, true);
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);

        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        // check initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);

        // assign the two sets in a row
        Action firstAction = assignSet(target, dsFirst);
        Action secondAction = assignSet(target, dsSecond);

        assertThat(actionRepository.findAll()).hasSize(2);
        assertThat(actionStatusRepository.findAll()).hasSize(2);

        // we cancel first -> second is left
        deploymentManagement.cancelAction(firstAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        // confirm cancellation
        firstAction = deploymentManagement.findActionWithDetails(firstAction.getId());
        firstAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(firstAction, Status.CANCELED, System.currentTimeMillis()), firstAction);
        assertThat(actionStatusRepository.findAll()).hasSize(4);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet()).isEqualTo(dsSecond);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        // we cancel second -> remain assigned until finished cancellation
        deploymentManagement.cancelAction(secondAction,
                targetManagement.findTargetByControllerID(target.getControllerId()));
        secondAction = deploymentManagement.findActionWithDetails(secondAction.getId());
        assertThat(actionStatusRepository.findAll()).hasSize(5);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet()).isEqualTo(dsSecond);
        // confirm cancellation
        secondAction.setStatus(Status.CANCELED);
        controllerManagement.addCancelActionStatus(
                new ActionStatus(secondAction, Status.CANCELED, System.currentTimeMillis()), secondAction);
        // cancelled success -> back to dsInstalled
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .isEqualTo(dsInstalled);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test

    public void forceQuitSetActionToInactive() throws InterruptedException {

        Target target = new Target("4712");
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);
        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        final DistributionSet ds = TestDataUtil
                .generateDistributionSet("newDS", softwareManagement, distributionSetManagement, true)
                .setRequiredMigrationStep(true);

        // verify initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);

        Action assigningAction = assignSet(target, ds);

        // verify assignment
        assertThat(actionRepository.findAll()).hasSize(1);
        assertThat(actionStatusRepository.findAll()).hasSize(1);

        target = targetManagement.findTargetByControllerID(target.getControllerId());

        // force quit assignment
        deploymentManagement.cancelAction(assigningAction, target);
        assigningAction = deploymentManagement.findActionWithDetails(assigningAction.getId());

        deploymentManagement.forceQuitAction(assigningAction, target);

        assigningAction = deploymentManagement.findActionWithDetails(assigningAction.getId());

        // verify
        assertThat(assigningAction.getStatus()).isEqualTo(Status.CANCELED);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet())
                .isEqualTo(dsInstalled);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    @Test

    public void forceQuitNotAllowedThrowsException() {

        Target target = new Target("4712");
        final DistributionSet dsInstalled = TestDataUtil.generateDistributionSet("installed", softwareManagement,
                distributionSetManagement, true);
        target.getTargetInfo().setInstalledDistributionSet(dsInstalled);
        target = targetManagement.createTarget(target);

        final DistributionSet ds = TestDataUtil
                .generateDistributionSet("newDS", softwareManagement, distributionSetManagement, true)
                .setRequiredMigrationStep(true);

        // verify initial status
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);

        final Action assigningAction = assignSet(target, ds);

        // verify assignment
        assertThat(actionRepository.findAll()).hasSize(1);
        assertThat(actionStatusRepository.findAll()).hasSize(1);

        // force quit assignment
        try {
            deploymentManagement.forceQuitAction(assigningAction,
                    targetManagement.findTargetByControllerID(target.getControllerId()));
            fail("expected ForceQuitActionNotAllowedException");
        } catch (final ForceQuitActionNotAllowedException ex) {
        }
    }

    private Action assignSet(final Target target, final DistributionSet ds) {
        deploymentManagement.assignDistributionSet(ds.getId(), new String[] { target.getControllerId() });
        assertThat(
                targetManagement.findTargetByControllerID(target.getControllerId()).getTargetInfo().getUpdateStatus())
                        .isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getAssignedDistributionSet())
                .isEqualTo(ds);
        final Action action = actionRepository.findByTargetAndDistributionSet(pageReq, target, ds).getContent().get(0);
        assertThat(action).isNotNull();
        return action;
    }

    /**
     * test a simple deployment by calling the
     * {@link TargetRepository#assignDistributionSet(DistributionSet, Iterable)}
     * and checking the active action and the action history of the targets.
     * 
     * @throws InterruptedException
     */
    @Test

    public void assignDistributionSet2Targets() throws InterruptedException {

        final EventHandlerMock eventHandlerMock = new EventHandlerMock(20);
        eventBus.register(eventHandlerMock);

        final String myCtrlIDPref = "myCtrlID";
        final Iterable<Target> savedNakedTargets = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(10, myCtrlIDPref, "first description"));

        final String myDeployedCtrlIDPref = "myDeployedCtrlID";
        final List<Target> savedDeployedTargets = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(20, myDeployedCtrlIDPref, "first description"));

        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        deploymentManagement.assignDistributionSet(ds, savedDeployedTargets);

        // verify that one Action for each assignDistributionSet
        assertThat(actionRepository.findAll(pageReq).getNumberOfElements()).isEqualTo(20);

        final Iterable<Target> allFoundTargets = targetManagement.findTargetsAll(pageReq).getContent();

        assertThat(allFoundTargets).containsAll(savedDeployedTargets).containsAll(savedNakedTargets);
        assertThat(savedDeployedTargets).doesNotContain(Iterables.toArray(savedNakedTargets, Target.class));
        assertThat(savedNakedTargets).doesNotContain(Iterables.toArray(savedDeployedTargets, Target.class));

        for (final Target myt : savedNakedTargets) {
            final Target t = targetManagement.findTargetByControllerID(myt.getControllerId());
            assertThat(deploymentManagement.findActionsByTarget(t)).isEmpty();
        }

        for (final Target myt : savedDeployedTargets) {
            final Target t = targetManagement.findTargetByControllerID(myt.getControllerId());
            final List<Action> activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(t);
            assertThat(activeActionsByTarget).isNotEmpty();
            assertThat(t.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
            for (final Action ua : activeActionsByTarget) {
                assertThat(ua.getDistributionSet()).isEqualTo(ds);
            }
        }

        final List<TargetAssignDistributionSetEvent> events = eventHandlerMock.getEvents(10, TimeUnit.SECONDS);

        assertTargetAssignDistributionSetEvents(savedDeployedTargets, ds, events);
    }

    @Test

    public void failDistributionSetAssigmentThatIsNotComplete() throws InterruptedException {
        final EventHandlerMock eventHandlerMock = new EventHandlerMock(0);
        eventBus.register(eventHandlerMock);

        final List<Target> targets = targetManagement.createTargets(TestDataUtil.generateTargets(10));

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new SoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2", null, ""));

        final DistributionSet incomplete = distributionSetManagement.createDistributionSet(
                new DistributionSet("incomplete", "v1", "", standardDsType, Lists.newArrayList(ah, jvm)));

        try {
            deploymentManagement.assignDistributionSet(incomplete, targets);
            fail("expected IncompleteDistributionSetException");
        } catch (final IncompleteDistributionSetException ex) {
        }

        incomplete.addModule(os);
        final DistributionSet nowComplete = distributionSetManagement.updateDistributionSet(incomplete);

        // give some chance to receive events asynchronously
        Thread.sleep(300);
        final List<TargetAssignDistributionSetEvent> events = eventHandlerMock.getEvents(1, TimeUnit.MILLISECONDS);
        assertThat(events).isEmpty();

        final EventHandlerMock eventHandlerMockAfterCompletionOfDs = new EventHandlerMock(10);
        eventBus.register(eventHandlerMockAfterCompletionOfDs);

        assertThat(deploymentManagement.assignDistributionSet(nowComplete, targets).getAssigned()).isEqualTo(10);
        assertTargetAssignDistributionSetEvents(targets, nowComplete,
                eventHandlerMockAfterCompletionOfDs.getEvents(10, TimeUnit.SECONDS));
    }

    @Test
    public void mutipleDeployments() throws InterruptedException {
        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 5;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        // Each of the four targets get one assignment (4 * 1 = 4)
        final int expectedNumberOfEventsForAssignment = 4;
        final EventHandlerMock eventHandlerMock = new EventHandlerMock(expectedNumberOfEventsForAssignment);
        eventBus.register(eventHandlerMock);

        // Each of the four targets get two more assignment the which are
        // cancelled (4 * 2 = 8)
        final int expectedNumberOfEventsForCancel = 8;
        final CancelEventHandlerMock cancelEventHandlerMock = new CancelEventHandlerMock(
                expectedNumberOfEventsForCancel);
        eventBus.register(cancelEventHandlerMock);

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        final List<Long> deployedTargetIDs = deploymentResult.getDeployedTargetIDs();
        final List<Long> undeployedTargetIDs = deploymentResult.getUndeployedTargetIDs();
        final List<Target> savedNakedTargets = deploymentResult.getUndeployedTargets();
        final List<Target> savedDeployedTargets = deploymentResult.getDeployedTargets();

        // retrieving all Actions created by the assignDistributionSet call
        final Page<Action> page = actionRepository.findAll(pageReq);
        // and verify the number
        assertThat(page.getTotalElements()).isEqualTo(noOfDeployedTargets * noOfDistributionSets);

        // only records retrieved from the DB can be evaluated to be sure that
        // all fields are
        // populated;
        final Iterable<Target> allFoundTargets = targetRepository.findAll();

        final Iterable<Target> deployedTargetsFromDB = targetRepository.findAll(deployedTargetIDs);
        final Iterable<Target> undeployedTargetsFromDB = targetRepository.findAll(undeployedTargetIDs);

        // test that number of Targets
        assertThat(allFoundTargets.spliterator().getExactSizeIfKnown())
                .isEqualTo(deployedTargetsFromDB.spliterator().getExactSizeIfKnown()
                        + undeployedTargetsFromDB.spliterator().getExactSizeIfKnown());
        assertThat(deployedTargetsFromDB.spliterator().getExactSizeIfKnown()).isEqualTo(noOfDeployedTargets);
        assertThat(undeployedTargetsFromDB.spliterator().getExactSizeIfKnown()).isEqualTo(noOfUndeployedTargets);

        // test the content of different lists
        assertThat(allFoundTargets).containsAll(deployedTargetsFromDB).containsAll(undeployedTargetsFromDB);
        assertThat(deployedTargetsFromDB).containsAll(savedDeployedTargets)
                .doesNotContain(Iterables.toArray(undeployedTargetsFromDB, Target.class));
        assertThat(undeployedTargetsFromDB).containsAll(savedNakedTargets)
                .doesNotContain(Iterables.toArray(deployedTargetsFromDB, Target.class));

        // For each of the 4 targets 1 distribution sets gets assigned
        eventHandlerMock.getEvents(10, TimeUnit.SECONDS);

        // For each of the 4 targets 2 distribution sets gets cancelled
        cancelEventHandlerMock.getEvents(10, TimeUnit.SECONDS);

    }

    @Test
    public void assignDistributionSetAndAddFinishedActionStatus() {
        final PageRequest pageRequest = new PageRequest(0, 100, Direction.ASC, ActionStatusFields.ID.getFieldName());

        final DeploymentResult deployResWithDsA = prepareComplexRepo("undep-A-T", 2, "dep-A-T", 4, 1, "dsA");
        final DeploymentResult deployResWithDsB = prepareComplexRepo("undep-B-T", 3, "dep-B-T", 5, 1, "dsB");
        final DeploymentResult deployResWithDsC = prepareComplexRepo("undep-C-T", 4, "dep-C-T", 6, 1, "dsC");

        // keep a reference to the created DistributionSets
        final DistributionSet dsA = deployResWithDsA.getDistributionSets().get(0);
        final DistributionSet dsB = deployResWithDsB.getDistributionSets().get(0);
        final DistributionSet dsC = deployResWithDsC.getDistributionSets().get(0);

        // retrieving the UpdateActions created by the assignments
        final Action updActA = actionRepository.findByDistributionSet(pageRequest, dsA).getContent().get(0);
        final Action updActB = actionRepository.findByDistributionSet(pageRequest, dsB).getContent().get(0);
        final Action updActC = actionRepository.findByDistributionSet(pageRequest, dsC).getContent().get(0);

        // verifying the correctness of the assignments
        for (final Target t : deployResWithDsA.getDeployedTargets()) {
            assertThat(t.getAssignedDistributionSet().getId()).isEqualTo(dsA.getId());
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).isNull();
        }
        for (final Target t : deployResWithDsB.getDeployedTargets()) {
            assertThat(t.getAssignedDistributionSet().getId()).isEqualTo(dsB.getId());
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).isNull();
        }
        for (final Target t : deployResWithDsC.getDeployedTargets()) {
            assertThat(t.getAssignedDistributionSet().getId()).isEqualTo(dsC.getId());
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).isNull();
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
        }

        final List<Target> updatedTsDsA = sendUpdateActionStatusToTargets(dsA, deployResWithDsA.getDeployedTargets(),
                Status.FINISHED, new String[] { "alles gut" });

        // verify, that dsA is deployed correctly
        assertThat(updatedTsDsA).isEqualTo(deployResWithDsA.getDeployedTargets());
        for (final Target t_ : updatedTsDsA) {
            final Target t = targetManagement.findTargetByControllerID(t_.getControllerId());
            assertThat(t.getAssignedDistributionSet()).isEqualTo(dsA);
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).isEqualTo(dsA);
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.IN_SYNC);
            assertThat(deploymentManagement.findActiveActionsByTarget(t)).hasSize(0);
        }

        // deploy dsA to the target which already have dsB deployed -> must
        // remove updActB from
        // activeActions, add a corresponding cancelAction and another
        // UpdateAction for dsA
        final Iterable<Target> deployed2DS = deploymentManagement
                .assignDistributionSet(dsA, deployResWithDsB.getDeployedTargets()).getAssignedTargets();
        final Action updActA2 = actionRepository.findByDistributionSet(pageRequest, dsA).getContent().get(1);

        assertThat(deployed2DS).containsAll(deployResWithDsB.getDeployedTargets());
        assertThat(deployed2DS).hasSameSizeAs(deployResWithDsB.getDeployedTargets());

        for (final Target t_ : deployed2DS) {
            final Target t = targetManagement.findTargetByControllerID(t_.getControllerId());
            assertThat(t.getAssignedDistributionSet()).isEqualTo(dsA);
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo()
                    .getInstalledDistributionSet()).isNull();
            assertThat(targetManagement.findTargetByControllerID(t.getControllerId()).getTargetInfo().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);

        }
    }

    /**
     * test the deletion of {@link DistributionSet}s including exception in case
     * of {@link Target}s are assigned by
     * {@link Target#getAssignedDistributionSet()} or
     * {@link Target#getInstalledDistributionSet()}
     */
    @Test
    public void deleteDistributionSet() {

        final PageRequest pageRequest = new PageRequest(0, 100, Direction.ASC, "id");

        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 2;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement, distributionSetManagement);

        distributionSetManagement.deleteDistributionSet(dsA.getId());
        dsA = distributionSetManagement.findDistributionSetById(dsA.getId());
        assertThat(dsA).isNull();

        // // verify that the ds is not physically deleted
        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            distributionSetManagement.deleteDistributionSet(ds.getId());
            final DistributionSet foundDS = distributionSetManagement.findDistributionSetById(ds.getId());
            assertThat(foundDS).isNotNull();
            assertThat(foundDS.isDeleted()).isTrue();
        }

        // verify that deleted attribute is used correctly
        List<DistributionSet> allFoundDS = distributionSetManagement.findDistributionSetsAll(pageReq, false, true)
                .getContent();
        assertThat(allFoundDS.size()).isEqualTo(0);
        allFoundDS = distributionSetManagement.findDistributionSetsAll(pageRequest, true, true).getContent();
        assertThat(allFoundDS).hasSize(noOfDistributionSets);

        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            sendUpdateActionStatusToTargets(ds, deploymentResult.getDeployedTargets(), Status.FINISHED,
                    "blabla alles gut");
        }
        // try to delete again
        distributionSetManagement.deleteDistributionSet(deploymentResult.getDistributionSetIDs()
                .toArray(new Long[deploymentResult.getDistributionSetIDs().size()]));
        // verify that the result is the same, even though distributionSet dsA
        // has been installed
        // successfully and no activeAction is referring to created distribution
        // sets
        allFoundDS = distributionSetManagement.findDistributionSetsAll(pageRequest, false, true).getContent();
        assertThat(allFoundDS.size()).isEqualTo(0);
        allFoundDS = distributionSetManagement.findDistributionSetsAll(pageRequest, true, true).getContent();
        assertThat(allFoundDS).hasSize(noOfDistributionSets);

    }

    @Test

    public void deletesTargetsAndVerifyCascadeDeletes() {

        final String undeployedTargetPrefix = "undep-T";
        final int noOfUndeployedTargets = 2;

        final String deployedTargetPrefix = "dep-T";
        final int noOfDeployedTargets = 4;

        final int noOfDistributionSets = 3;

        final DeploymentResult deploymentResult = prepareComplexRepo(undeployedTargetPrefix, noOfUndeployedTargets,
                deployedTargetPrefix, noOfDeployedTargets, noOfDistributionSets, "myTestDS");

        for (final DistributionSet ds : deploymentResult.getDistributionSets()) {
            sendUpdateActionStatusToTargets(ds, deploymentResult.getDeployedTargets(), Status.FINISHED,
                    "blabla alles gut");
        }

        assertThat(targetManagement.countTargetsAll()).isNotZero();
        assertThat(actionStatusRepository.count()).isNotZero();

        targetManagement
                .deleteTargets(deploymentResult.getUndeployedTargetIDs().toArray(new Long[noOfUndeployedTargets]));
        targetManagement.deleteTargets(deploymentResult.getDeployedTargetIDs().toArray(new Long[noOfDeployedTargets]));

        assertThat(targetManagement.countTargetsAll()).isZero();
        assertThat(actionStatusRepository.count()).isZero();
    }

    private List<Target> sendUpdateActionStatusToTargets(final DistributionSet dsA, final Iterable<Target> targs,
            final Status status, final String... msgs) {
        final List<Target> result = new ArrayList<Target>();
        for (final Target t : targs) {
            final List<Action> findByTarget = actionRepository.findByTarget(t);
            for (final Action action : findByTarget) {
                result.add(sendUpdateActionStatusToTarget(status, action, t, msgs));
            }
        }
        return result;
    }

    private Target sendUpdateActionStatusToTarget(final Status status, final Action updActA, final Target t,
            final String... msgs) {
        updActA.setStatus(status);

        final ActionStatus statusMessages = new ActionStatus();
        statusMessages.setAction(updActA);
        statusMessages.setOccurredAt(System.currentTimeMillis());
        statusMessages.setStatus(status);
        for (final String msg : msgs) {
            statusMessages.addMessage(msg);
        }
        controllerManagament.addUpdateActionStatus(statusMessages, updActA);
        return targetManagement.findTargetByControllerID(t.getControllerId());
    }

    @Test

    public void alternatingAssignmentAndAddUpdateActionStatus() {

        final DistributionSet dsA = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        final DistributionSet dsB = TestDataUtil.generateDistributionSet("b", softwareManagement,
                distributionSetManagement);
        Target targ = targetManagement
                .createTarget(TestDataUtil.buildTargetFixture("target-id-A", "first description"));

        List<Target> targs = new ArrayList<Target>();
        targs.add(targ);

        // doing the assignment
        targs = deploymentManagement.assignDistributionSet(dsA, targs).getAssignedTargets();
        targ = targetManagement.findTargetByControllerID(targs.iterator().next().getControllerId());

        // checking the revisions of the created entities
        // verifying that the revision of the object and the revision within the
        // DB has not changed
        assertThat(dsA.getOptLockRevision()).isEqualTo(
                distributionSetManagement.findDistributionSetByIdWithDetails(dsA.getId()).getOptLockRevision());

        // verifying that the assignment is correct
        assertEquals(1, deploymentManagement.findActiveActionsByTarget(targ).size());
        assertEquals(1, deploymentManagement.findActionsByTarget(targ).size());
        assertEquals(TargetUpdateStatus.PENDING, targ.getTargetInfo().getUpdateStatus());
        assertEquals(dsA, targ.getAssignedDistributionSet());
        assertEquals(dsA, deploymentManagement.findActiveActionsByTarget(targ).get(0).getDistributionSet());
        assertNull(targ.getTargetInfo().getInstalledDistributionSet());

        final Page<Action> updAct = actionRepository.findByDistributionSet(pageReq, dsA);
        final Action action = updAct.getContent().get(0);
        action.setStatus(Status.FINISHED);
        final ActionStatus statusMessage = new ActionStatus(action, Status.FINISHED, System.currentTimeMillis(), "");
        controllerManagament.addUpdateActionStatus(statusMessage, action);

        targ = targetManagement.findTargetByControllerID(targ.getControllerId());

        assertEquals(0, deploymentManagement.findActiveActionsByTarget(targ).size());
        // try {
        assertEquals(1, deploymentManagement.findInActiveActionsByTarget(targ).size());
        // }
        // catch( final LazyInitializationException ex ) {
        //
        // }
        assertEquals(TargetUpdateStatus.IN_SYNC, targ.getTargetInfo().getUpdateStatus());
        assertEquals(dsA, targ.getAssignedDistributionSet());
        assertEquals(dsA, targ.getTargetInfo().getInstalledDistributionSet());

        targs = deploymentManagement.assignDistributionSet(dsB.getId(), new String[] { "target-id-A" })
                .getAssignedTargets();

        targ = targs.iterator().next();

        assertEquals(1, deploymentManagement.findActiveActionsByTarget(targ).size());
        assertEquals(TargetUpdateStatus.PENDING,
                targetManagement.findTargetByControllerID(targ.getControllerId()).getTargetInfo().getUpdateStatus());
        assertEquals(dsB, targ.getAssignedDistributionSet());
        assertEquals(dsA.getId(), targetManagement.findTargetByControllerIDWithDetails(targ.getControllerId())
                .getTargetInfo().getInstalledDistributionSet().getId());
        assertEquals(dsB, deploymentManagement.findActiveActionsByTarget(targ).get(0).getDistributionSet());

    }

    @Test
    public void checkThatDsRevisionsIsNotChangedWithTargetAssignment() {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        final DistributionSet dsB = TestDataUtil.generateDistributionSet("b", softwareManagement,
                distributionSetManagement);
        Target targ = targetManagement
                .createTarget(TestDataUtil.buildTargetFixture("target-id-A", "first description"));

        assertThat(dsA.getOptLockRevision()).isEqualTo(
                distributionSetManagement.findDistributionSetByIdWithDetails(dsA.getId()).getOptLockRevision());

        final List<Target> targs = new ArrayList<Target>();
        targs.add(targ);
        final Iterable<Target> savedTargs = deploymentManagement.assignDistributionSet(dsA, targs).getAssignedTargets();
        targ = savedTargs.iterator().next();

        assertThat(dsA.getOptLockRevision()).isEqualTo(
                distributionSetManagement.findDistributionSetByIdWithDetails(dsA.getId()).getOptLockRevision());
    }

    @Test

    public void forceSoftAction() {
        // prepare
        final Target target = targetManagement.createTarget(new Target("knownControllerId"));
        final DistributionSet ds = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = deploymentManagement
                .assignDistributionSet(ds.getId(), ActionType.SOFT, Action.NO_FORCE_TIME, target.getControllerId());
        final Action action = assignDistributionSet.getActions().get(0);
        // verify preparation
        Action findAction = deploymentManagement.findAction(action.getId());
        assertThat(findAction.getActionType()).isEqualTo(ActionType.SOFT);

        // test
        deploymentManagement.forceTargetAction(action.getId());

        // verify test
        findAction = deploymentManagement.findAction(action.getId());
        assertThat(findAction.getActionType()).isEqualTo(ActionType.FORCED);
    }

    @Test

    public void forceAlreadyForcedActionNothingChanges() {
        // prepare
        final Target target = targetManagement.createTarget(new Target("knownControllerId"));
        final DistributionSet ds = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);
        // assign ds to create an action
        final DistributionSetAssignmentResult assignDistributionSet = deploymentManagement
                .assignDistributionSet(ds.getId(), ActionType.FORCED, Action.NO_FORCE_TIME, target.getControllerId());
        final Action action = assignDistributionSet.getActions().get(0);
        // verify perparation
        Action findAction = deploymentManagement.findAction(action.getId());
        assertThat(findAction.getActionType()).isEqualTo(ActionType.FORCED);

        // test
        final Action forceTargetAction = deploymentManagement.forceTargetAction(action.getId());

        // verify test
        assertThat(forceTargetAction.getActionType()).isEqualTo(ActionType.FORCED);
        findAction = deploymentManagement.findAction(action.getId());
        assertThat(findAction.getActionType()).isEqualTo(ActionType.FORCED);
    }

    /**
     * Helper methods that creates 2 lists of targets and a list of distribution
     * sets.
     * <p>
     * <b>All created distribution sets are assigned to all targets of the
     * target list deployedTargets.</b>
     * 
     * @param undeployedTargetPrefix
     *            prefix to be used as target controller prefix
     * @param noOfUndeployedTargets
     *            number of targets which remain undeployed
     * @param deployedTargetPrefix
     *            prefix to be used as target controller prefix
     * @param noOfDeployedTargets
     *            number of targets to which the created distribution sets
     *            assigned
     * @param noOfDistributionSets
     *            number of distribution sets
     * @param distributionSetPrefix
     *            prefix for the created distribution sets
     * @return the {@link DeploymentResult} containing all created targets, the
     *         distribution sets, the corresponding IDs for later evaluation in
     *         tests
     */
    private DeploymentResult prepareComplexRepo(final String undeployedTargetPrefix, final int noOfUndeployedTargets,
            final String deployedTargetPrefix, final int noOfDeployedTargets, final int noOfDistributionSets,
            final String distributionSetPrefix) {
        final Iterable<Target> nakedTargets = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(noOfUndeployedTargets, undeployedTargetPrefix, "first description"));

        List<Target> deployedTargets = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(noOfDeployedTargets, deployedTargetPrefix, "first description"));

        // creating 10 DistributionSets
        final List<DistributionSet> dsList = TestDataUtil.generateDistributionSets(distributionSetPrefix,
                noOfDistributionSets, softwareManagement, distributionSetManagement);
        String time = String.valueOf(System.currentTimeMillis());
        time = time.substring(time.length() - 5);

        // assigning all DistributionSet to the Target in the list
        // deployedTargets
        for (final DistributionSet ds : dsList) {
            deployedTargets = deploymentManagement.assignDistributionSet(ds, deployedTargets).getAssignedTargets();
        }

        final DeploymentResult deploymentResult = new DeploymentResult(deployedTargets, nakedTargets, dsList,
                deployedTargetPrefix, undeployedTargetPrefix, distributionSetPrefix);
        return deploymentResult;

    }

    private void assertTargetAssignDistributionSetEvents(final List<Target> targets, final DistributionSet ds,
            final List<TargetAssignDistributionSetEvent> events) {
        for (final Target myt : targets) {
            boolean found = false;
            for (final TargetAssignDistributionSetEvent event : events) {
                if (event.getControllerId().equals(myt.getControllerId())) {
                    found = true;
                    final List<Action> activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(myt);
                    assertThat(activeActionsByTarget).isNotEmpty();
                    assertThat(event.getActionId()).isEqualTo(activeActionsByTarget.get(0).getId())
                            .as("Action id in database and event do not match");
                    assertThat(event.getSoftwareModules())
                            .containsOnly(ds.getModules().toArray(new SoftwareModule[ds.getModules().size()]));
                }
            }
            assertThat(found).isTrue().as("No event found for controller " + myt.getControllerId());
        }
    }

    /**
     *
     *
     */
    private class DeploymentResult

    {
        final List<Long> deployedTargetIDs = new ArrayList<Long>();
        final List<Long> undeployedTargetIDs = new ArrayList<Long>();
        final List<Long> distributionSetIDs = new ArrayList<Long>();

        private final List<Target> undeployedTargets = new ArrayList<Target>();
        private final List<Target> deployedTargets = new ArrayList<Target>();
        private final List<DistributionSet> distributionSets = new ArrayList<DistributionSet>();

        private final String undeployedTargetPrefix;
        private final String deployedTargetPrefix;
        private final String distributionSetPrefix;

        public DeploymentResult(final Iterable<Target> deployedTs, final Iterable<Target> undeployedTs,
                final Iterable<DistributionSet> dss, final String deployedTargetPrefix,
                final String undeployedTargetPrefix, final String distributionSetPrefix) {

            this.undeployedTargetPrefix = undeployedTargetPrefix;
            this.deployedTargetPrefix = deployedTargetPrefix;
            this.distributionSetPrefix = distributionSetPrefix;

            Iterables.addAll(deployedTargets, deployedTs);
            Iterables.addAll(undeployedTargets, undeployedTs);
            Iterables.addAll(distributionSets, dss);

            deployedTargets.forEach(t -> deployedTargetIDs.add(t.getId()));

            undeployedTargets.forEach(t -> undeployedTargetIDs.add(t.getId()));

            distributionSets.forEach(ds -> distributionSetIDs.add(ds.getId()));

        }

        /**
         * @return the distributionSetIDs
         */
        public List<Long> getDistributionSetIDs() {
            return distributionSetIDs;
        }

        /**
         * @return
         */
        public List<Long> getDeployedTargetIDs() {
            return deployedTargetIDs;
        }

        /**
         * @return
         */
        public List<Target> getUndeployedTargets() {
            return undeployedTargets;
        }

        /**
         * @return
         */
        public List<DistributionSet> getDistributionSets() {
            return distributionSets;
        }

        /**
         * @return
         */
        public List<Target> getDeployedTargets() {
            return deployedTargets;
        }

        /**
         * @return the noOfUndeployedTargets
         */
        public int getNoOfUndeployedTargets() {
            return undeployedTargetIDs.size();
        }

        /**
         * @return the noOfDeployedTargets
         */
        public int getNoOfDeployedTargets() {
            return deployedTargetIDs.size();
        }

        /**
         * @return the noOfDistributionSets
         */
        public int getNoOfDistributionSets() {
            return distributionSets.size();
        }

        /**
         * @return the undeployedTargetIDs
         */
        public List<Long> getUndeployedTargetIDs() {
            return undeployedTargetIDs;
        }

        /**
         * @return the undeployedTargetPrefix
         */
        public String getUndeployedTargetPrefix() {
            return undeployedTargetPrefix;
        }

        /**
         * @return the deployedTargetPrefix
         */
        public String getDeployedTargetPrefix() {
            return deployedTargetPrefix;
        }

        /**
         * @return the distributionSetPrefix
         */
        public String getDistributionSetPrefix() {
            return distributionSetPrefix;
        }
    }

    private static class EventHandlerMock {
        private final List<TargetAssignDistributionSetEvent> events = Collections
                .synchronizedList(new LinkedList<TargetAssignDistributionSetEvent>());
        private final CountDownLatch latch;
        private final int expectedNumberOfEvents;

        private EventHandlerMock(final int expectedNumberOfEvents) {
            this.expectedNumberOfEvents = expectedNumberOfEvents;
            this.latch = new CountDownLatch(expectedNumberOfEvents);
        }

        @Subscribe
        public void handleEvent(final TargetAssignDistributionSetEvent event) {
            events.add(event);
            latch.countDown();
        }

        public List<TargetAssignDistributionSetEvent> getEvents(final long timeout, final TimeUnit unit)
                throws InterruptedException {
            latch.await(timeout, unit);
            final List<TargetAssignDistributionSetEvent> handledEvents = new LinkedList<TargetAssignDistributionSetEvent>(
                    events);
            assertThat(handledEvents).hasSize(expectedNumberOfEvents)
                    .as("Did not receive the expected amount of events (" + expectedNumberOfEvents
                            + ") within timeout. Received events are " + handledEvents);
            return handledEvents;
        }
    }

    private static class CancelEventHandlerMock {
        private final List<CancelTargetAssignmentEvent> events = Collections
                .synchronizedList(new LinkedList<CancelTargetAssignmentEvent>());
        private final CountDownLatch latch;
        private final int expectedNumberOfEvents;

        private CancelEventHandlerMock(final int expectedNumberOfEvents) {
            this.expectedNumberOfEvents = expectedNumberOfEvents;
            this.latch = new CountDownLatch(expectedNumberOfEvents);
        }

        @Subscribe
        public void handleEvent(final CancelTargetAssignmentEvent event) {
            events.add(event);
            latch.countDown();
        }

        public List<CancelTargetAssignmentEvent> getEvents(final long timeout, final TimeUnit unit)
                throws InterruptedException {
            latch.await(timeout, unit);
            final List<CancelTargetAssignmentEvent> handledEvents = new LinkedList<CancelTargetAssignmentEvent>(events);
            assertThat(handledEvents).hasSize(expectedNumberOfEvents)
                    .as("Did not receive the expected amount of events (" + expectedNumberOfEvents
                            + ") within timeout. Received events are " + handledEvents);
            return handledEvents;
        }
    }

}
