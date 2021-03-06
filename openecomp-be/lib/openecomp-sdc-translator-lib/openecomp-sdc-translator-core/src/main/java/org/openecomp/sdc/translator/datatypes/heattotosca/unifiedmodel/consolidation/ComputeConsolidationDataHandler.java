/*
 * Copyright © 2016-2018 European Support Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openecomp.sdc.translator.datatypes.heattotosca.unifiedmodel.consolidation;

import java.util.Objects;
import org.onap.sdc.tosca.datatypes.model.NodeTemplate;
import org.onap.sdc.tosca.datatypes.model.RequirementAssignment;
import org.onap.sdc.tosca.datatypes.model.ServiceTemplate;
import org.openecomp.sdc.heat.datatypes.model.HeatOrchestrationTemplate;
import org.openecomp.sdc.heat.datatypes.model.Resource;
import org.openecomp.sdc.tosca.services.DataModelUtil;
import org.openecomp.sdc.tosca.services.ToscaUtil;
import org.openecomp.sdc.translator.datatypes.heattotosca.TranslationContext;
import org.openecomp.sdc.translator.datatypes.heattotosca.to.TranslateTo;
import org.openecomp.sdc.translator.services.heattotosca.NameExtractor;

public class ComputeConsolidationDataHandler implements ConsolidationDataHandler {

    private final ComputeConsolidationData computeConsolidationData;

    ComputeConsolidationDataHandler(ComputeConsolidationData computeConsolidationData) {
        this.computeConsolidationData = computeConsolidationData;
    }

    @Override
    public void addNodesConnectedOut(TranslateTo translateTo, String nodeTemplateId, String requirementId,
                                     RequirementAssignment requirementAssignment) {

        String translatedSourceNodeId = translateTo.getTranslatedId();
        ServiceTemplate serviceTemplate = translateTo.getServiceTemplate();
        NodeTemplate computeNodeTemplate = DataModelUtil.getNodeTemplate(serviceTemplate, translatedSourceNodeId);
        String nodeType = computeNodeTemplate.getType();

        EntityConsolidationData entityConsolidationData =
                getComputeTemplateConsolidationData(translateTo, nodeType, translatedSourceNodeId);

        if (Objects.nonNull(entityConsolidationData)) {
            entityConsolidationData.addNodesConnectedOut(nodeTemplateId, requirementId, requirementAssignment);
        }
    }

    @Override
    public void addNodesConnectedIn(TranslateTo translateTo, String sourceNodeTemplateId,
                                    String dependentNodeTemplateId, String targetResourceId, String requirementId,
                                    RequirementAssignment requirementAssignment) {

        ServiceTemplate serviceTemplate = translateTo.getServiceTemplate();
        NodeTemplate nodeTemplate = DataModelUtil.getNodeTemplate(serviceTemplate, dependentNodeTemplateId);
        String nodeType = getNodeType(nodeTemplate, translateTo, targetResourceId, dependentNodeTemplateId);
        EntityConsolidationData entityConsolidationData =
                getComputeTemplateConsolidationData(translateTo, nodeType, dependentNodeTemplateId);

        if (Objects.nonNull(entityConsolidationData)) {
            entityConsolidationData.addNodesConnectedIn(sourceNodeTemplateId, requirementId, requirementAssignment);
        }
    }

    @Override
    public void removeParamNameFromAttrFuncList(ServiceTemplate serviceTemplate,
            HeatOrchestrationTemplate heatOrchestrationTemplate, String paramName, String contrailSharedResourceId,
            String sharedTranslatedResourceId) {

        NodeTemplate nodeTemplate = DataModelUtil.getNodeTemplate(serviceTemplate, sharedTranslatedResourceId);
        EntityConsolidationData entityConsolidationData =
                getComputeTemplateConsolidationData(serviceTemplate, nodeTemplate.getType(),
                        sharedTranslatedResourceId);

        if (Objects.nonNull(entityConsolidationData)) {
            entityConsolidationData.removeParamNameFromAttrFuncList(paramName);
        }
    }

    private ComputeTemplateConsolidationData getComputeTemplateConsolidationData(
            TranslateTo translateTo, String computeNodeType, String computeNodeTemplateId) {

        ServiceTemplate serviceTemplate = translateTo.getServiceTemplate();
        return getComputeTemplateConsolidationData(serviceTemplate, computeNodeType, computeNodeTemplateId);
    }

    private ComputeTemplateConsolidationData getComputeTemplateConsolidationData(ServiceTemplate serviceTemplate,
            String computeNodeType, String computeNodeTemplateId) {

        String serviceTemplateFileName = ToscaUtil.getServiceTemplateFileName(serviceTemplate);

        FileComputeConsolidationData fileComputeConsolidationData =
                computeConsolidationData.getFileComputeConsolidationData(serviceTemplateFileName);
        if (fileComputeConsolidationData == null) {
            fileComputeConsolidationData = new FileComputeConsolidationData();
            computeConsolidationData.setFileComputeConsolidationData(serviceTemplateFileName,
                    fileComputeConsolidationData);
        }

        TypeComputeConsolidationData typeComputeConsolidationData =
                fileComputeConsolidationData.getTypeComputeConsolidationData(computeNodeType);
        if (typeComputeConsolidationData == null) {
            typeComputeConsolidationData = new TypeComputeConsolidationData();
            fileComputeConsolidationData.setTypeComputeConsolidationData(computeNodeType, typeComputeConsolidationData);
        }

        ComputeTemplateConsolidationData computeTemplateConsolidationData =
                typeComputeConsolidationData.getComputeTemplateConsolidationData(computeNodeTemplateId);
        if (computeTemplateConsolidationData == null) {
            computeTemplateConsolidationData = new ComputeTemplateConsolidationData();
            computeTemplateConsolidationData.setNodeTemplateId(computeNodeTemplateId);
            typeComputeConsolidationData.setComputeTemplateConsolidationData(computeNodeTemplateId,
                    computeTemplateConsolidationData);
        }

        return computeTemplateConsolidationData;
    }

    private String getNodeType(NodeTemplate computeNodeTemplate, TranslateTo translateTo, String targetResourceId,
            String nodeTemplateId) {

        if (Objects.isNull(computeNodeTemplate)) {
            Resource targetResource = translateTo.getHeatOrchestrationTemplate().getResources().get(targetResourceId);
            NameExtractor nodeTypeNameExtractor = TranslationContext.getNameExtractorImpl(targetResource.getType());
            return nodeTypeNameExtractor.extractNodeTypeName(translateTo.getHeatOrchestrationTemplate()
                                    .getResources().get(nodeTemplateId), nodeTemplateId, nodeTemplateId);
        }

        return computeNodeTemplate.getType();
    }
}