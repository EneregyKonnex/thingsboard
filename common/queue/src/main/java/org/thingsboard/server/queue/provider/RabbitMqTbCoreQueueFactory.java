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
package org.thingsboard.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqAdmin;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqConsumerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqProducerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqQueueArguments;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqSettings;
import org.thingsboard.server.queue.settings.TbQueueAlarmRulesSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq' && '${service.type:null}'=='tb-core'")
public class RabbitMqTbCoreQueueFactory implements TbCoreQueueFactory {

    private final TbRabbitMqSettings rabbitMqSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbQueueAlarmRulesSettings arSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    private final TbQueueAdmin jsExecutorAdmin;
    private final TbQueueAdmin transportApiAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin arAdmin;

    public RabbitMqTbCoreQueueFactory(TbRabbitMqSettings rabbitMqSettings,
                                      TbQueueCoreSettings coreSettings,
                                      TbQueueTransportApiSettings transportApiSettings,
                                      TbQueueRuleEngineSettings ruleEngineSettings,
                                      TopicService topicService,
                                      TbServiceInfoProvider serviceInfoProvider,
                                      TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                      TbQueueTransportNotificationSettings transportNotificationSettings,
                                      TbQueueAlarmRulesSettings arSettings, TbRabbitMqQueueArguments queueArguments) {
        this.rabbitMqSettings = rabbitMqSettings;
        this.coreSettings = coreSettings;
        this.transportApiSettings = transportApiSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.topicService = topicService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.jsInvokeSettings = jsInvokeSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.arSettings = arSettings;

        this.coreAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getCoreArgs());
        this.ruleEngineAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getRuleEngineArgs());
        this.jsExecutorAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getJsExecutorArgs());
        this.transportApiAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getTransportApiArgs());
        this.notificationAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getNotificationsArgs());
        this.arAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getArArgs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(notificationAdmin, rabbitMqSettings, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(notificationAdmin, rabbitMqSettings, topicService.buildTopicName(ruleEngineSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(notificationAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        return new TbRabbitMqConsumerTemplate<>(coreAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new TbRabbitMqConsumerTemplate<>(notificationAdmin, rabbitMqSettings,
                topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        return new TbRabbitMqConsumerTemplate<>(transportApiAdmin, rabbitMqSettings, topicService.buildTopicName(transportApiSettings.getRequestsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new TbRabbitMqProducerTemplate<>(transportApiAdmin, rabbitMqSettings, topicService.buildTopicName(transportApiSettings.getResponsesTopic()));
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbQueueProducer<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> producer = new TbRabbitMqProducerTemplate<>(jsExecutorAdmin, rabbitMqSettings, jsInvokeSettings.getRequestTopic());
        TbQueueConsumer<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> consumer = new TbRabbitMqConsumerTemplate<>(jsExecutorAdmin, rabbitMqSettings,
                jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                });

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorAdmin);
        builder.requestTemplate(producer);
        builder.responseTemplate(consumer);
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        //TODO: version-control
        return null;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        return new TbRabbitMqConsumerTemplate<>(coreAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        return new TbRabbitMqConsumerTemplate<>(coreAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTbAlarmRuleStateServiceMsg>> createAlarmRulesMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(arAdmin, rabbitMqSettings, arSettings.getTopic());
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (jsExecutorAdmin != null) {
            jsExecutorAdmin.destroy();
        }
        if (transportApiAdmin != null) {
            transportApiAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
    }
}
