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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Query;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Iterables;



public class TargetManagementTest extends AbstractIntegrationTest {

    @Test
    
    @WithUser(tenantId = "tenantWhichDoesNotExists", allSpPermissions = true, autoCreateTenant = false)
    public void createTargetForTenantWhichDoesNotExistThrowsTenantNotExistException() {
        try {
            targetManagement.createTarget(new Target("targetId123"));
            fail("tenant not exist");
        } catch (final TenantNotExistException e) {
        }
    }

    @Test
    
    public void assignAndUnassignTargetsToTag() {
        final List<String> assignTarget = new ArrayList<String>();
        assignTarget.add(targetManagement.createTarget(new Target("targetId123")).getControllerId());
        assignTarget.add(targetManagement.createTarget(new Target("targetId1234")).getControllerId());
        assignTarget.add(targetManagement.createTarget(new Target("targetId1235")).getControllerId());
        assignTarget.add(targetManagement.createTarget(new Target("targetId1236")).getControllerId());
        assignTarget.add("NotExist");

        final TargetTag targetTag = tagManagement.createTargetTag(new TargetTag("Tag1"));

        final List<Target> assignedTargets = targetManagement.assignTag(assignTarget, targetTag);
        assertThat(assignedTargets.size()).isEqualTo(4);
        assignedTargets.forEach(target -> assertThat(target.getTags().size()).isEqualTo(1));

        TargetTag findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(assignedTargets.size()).isEqualTo(findTargetTag.getAssignedToTargets().size());

        assertThat(targetManagement.unAssignTag("NotExist", findTargetTag)).isNull();

        final Target unAssignTarget = targetManagement.unAssignTag("targetId123", findTargetTag);
        assertThat(unAssignTarget.getControllerId()).isEqualTo("targetId123");
        assertThat(unAssignTarget.getTags().size()).isEqualTo(0);
        findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(findTargetTag.getAssignedToTargets().size()).isEqualTo(3);

        final List<Target> unAssignTargets = targetManagement.unAssignAllTargetsByTag(findTargetTag);
        findTargetTag = tagManagement.findTargetTag("Tag1");
        assertThat(findTargetTag.getAssignedToTargets().size()).isEqualTo(0);
        assertThat(unAssignTargets.size()).isEqualTo(3);
        unAssignTargets.forEach(target -> assertThat(target.getTags().size()).isEqualTo(0));
    }

    @Test
    
    public void deleteAndCreateTargets() {
        Target target = targetManagement.createTarget(new Target("targetId123"));
        assertThat(targetManagement.countTargetsAll()).isEqualTo(1);
        targetManagement.deleteTargets(target.getId());
        assertThat(targetManagement.countTargetsAll()).isEqualTo(0);

        target = createTargetWithAttributes("4711");
        assertThat(targetManagement.countTargetsAll()).isEqualTo(1);
        targetManagement.deleteTargets(target.getId());
        assertThat(targetManagement.countTargetsAll()).isEqualTo(0);

        final List<Long> targets = new ArrayList<Long>();
        for (int i = 0; i < 5; i++) {
            target = targetManagement.createTarget(new Target("" + i));
            targets.add(target.getId());
            targets.add(createTargetWithAttributes("" + (i * i + 1000)).getId());
        }
        assertThat(targetManagement.countTargetsAll()).isEqualTo(10);
        targetManagement.deleteTargets(targets.toArray(new Long[targets.size()]));
        assertThat(targetManagement.countTargetsAll()).isEqualTo(0);
    }

    private Target createTargetWithAttributes(final String controllerId) {
        Target target = new Target(controllerId);
        final Map<String, String> testData = new HashMap<>();
        testData.put("test1", "testdata1");

        target = targetManagement.createTarget(target);
        target = controllerManagament.updateControllerAttributes(controllerId, testData);

        target = targetManagement.findTargetByControllerIDWithDetails(controllerId);
        assertThat(target.getTargetInfo().getControllerAttributes()).isEqualTo(testData);
        return target;
    }

    @Test
    
    public void findTargetByControllerIDWithDetails() {
        final DistributionSet set = TestDataUtil.generateDistributionSet("test", softwareManagement,
                distributionSetManagement);
        final DistributionSet set2 = TestDataUtil.generateDistributionSet("test2", softwareManagement,
                distributionSetManagement);

        assertThat(targetManagement.countTargetByAssignedDistributionSet(set.getId())).isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set.getId())).isEqualTo(0);
        assertThat(targetManagement.countTargetByAssignedDistributionSet(set2.getId())).isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set2.getId())).isEqualTo(0);

        Target target = createTargetWithAttributes("4711");

        final long current = System.currentTimeMillis();
        controllerManagament.updateLastTargetQuery("4711", null);

        final DistributionSetAssignmentResult result = deploymentManagement.assignDistributionSet(set.getId(), "4711");

        final Action action = result.getActions().get(0);
        action.setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(new ActionStatus(action, Status.FINISHED,
                System.currentTimeMillis(), "message"), action);
        deploymentManagement.assignDistributionSet(set2.getId(), "4711");

        target = targetManagement.findTargetByControllerIDWithDetails("4711");
        // read data

        assertThat(targetManagement.countTargetByAssignedDistributionSet(set.getId())).isEqualTo(0);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set.getId())).isEqualTo(1);
        assertThat(targetManagement.countTargetByAssignedDistributionSet(set2.getId())).isEqualTo(1);
        assertThat(targetManagement.countTargetByInstalledDistributionSet(set2.getId())).isEqualTo(0);
        assertThat(target.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(target.getAssignedDistributionSet()).isEqualTo(set2);
        assertThat(target.getTargetInfo().getInstalledDistributionSet().getId()).isEqualTo(set.getId());

    }

    @Test
    
    public void createMultipleTargetsDuplicate() {
        final List<Target> targets = TestDataUtil.buildTargetFixtures(5, "mySimpleTargs", "my simple targets");
        targetManagement.createTargets(targets);
        try {
            targetManagement.createTargets(targets);
            fail("Targets already exists");
        } catch (final EntityAlreadyExistsException e) {
        }

    }

    @Test
    
    public void createTargetDuplicate() {
        targetManagement.createTarget(new Target("4711"));
        try {
            targetManagement.createTarget(new Target("4711"));
            fail("Target already exists");
        } catch (final EntityAlreadyExistsException e) {
        }
    }

    /**
     * verifies, that all {@link TargetTag} of parameter. NOTE: it's accepted
     * that the target have additional tags assigned to them which are not
     * contained within parameter tags.
     * 
     * @param strict
     *            if true, the given targets MUST contain EXACTLY ALL given
     *            tags, AND NO OTHERS. If false, the given targets MUST contain
     *            ALL given tags, BUT MAY CONTAIN FURTHER ONE
     * @param targets
     *            targets to be verified
     * @param tags
     *            are contained within tags of all targets.
     * @param tags
     *            to be found in the tags of the targets
     */
    private void checkTargetHasTags(final boolean strict, final Iterable<Target> targets, final TargetTag... tags) {
        _target: for (final Target tl : targets) {
            final Target t = targetManagement.findTargetByControllerID(tl.getControllerId());

            for (final Tag tt : t.getTags()) {
                for (final Tag tag : tags) {
                    if (tag.getName().equals(tt.getName())) {
                        continue _target;
                    }
                }
                if (strict) {
                    fail();
                }
            }
            fail();
        }
    }

    private void checkTargetHasNotTags(final Iterable<Target> targets, final TargetTag... tags) {
        for (final Target tl : targets) {
            final Target t = targetManagement.findTargetByControllerID(tl.getControllerId());

            for (final Tag tag : tags) {
                for (final Tag tt : t.getTags()) {
                    if (tag.getName().equals(tt.getName())) {
                        fail();
                    }
                }
            }
        }
    }

    @Test
    @WithUser(allSpPermissions = true)
    
    public void singleTargetIsInsertedIntoRepo() throws Exception {

        final String myCtrlID = "myCtrlID";
        final Target target = TestDataUtil.buildTargetFixture(myCtrlID, "the description!");

        Target savedTarget = targetManagement.createTarget(target);
        assertNotNull(savedTarget);
        final Long createdAt = savedTarget.getCreatedAt();
        Long modifiedAt = savedTarget.getLastModifiedAt();
        assertEquals(createdAt, modifiedAt);
        assertNotNull(savedTarget.getCreatedAt());
        assertNotNull(savedTarget.getLastModifiedAt());
        assertEquals(target, savedTarget);

        savedTarget.setDescription("changed description");
        Thread.sleep(1);
        savedTarget = targetManagement.updateTarget(savedTarget);

        assertNotNull(savedTarget.getLastModifiedAt());
        assertNotEquals(createdAt, savedTarget.getLastModifiedAt());
        assertNotEquals(modifiedAt, savedTarget.getLastModifiedAt());
        modifiedAt = savedTarget.getLastModifiedAt();

        final Target foundTarget = targetManagement.findTargetByControllerID(savedTarget.getControllerId());

        assertNotNull(foundTarget);
        assertEquals(myCtrlID, foundTarget.getControllerId());
        assertEquals(savedTarget, foundTarget);
        assertEquals(createdAt, foundTarget.getCreatedAt());
        assertEquals(modifiedAt, foundTarget.getLastModifiedAt());
    }

    @Test
    @WithUser(allSpPermissions = true)
    
    public void bulkTargetCreationAndDelete() throws Exception {
        final String myCtrlID = "myCtrlID";
        final List<Target> firstList = TestDataUtil.buildTargetFixtures(100, myCtrlID, "first description");

        final Target extra = TestDataUtil.buildTargetFixture("myCtrlID-00081XX", "first description");

        List<Target> firstSaved = targetManagement.createTargets(firstList);

        final Target savedExtra = targetManagement.createTarget(extra);

        Iterable<Target> allFound = targetRepository.findAll();
        assertEquals(firstList.size(), firstSaved.spliterator().getExactSizeIfKnown());
        assertEquals(firstList.size() + 1, allFound.spliterator().getExactSizeIfKnown());

        // change the objects and save to again to trigger a change on
        // lastModifiedAt
        firstSaved.forEach(t -> t.setName(t.getName().concat("\tchanged")));
        firstSaved = targetManagement.updateTargets(firstSaved);

        // verify that all entries are found
        _founds: for (final Target foundTarget : allFound) {
            for (final Target changedTarget : firstSaved) {
                if (changedTarget.getControllerId().equals(foundTarget.getControllerId())) {
                    assertEquals(changedTarget.getDescription(), foundTarget.getDescription());
                    assertTrue(changedTarget.getName().startsWith(foundTarget.getName()));
                    assertTrue(changedTarget.getName().endsWith("changed"));
                    assertEquals(changedTarget.getCreatedAt(), foundTarget.getCreatedAt());
                    assertThat(changedTarget.getLastModifiedAt()).isNotEqualTo(changedTarget.getCreatedAt());

                    continue _founds;
                }
            }

            if (!foundTarget.getControllerId().equals(savedExtra.getControllerId())) {
                fail();
            }
        }

        targetManagement.deleteTargets(savedExtra.getId());

        final int nr2Del = 50;
        int i = nr2Del;
        final Long[] deletedTargetIDs = new Long[nr2Del];
        final Target[] deletedTargets = new Target[nr2Del];

        final Iterator<Target> it = firstSaved.iterator();
        while (nr2Del > 0 && it.hasNext() && i > 0) {
            final Target pt = it.next();
            deletedTargetIDs[i - 1] = pt.getId();
            deletedTargets[i - 1] = pt;
            i--;
        }

        targetManagement.deleteTargets(deletedTargetIDs);

        allFound = targetManagement.findTargetsAll(new PageRequest(0, 200)).getContent();
        assertEquals(firstSaved.spliterator().getExactSizeIfKnown() - nr2Del, allFound.spliterator()
                .getExactSizeIfKnown());

        // verify that all undeleted are still found
        assertThat(allFound).doesNotContain(deletedTargets);
    }

    @Test
    
    public void savingTargetControllerAttributes() {
        Iterable<Target> ts = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(100, "myCtrlID",
                "first description"));

        final Map<String, String> attribs = new HashMap<String, String>();
        attribs.put("a.b.c", "abc");
        attribs.put("x.y.z", "");
        attribs.put("1.2.3", "123");
        attribs.put("1.2.3.4", "1234");
        attribs.put("1.2.3.4.5", "12345");
        final Set<String> attribs2Del = new HashSet<String>();
        attribs2Del.add("x.y.z");
        attribs2Del.add("1.2.3");

        for (final Target t : ts) {
            TargetInfo targetInfo = t.getTargetInfo();
            targetInfo.setNew(false);
            for (final Entry<String, String> attrib : attribs.entrySet()) {
                final String key = attrib.getKey();
                final String value = String.format("%s-%s", attrib.getValue(), t.getControllerId());
                targetInfo.getControllerAttributes().put(key, value);
            }
            targetInfo = targetInfoRepository.save(targetInfo);
        }
        final Query qry = entityManager.createNativeQuery("select * from sp_target_attributes ta");
        final List result = qry.getResultList();
        assertEquals(attribs.size() * ts.spliterator().getExactSizeIfKnown(), result.size());

        for (final Target myT : ts) {
            final Target t = targetManagement.findTargetByControllerIDWithDetails(myT.getControllerId());
            assertEquals(attribs.size(), t.getTargetInfo().getControllerAttributes().size());
            for (final Entry<String, String> ca : t.getTargetInfo().getControllerAttributes().entrySet()) {
                assertTrue(attribs.containsKey(ca.getKey()));
                // has the same value: see string concatenation above
                assertEquals(String.format("%s-%s", attribs.get(ca.getKey()), t.getControllerId()), ca.getValue());
            }
        }

        ts = targetManagement.findTargetsAll(new PageRequest(0, 100)).getContent();
        final Iterator<Target> tsIt = ts.iterator();
        // all attributs of the target are deleted
        final Target[] ts2DelAllAttribs = new Target[] { tsIt.next(), tsIt.next(), tsIt.next() };
        // a few attributs are deleted
        final Target[] ts2DelAttribs = new Target[] { tsIt.next(), tsIt.next() };

        // perform the deletion operations accordingly
        for (final Target ta : ts2DelAllAttribs) {
            final Target t = targetManagement.findTargetByControllerIDWithDetails(ta.getControllerId());

            final TargetInfo targetStatus = t.getTargetInfo();
            targetStatus.getControllerAttributes().clear();
            targetInfoRepository.save(targetStatus);
        }

        for (final Target ta : ts2DelAttribs) {
            final Target t = targetManagement.findTargetByControllerIDWithDetails(ta.getControllerId());

            final TargetInfo targetStatus = t.getTargetInfo();
            for (final String attribKey : attribs2Del) {
                targetStatus.getControllerAttributes().remove(attribKey);
            }
            targetInfoRepository.save(targetStatus);
        }

        // only the number of the remaining targets and controller attributes
        // are checked
        final Iterable<Target> restTS = targetRepository.findAll();

        restTarget_: for (final Target targetl : restTS) {
            final Target target = targetManagement.findTargetByControllerIDWithDetails(targetl.getControllerId());

            // verify that all members of the list ts2DelAllAttribs don't have
            // any attributes
            for (final Target tNoAttribl : ts2DelAllAttribs) {
                final Target tNoAttrib = targetManagement.findTargetByControllerID(tNoAttribl.getControllerId());

                if (tNoAttrib.getControllerId().equals(target.getControllerId())) {
                    assertThat(target.getTargetInfo().getControllerAttributes()).isEmpty();
                    continue restTarget_;
                }
            }
            // verify that that the attribute list of all members of the list
            // ts2DelAttribs don't have
            // attributes which have been deleted
            for (final Target tNoAttribl : ts2DelAttribs) {
                final Target tNoAttrib = targetManagement.findTargetByControllerID(tNoAttribl.getControllerId());

                if (tNoAttrib.getControllerId().equals(target.getControllerId())) {
                    assertThat(target.getTargetInfo().getControllerAttributes().keySet().toArray()).doesNotContain(
                            attribs2Del.toArray());
                    continue restTarget_;
                }
            }
        }
    }

    @Test
    
    public void targetTagAssignment() {
        Target t1 = TestDataUtil.buildTargetFixture("id-1", "blablub");
        final int noT2Tags = 4;
        final int noT1Tags = 3;
        final List<TargetTag> t1Tags = tagManagement.createTargetTags(TestDataUtil.buildTargetTagFixtures(noT1Tags,
                "tag1"));
        t1.getTags().addAll(t1Tags);
        t1 = targetManagement.createTarget(t1);

        Target t2 = TestDataUtil.buildTargetFixture("id-2", "blablub");
        final List<TargetTag> t2Tags = tagManagement.createTargetTags(TestDataUtil.buildTargetTagFixtures(noT2Tags,
                "tag2"));
        t2.getTags().addAll(t2Tags);
        t2 = targetManagement.createTarget(t2);

        t1 = targetManagement.findTargetByControllerID(t1.getControllerId());
        assertThat(t1.getTags()).hasSize(noT1Tags).containsAll(t1Tags);
        assertThat(t1.getTags()).hasSize(noT1Tags).doesNotContain(Iterables.toArray(t2Tags, TargetTag.class));

        t2 = targetManagement.findTargetByControllerID(t2.getControllerId());
        assertThat(t2.getTags()).hasSize(noT2Tags).containsAll(t2Tags);
        assertThat(t2.getTags()).hasSize(noT2Tags).doesNotContain(Iterables.toArray(t1Tags, TargetTag.class));
    }

    @Test
    
    public void targetTagBulkAssignments() {
        final List<Target> tagATargets = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(10,
                "tagATargets", "first description"));
        final List<Target> tagBTargets = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(10,
                "tagBTargets", "first description"));
        final List<Target> tagCTargets = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(10,
                "tagCTargets", "first description"));

        final List<Target> tagABTargets = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(10,
                "tagABTargets", "first description"));

        final List<Target> tagABCTargets = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(10,
                "tagABCTargets", "first description"));

        final TargetTag tagA = tagManagement.createTargetTag(new TargetTag("A"));
        final TargetTag tagB = tagManagement.createTargetTag(new TargetTag("B"));
        final TargetTag tagC = tagManagement.createTargetTag(new TargetTag("C"));
        final TargetTag tagX = tagManagement.createTargetTag(new TargetTag("X"));

        // doing different assignments
        targetManagement.toggleTagAssignment(tagATargets, tagA);
        targetManagement.toggleTagAssignment(tagBTargets, tagB);
        targetManagement.toggleTagAssignment(tagCTargets, tagC);

        targetManagement.toggleTagAssignment(tagABTargets, tagA);
        targetManagement.toggleTagAssignment(tagABTargets, tagB);

        targetManagement.toggleTagAssignment(tagABCTargets, tagA);
        targetManagement.toggleTagAssignment(tagABCTargets, tagB);
        targetManagement.toggleTagAssignment(tagABCTargets, tagC);

        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "X")).isEqualTo(0);

        // search for targets with tag tagA
        final List<Target> targetWithTagA = new ArrayList<Target>();
        final List<Target> targetWithTagB = new ArrayList<Target>();
        final List<Target> targetWithTagC = new ArrayList<Target>();

        // storing target lists to enable easy evaluation
        Iterables.addAll(targetWithTagA, tagATargets);
        Iterables.addAll(targetWithTagA, tagABTargets);
        Iterables.addAll(targetWithTagA, tagABCTargets);

        Iterables.addAll(targetWithTagB, tagBTargets);
        Iterables.addAll(targetWithTagB, tagABTargets);
        Iterables.addAll(targetWithTagB, tagABCTargets);

        Iterables.addAll(targetWithTagC, tagCTargets);
        Iterables.addAll(targetWithTagC, tagABCTargets);

        // check the target lists as returned by assignTag
        checkTargetHasTags(false, targetWithTagA, tagA);
        checkTargetHasTags(false, targetWithTagB, tagB);
        checkTargetHasTags(false, targetWithTagC, tagC);

        checkTargetHasNotTags(tagATargets, tagB, tagC);
        checkTargetHasNotTags(tagBTargets, tagA, tagC);
        checkTargetHasNotTags(tagCTargets, tagA, tagB);

        // check again target lists refreshed from DB
        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "A")).isEqualTo(
                targetWithTagA.size());
        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "B")).isEqualTo(
                targetWithTagB.size());
        assertThat(targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, "C")).isEqualTo(
                targetWithTagC.size());
    }

    @Test
    
    public void targetTagBulkUnassignments() {
        final TargetTag targTagA = tagManagement.createTargetTag(new TargetTag("Targ-A-Tag"));
        final TargetTag targTagB = tagManagement.createTargetTag(new TargetTag("Targ-B-Tag"));
        final TargetTag targTagC = tagManagement.createTargetTag(new TargetTag("Targ-C-Tag"));

        final List<Target> targAs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(25, "target-id-A",
                "first description"));
        final List<Target> targBs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(20, "target-id-B",
                "first description"));
        final List<Target> targCs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(15, "target-id-C",
                "first description"));

        final List<Target> targABs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(12,
                "target-id-AB", "first description"));
        final List<Target> targACs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(13,
                "target-id-AC", "first description"));
        final List<Target> targBCs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(7, "target-id-BC",
                "first description"));
        final List<Target> targABCs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(17,
                "target-id-ABC", "first description"));

        targetManagement.toggleTagAssignment(targAs, targTagA);
        targetManagement.toggleTagAssignment(targABs, targTagA);
        targetManagement.toggleTagAssignment(targACs, targTagA);
        targetManagement.toggleTagAssignment(targABCs, targTagA);

        targetManagement.toggleTagAssignment(targBs, targTagB);
        targetManagement.toggleTagAssignment(targABs, targTagB);
        targetManagement.toggleTagAssignment(targBCs, targTagB);
        targetManagement.toggleTagAssignment(targABCs, targTagB);

        targetManagement.toggleTagAssignment(targCs, targTagC);
        targetManagement.toggleTagAssignment(targACs, targTagC);
        targetManagement.toggleTagAssignment(targBCs, targTagC);
        targetManagement.toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA);
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targACs, targTagA, targTagC);
        checkTargetHasTags(true, targBCs, targTagB, targTagC);
        checkTargetHasTags(true, targABCs, targTagA, targTagB, targTagC);

        targetManagement.toggleTagAssignment(targCs, targTagC);
        targetManagement.toggleTagAssignment(targACs, targTagC);
        targetManagement.toggleTagAssignment(targBCs, targTagC);
        targetManagement.toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA);
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targBCs, targTagB);
        checkTargetHasTags(true, targACs, targTagA);

        checkTargetHasNotTags(targCs, targTagC);
        checkTargetHasNotTags(targACs, targTagC);
        checkTargetHasNotTags(targBCs, targTagC);
        checkTargetHasNotTags(targABCs, targTagC);

    }

    @Test
    
    public void findTargetsByControllerIDsWithTags() {
        final TargetTag targTagA = tagManagement.createTargetTag(new TargetTag("Targ-A-Tag"));

        final List<Target> targAs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(25, "target-id-A",
                "first description"));

        targetManagement.toggleTagAssignment(targAs, targTagA);

        assertThat(
                targetManagement.findTargetsByControllerIDsWithTags(targAs.stream()
                        .map(target -> target.getControllerId()).collect(Collectors.toList()))).hasSize(25);

        // no lazy loading exception and tag correctly assigned
        assertThat(
                targetManagement
                        .findTargetsByControllerIDsWithTags(
                                targAs.stream().map(target -> target.getControllerId()).collect(Collectors.toList()))
                        .stream().map(target -> target.getTags().contains(targTagA)).collect(Collectors.toList()))
                .containsOnly(true);
    }

    @Test
    
    public void findAllTargetIdNamePaiss() {
        final List<Target> targAs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(25, "target-id-A",
                "first description"));
        final String[] createdTargetIds = targAs.stream().map(t -> t.getControllerId())
                .toArray(size -> new String[size]);

        final List<TargetIdName> findAllTargetIdNames = targetManagement.findAllTargetIds();
        final List<String> findAllTargetIds = findAllTargetIdNames.stream().map(TargetIdName::getControllerId)
                .collect(Collectors.toList());

        assertThat(findAllTargetIds).containsOnly(createdTargetIds);
    }

    @Test
    
    public void findTargetsWithNoTag() {

        final TargetTag targTagA = tagManagement.createTargetTag(new TargetTag("Targ-A-Tag"));
        final List<Target> targAs = targetManagement.createTargets(TestDataUtil.buildTargetFixtures(25, "target-id-A",
                "first description"));
        targetManagement.toggleTagAssignment(targAs, targTagA);

        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(25, "target-id-B", "first description"));

        final String[] tagNames = null;
        final List<Target> targetsListWithNoTag = targetManagement.findTargetByFilters(new PageRequest(0, 500), null,
                null, null, Boolean.TRUE, tagNames).getContent();

        // Total targets
        assertEquals(50, targetManagement.findAllTargetIds().size());
        // Targets with no tag
        assertEquals(25, targetsListWithNoTag.size());
    }
}
