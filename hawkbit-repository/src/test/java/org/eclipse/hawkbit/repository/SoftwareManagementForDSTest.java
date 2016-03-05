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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.repository.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.exception.DistributionSetTypeUndefinedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityLockedException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Lists;

/**
 * {@link SoftwareManagement} test focused on {@link DistributionSet} and
 * {@link DistributionSetType} related stuff.
 *
 *
 *
 */

public class SoftwareManagementForDSTest extends AbstractIntegrationTest {

    @Test

    public void updateUnassignedDistributionSetTypeModules() {
        DistributionSetType updatableType = distributionSetManagement
                .createDistributionSetType(new DistributionSetType("updatableType", "to be deleted", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();

        // add OS
        updatableType.addMandatoryModuleType(osType);
        updatableType = distributionSetManagement.updateDistributionSetType(updatableType);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .containsOnly(osType);

        // add JVM
        updatableType.addMandatoryModuleType(runtimeType);
        updatableType = distributionSetManagement.updateDistributionSetType(updatableType);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .containsOnly(osType, runtimeType);

        // remove OS
        updatableType.removeModuleType(osType.getId());
        updatableType = distributionSetManagement.updateDistributionSetType(updatableType);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .containsOnly(runtimeType);
    }

    @Test

    public void updateAssignedDistributionSetTypeMetaData() {
        final DistributionSetType nonUpdatableType = distributionSetManagement
                .createDistributionSetType(new DistributionSetType("updatableType", "to be deletd", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();
        distributionSetManagement
                .createDistributionSet(new DistributionSet("newtypesoft", "1", "", nonUpdatableType, null));

        nonUpdatableType.setDescription("a new description");
        nonUpdatableType.setColour("test123");

        distributionSetManagement.updateDistributionSetType(nonUpdatableType);

        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getDescription())
                .isEqualTo("a new description");
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getColour())
                .isEqualTo("test123");
    }

    @Test(expected = EntityReadOnlyException.class)

    public void addModuleToAssignedDistributionSetTypeFails() {
        final DistributionSetType nonUpdatableType = distributionSetManagement
                .createDistributionSetType(new DistributionSetType("updatableType", "to be deletd", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();
        distributionSetManagement
                .createDistributionSet(new DistributionSet("newtypesoft", "1", "", nonUpdatableType, null));

        nonUpdatableType.addMandatoryModuleType(osType);
        distributionSetManagement.updateDistributionSetType(nonUpdatableType);
    }

    @Test(expected = EntityReadOnlyException.class)

    public void removeModuleToAssignedDistributionSetTypeFails() {
        DistributionSetType nonUpdatableType = distributionSetManagement
                .createDistributionSetType(new DistributionSetType("updatableType", "to be deletd", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();

        nonUpdatableType.addMandatoryModuleType(osType);
        nonUpdatableType = distributionSetManagement.updateDistributionSetType(nonUpdatableType);
        distributionSetManagement
                .createDistributionSet(new DistributionSet("newtypesoft", "1", "", nonUpdatableType, null));

        nonUpdatableType.removeModuleType(osType.getId());
        nonUpdatableType = distributionSetManagement.updateDistributionSetType(nonUpdatableType);
    }

    @Test

    public void deleteUnassignedDistributionSetType() {
        final DistributionSetType hardDelete = distributionSetManagement
                .createDistributionSetType(new DistributionSetType("deleted", "to be deleted", ""));

        assertThat(distributionSetTypeRepository.findAll()).contains(hardDelete);
        distributionSetManagement.deleteDistributionSetType(hardDelete);

        assertThat(distributionSetTypeRepository.findAll()).doesNotContain(hardDelete);
    }

    @Test

    public void deleteAssignedDistributionSetType() {
        final DistributionSetType softDelete = distributionSetManagement
                .createDistributionSetType(new DistributionSetType("softdeleted", "to be deletd", ""));

        assertThat(distributionSetTypeRepository.findAll()).contains(softDelete);
        final DistributionSet dsNewType = distributionSetManagement
                .createDistributionSet(new DistributionSet("newtypesoft", "1", "", softDelete, null));

        distributionSetManagement.deleteDistributionSetType(softDelete);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("softdeleted").isDeleted()).isEqualTo(true);
    }

    // TODO: kzimmerm: test N+1

    @Test(expected = EntityAlreadyExistsException.class)

    public void createDuplicateDistributionSetsFailsWithException() {
        TestDataUtil.generateDistributionSet("a", softwareManagement, distributionSetManagement);

        TestDataUtil.generateDistributionSet("a", softwareManagement, distributionSetManagement);

    }

    @Test

    public void createDistributionSetMetadata() {
        final String knownKey = "dsMetaKnownKey";
        final String knownValue = "dsMetaKnownValue";

        final DistributionSet ds = TestDataUtil.generateDistributionSet("testDs", softwareManagement,
                distributionSetManagement);

        final DistributionSetMetadata metadata = new DistributionSetMetadata(knownKey, ds, knownValue);
        final DistributionSetMetadata createdMetadata = distributionSetManagement
                .createDistributionSetMetadata(metadata);

        assertThat(createdMetadata).isNotNull();
        assertThat(createdMetadata.getId().getKey()).isEqualTo(knownKey);
        assertThat(createdMetadata.getDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(createdMetadata.getValue()).isEqualTo(knownValue);
    }

    @Test

    public void updateDistributionSetForbiddedWithIllegalUpdate() {
        // prepare data
        Target target = new Target("4711");
        target = targetManagement.createTarget(target);

        SoftwareModule ah2 = new SoftwareModule(appType, "agent-hub2", "1.0.5", null, "");
        SoftwareModule os2 = new SoftwareModule(osType, "poky2", "3.0.3", null, "");

        DistributionSet ds = TestDataUtil.generateDistributionSet("ds-1", softwareManagement,
                distributionSetManagement);

        ah2 = softwareManagement.createSoftwareModule(ah2);
        os2 = softwareManagement.createSoftwareModule(os2);

        // update is allowed as it is still not assigned to a target
        ds.addModule(ah2);
        ds = distributionSetManagement.updateDistributionSet(ds);

        // assign target
        deploymentManagement.assignDistributionSet(ds.getId(), target.getControllerId());
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());

        // description change is still allowed
        ds.setDescription("a different desc");
        ds = distributionSetManagement.updateDistributionSet(ds);

        // description change is still allowed
        ds.setName("a new name");
        ds = distributionSetManagement.updateDistributionSet(ds);

        // description change is still allowed
        ds.setVersion("a new version");
        ds = distributionSetManagement.updateDistributionSet(ds);

        // not allowed as it is assigned now
        ds.addModule(os2);
        try {
            ds = distributionSetManagement.updateDistributionSet(ds);
            fail("Expected EntityLockedException");
        } catch (final EntityLockedException e) {

        }

        // not allowed as it is assigned now
        ds.removeModule(ds.findFirstModuleByType(appType));
        try {
            ds = distributionSetManagement.updateDistributionSet(ds);
            fail("Expected EntityLockedException");
        } catch (final EntityLockedException e) {

        }
    }

    @Test(expected = DistributionSetTypeUndefinedException.class)

    public void updateDistributionSetModuleWithUndefinedTypeFails() {
        final DistributionSet testSet = new DistributionSet();
        final SoftwareModule module = new SoftwareModule(appType, "agent-hub2", "1.0.5", null, "");

        // update data
        testSet.addModule(module);
    }

    @Test(expected = UnsupportedSoftwareModuleForThisDistributionSetException.class)

    public void updateDistributionSetUnsupportedModuleFails() {
        final DistributionSet set = new DistributionSet("agent-hub2", "1.0.5", "desc",
                new DistributionSetType("test", "test", "test").addMandatoryModuleType(osType), null);
        final SoftwareModule module = new SoftwareModule(appType, "agent-hub2", "1.0.5", null, "");

        // update data
        set.addModule(module);
    }

    @Test

    public void updateDistributionSet() {
        // prepare data
        Target target = new Target("4711");
        target = targetManagement.createTarget(target);

        SoftwareModule os2 = new SoftwareModule(osType, "poky2", "3.0.3", null, "");
        final SoftwareModule app2 = new SoftwareModule(appType, "app2", "3.0.3", null, "");

        DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement, distributionSetManagement);

        os2 = softwareManagement.createSoftwareModule(os2);

        // update data
        // legal update of module addition
        ds.addModule(os2);
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.findFirstModuleByType(osType)).isEqualTo(os2);

        // legal update of module removal
        ds.removeModule(ds.findFirstModuleByType(appType));
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.findFirstModuleByType(appType)).isNull();

        // Update description
        ds.setDescription("a new description");
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.getDescription()).isEqualTo("a new description");

        // Update name
        ds.setName("a new name");
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.getName()).isEqualTo("a new name");
    }

    @Test
    @WithUser(allSpPermissions = true)

    public void updateDistributionSetMetadata() throws InterruptedException {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a DS
        final DistributionSet ds = TestDataUtil.generateDistributionSet("testDs", softwareManagement,
                distributionSetManagement);
        // initial opt lock revision must be zero
        assertThat(ds.getOptLockRevision()).isEqualTo(1L);

        // create an DS meta data entry
        final DistributionSetMetadata dsMetadata = distributionSetManagement
                .createDistributionSetMetadata(new DistributionSetMetadata(knownKey, ds, knownValue));

        DistributionSet changedLockRevisionDS = distributionSetManagement.findDistributionSetById(ds.getId());
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(2L);

        // modifying the meta data value
        dsMetadata.setValue(knownUpdateValue);
        dsMetadata.setKey(knownKey);
        dsMetadata.setDistributionSet(changedLockRevisionDS);

        Thread.sleep(100);

        // update the DS metadata
        final DistributionSetMetadata updated = distributionSetManagement.updateDistributionSetMetadata(dsMetadata);
        // we are updating the sw meta data so also modifying the base software
        // module so opt lock
        // revision must be three
        changedLockRevisionDS = distributionSetManagement.findDistributionSetById(ds.getId());
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(3L);
        assertThat(changedLockRevisionDS.getLastModifiedAt()).isGreaterThan(0L);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getDistributionSet().getId()).isEqualTo(ds.getId());
    }

    @Test

    public void findDistributionSetsAllOrderedByLinkTarget() {

        final List<DistributionSet> buildDistributionSets = TestDataUtil.generateDistributionSets("dsOrder", 10,
                softwareManagement, distributionSetManagement);

        final List<Target> buildTargetFixtures = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(5, "tOrder", "someDesc"));

        final Iterator<DistributionSet> dsIterator = buildDistributionSets.iterator();
        final Iterator<Target> tIterator = buildTargetFixtures.iterator();
        final DistributionSet dsFirst = dsIterator.next();
        final DistributionSet dsSecond = dsIterator.next();
        final DistributionSet dsThree = dsIterator.next();
        final DistributionSet dsFour = dsIterator.next();
        final Target tFirst = tIterator.next();
        final Target tSecond = tIterator.next();

        // set assigned
        deploymentManagement.assignDistributionSet(dsSecond.getId(), tSecond.getControllerId());
        deploymentManagement.assignDistributionSet(dsThree.getId(), tFirst.getControllerId());
        // set installed
        final ArrayList<Target> installedDSSecond = new ArrayList<>();
        installedDSSecond.add(tSecond);
        sendUpdateActionStatusToTargets(dsSecond, installedDSSecond, Status.FINISHED, "some message");

        deploymentManagement.assignDistributionSet(dsFour.getId(), tSecond.getControllerId());

        final DistributionSetFilterBuilder distributionSetFilterBuilder = new DistributionSetFilterBuilder()
                .setIsDeleted(false).setIsComplete(true).setSelectDSWithNoTag(Boolean.FALSE);

        // target first only has an assigned DS-three so check order correct
        final List<DistributionSet> tFirstPin = distributionSetManagement.findDistributionSetsAllOrderedByLinkTarget(
                pageReq, distributionSetFilterBuilder, tFirst.getControllerId()).getContent();
        assertThat(tFirstPin.get(0)).isEqualTo(dsThree);
        assertThat(tFirstPin).hasSize(10);

        // target second has installed DS-2 and assigned DS-4 so check order
        // correct
        final List<DistributionSet> tSecondPin = distributionSetManagement.findDistributionSetsAllOrderedByLinkTarget(
                pageReq, distributionSetFilterBuilder, tSecond.getControllerId()).getContent();
        assertThat(tSecondPin.get(0)).isEqualTo(dsSecond);
        assertThat(tSecondPin.get(1)).isEqualTo(dsFour);
        assertThat(tFirstPin).hasSize(10);
    }

    @Test

    public void searchDistributionSetsOnFilters() {
        DistributionSetTag dsTagA = tagManagement
                .createDistributionSetTag(new DistributionSetTag("DistributionSetTag-A"));
        final DistributionSetTag dsTagB = tagManagement
                .createDistributionSetTag(new DistributionSetTag("DistributionSetTag-B"));
        final DistributionSetTag dsTagC = tagManagement
                .createDistributionSetTag(new DistributionSetTag("DistributionSetTag-C"));
        final DistributionSetTag dsTagD = tagManagement
                .createDistributionSetTag(new DistributionSetTag("DistributionSetTag-D"));

        List<DistributionSet> ds100Group1 = TestDataUtil.generateDistributionSets("", 100, softwareManagement,
                distributionSetManagement);
        List<DistributionSet> ds100Group2 = TestDataUtil.generateDistributionSets("test2", 100, softwareManagement,
                distributionSetManagement);
        DistributionSet dsDeleted = TestDataUtil.generateDistributionSet("deleted", softwareManagement,
                distributionSetManagement);
        final DistributionSet dsInComplete = distributionSetManagement
                .createDistributionSet(new DistributionSet("notcomplete", "1", "", standardDsType, null));

        final DistributionSetType newType = distributionSetManagement
                .createDistributionSetType(new DistributionSetType("foo", "bar", "test").addMandatoryModuleType(osType)
                        .addOptionalModuleType(appType).addOptionalModuleType(runtimeType));

        final DistributionSet dsNewType = distributionSetManagement
                .createDistributionSet(new DistributionSet("newtype", "1", "", newType, dsDeleted.getModules()));

        deploymentManagement.assignDistributionSet(dsDeleted,
                targetManagement.createTargets(Lists.newArrayList(TestDataUtil.generateTargets(5))));
        distributionSetManagement.deleteDistributionSet(dsDeleted);
        dsDeleted = distributionSetManagement.findDistributionSetById(dsDeleted.getId());

        ds100Group1 = distributionSetManagement.toggleTagAssignment(ds100Group1, dsTagA).getAssignedDs();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());
        ds100Group1 = distributionSetManagement.toggleTagAssignment(ds100Group1, dsTagB).getAssignedDs();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());
        ds100Group2 = distributionSetManagement.toggleTagAssignment(ds100Group2, dsTagA).getAssignedDs();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());

        // check setup
        assertThat(distributionSetRepository.findAll()).hasSize(203);

        // Find all
        List<DistributionSet> expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsDeleted);
        expected.add(dsInComplete);
        expected.add(dsNewType);

        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, getDistributionSetFilterBuilder().build()).getContent())
                        .hasSize(203).containsOnly(expected.toArray(new DistributionSet[0]));

        DistributionSetFilterBuilder distributionSetFilterBuilder;

        // search for not deleted
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(202);

        // search for completed
        expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsDeleted);
        expected.add(dsNewType);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(202)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE);
        expected = new ArrayList<DistributionSet>();
        expected.add(dsInComplete);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        // search for type
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(newType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(202);

        // search for text
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        // search for tags
        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagA.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(200);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagA.getName(), dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(200);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagC.getName(), dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagC.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete
        expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsNewType);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(201)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<DistributionSet>();
        expected.add(dsInComplete);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<DistributionSet>();
        expected.add(dsDeleted);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE)
                .setIsComplete(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete and type
        expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.FALSE)
                .setIsComplete(Boolean.TRUE).setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(200)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<DistributionSet>();
        expected.add(dsDeleted);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setType(standardDsType).setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE)
                .setIsComplete(Boolean.FALSE).setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        expected = new ArrayList<DistributionSet>();
        expected.add(dsNewType);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setType(newType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        // combine deleted and complete and type and text
        expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setType(standardDsType).setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(100)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.TRUE).setType(standardDsType).setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType).setSearchText("%test2")
                .setIsComplete(false).setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(newType).setSearchText("%test2")
                .setIsComplete(Boolean.TRUE).setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete and type and text and tag
        expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true).setType(standardDsType)
                .setSearchText("%test2").setTagNames(Lists.newArrayList(dsTagA.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(100)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType).setSearchText("%test2")
                .setTagNames(Lists.newArrayList(dsTagA.getName())).setIsComplete(Boolean.FALSE)
                .setIsDeleted(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

    }

    private DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return new DistributionSetFilterBuilder();
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

    @Test

    public void findDistributionSetsWithoutLazy() {
        TestDataUtil.generateDistributionSets(20, softwareManagement, distributionSetManagement);

        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(20);
    }

    @Test

    public void deleteUnassignedDistributionSet() {
        DistributionSet ds1 = TestDataUtil.generateDistributionSet("ds-1", softwareManagement,
                distributionSetManagement);
        DistributionSet ds2 = TestDataUtil.generateDistributionSet("ds-2", softwareManagement,
                distributionSetManagement);

        ds1 = distributionSetManagement.findDistributionSetByNameAndVersion(ds1.getName(), ds1.getVersion());
        ds2 = distributionSetManagement.findDistributionSetByNameAndVersion(ds2.getName(), ds2.getVersion());

        // delete a ds
        assertThat(distributionSetRepository.findAll()).hasSize(2);
        distributionSetManagement.deleteDistributionSet(ds1.getId());
        // not assigned so not marked as deleted but fully deleted
        assertThat(distributionSetRepository.findAll()).hasSize(1);
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, Boolean.FALSE, Boolean.TRUE)
                .getTotalElements()).isEqualTo(1);
    }

    @Test

    public void findAllDistributionSetMetadataByDsId() {
        // create a DS
        DistributionSet ds1 = TestDataUtil.generateDistributionSet("testDs1", softwareManagement,
                distributionSetManagement);
        DistributionSet ds2 = TestDataUtil.generateDistributionSet("testDs2", softwareManagement,
                distributionSetManagement);

        for (int index = 0; index < 10; index++) {

            ds1 = distributionSetManagement
                    .createDistributionSetMetadata(new DistributionSetMetadata("key" + index, ds1, "value" + index))
                    .getDistributionSet();
        }

        for (int index = 0; index < 20; index++) {

            ds2 = distributionSetManagement
                    .createDistributionSetMetadata(new DistributionSetMetadata("key" + index, ds2, "value" + index))
                    .getDistributionSet();
        }

        final Page<DistributionSetMetadata> metadataOfDs1 = distributionSetManagement
                .findDistributionSetMetadataByDistributionSetId(ds1.getId(), new PageRequest(0, 100));

        final Page<DistributionSetMetadata> metadataOfDs2 = distributionSetManagement
                .findDistributionSetMetadataByDistributionSetId(ds2.getId(), new PageRequest(0, 100));

        assertThat(metadataOfDs1.getNumberOfElements()).isEqualTo(10);
        assertThat(metadataOfDs1.getTotalElements()).isEqualTo(10);

        assertThat(metadataOfDs2.getNumberOfElements()).isEqualTo(20);
        assertThat(metadataOfDs2.getTotalElements()).isEqualTo(20);
    }

    @Test
    public void deleteAssignedDistributionSet() {
        DistributionSet ds1 = TestDataUtil.generateDistributionSet("ds-1", softwareManagement,
                distributionSetManagement);
        DistributionSet ds2 = TestDataUtil.generateDistributionSet("ds-2", softwareManagement,
                distributionSetManagement);
        DistributionSet dsAssigned = TestDataUtil.generateDistributionSet("ds-3", softwareManagement,
                distributionSetManagement);

        ds1 = distributionSetManagement.findDistributionSetByNameAndVersion(ds1.getName(), ds1.getVersion());
        ds2 = distributionSetManagement.findDistributionSetByNameAndVersion(ds2.getName(), ds2.getVersion());

        // create assigned DS
        dsAssigned = distributionSetManagement.findDistributionSetByNameAndVersion(dsAssigned.getName(),
                dsAssigned.getVersion());
        final Target target = new Target("4712");
        final Target savedTarget = targetManagement.createTarget(target);
        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);
        deploymentManagement.assignDistributionSet(dsAssigned, toAssign);

        // delete a ds
        assertThat(distributionSetRepository.findAll()).hasSize(3);
        distributionSetManagement.deleteDistributionSet(dsAssigned.getId());

        // not assigned so not marked as deleted
        assertThat(distributionSetRepository.findAll()).hasSize(3);
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, Boolean.FALSE, Boolean.TRUE)
                .getTotalElements()).isEqualTo(2);
    }

    /**
     * helper method which re-orders a list as expected. Re-orders the given
     * distribution set in the order as given and returns a new list with the
     * new order.
     * 
     * @param dsThree
     * @param buildDistributionSets
     * @return
     */
    private List<DistributionSet> reOrderDSList(final Iterable<DistributionSet> buildDistributionSets,
            final DistributionSet... ds) {
        final List<DistributionSet> reOrderedList = new ArrayList<>();

        final Iterator<DistributionSet> iterator = buildDistributionSets.iterator();
        while (iterator.hasNext()) {
            final DistributionSet next = iterator.next();
            int reorder = -1;
            for (int index = 0; index < ds.length; index++) {
                if (next.equals(ds[index])) {
                    reorder = index;
                }
            }
            if (reorder >= 0) {
                reOrderedList.add(reorder, next);
            } else {
                reOrderedList.add(next);
            }
        }

        return reOrderedList;
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

}
