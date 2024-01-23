/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.alarm.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmRuleService extends AbstractEntityService implements AlarmRuleService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private AlarmRuleDao alarmRuleDao;

    @Autowired
    private DataValidator<AlarmRule> alarmRuleDataValidator;

    @Override
    public AlarmRule saveAlarmRule(TenantId tenantId, AlarmRule alarmRule) {
        alarmRuleDataValidator.validate(alarmRule, AlarmRule::getTenantId);
        AlarmRule saved = alarmRuleDao.save(tenantId, alarmRule);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(saved.getId()).created(alarmRule.getId() == null).build());
        return saved;
    }

    @Override
    public void deleteAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId) {
        log.debug("Deleting AlarmRule Id: {}", alarmRuleId);
        alarmRuleDao.removeById(tenantId, alarmRuleId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId)
                .entityId(alarmRuleId).build());
    }

    @Override
    public AlarmRule findAlarmRuleById(TenantId tenantId, AlarmRuleId alarmRuleId) {
        log.trace("Executing findAlarmById [{}]", alarmRuleId);
        validateId(alarmRuleId, "Incorrect alarmRuleId " + alarmRuleId);
        return alarmRuleDao.findById(tenantId, alarmRuleId.getId());
    }

    @Override
    public PageData<AlarmRuleInfo> findAlarmRuleInfos(TenantId tenantId, PageLink pageLink) {
        return alarmRuleDao.findAlarmRuleInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AlarmRule> findAlarmRules(TenantId tenantId, PageLink pageLink) {
        return alarmRuleDao.findAlarmRulesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AlarmRule> findEnabledAlarmRules(TenantId tenantId, PageLink pageLink) {
        return alarmRuleDao.findEnabledAlarmRulesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteAlarmRulesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAlarmRulesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAlarmRulesRemover.removeEntities(tenantId, tenantId);
    }

    private PaginatedRemover<TenantId, AlarmRuleInfo> tenantAlarmRulesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<AlarmRuleInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return alarmRuleDao.findAlarmRuleInfosByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, AlarmRuleInfo entity) {
                    deleteAlarmRule(tenantId, new AlarmRuleId(entity.getId().getId()));
                }
            };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAlarmRuleById(tenantId, new AlarmRuleId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM_RULE;
    }
}
