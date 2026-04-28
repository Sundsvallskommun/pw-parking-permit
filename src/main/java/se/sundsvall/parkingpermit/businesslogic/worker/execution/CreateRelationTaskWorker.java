package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.RelatedErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.List;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.PartyAssetsService;
import se.sundsvall.parkingpermit.service.RelationService;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASE_DATA_REASON_APPEAL;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_STATUS_ACTIVE;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

@Component
@ExternalTaskSubscription("CreateRelationTask")
public class CreateRelationTaskWorker extends AbstractTaskWorker {

	private final PartyAssetsService partyAssetsService;
	private final RelationService relationService;

	CreateRelationTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, PartyAssetsService partyAssetService,
		RelationService relationService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.partyAssetsService = partyAssetService;
		this.relationService = relationService;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CreateRelationTask");
			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var relatedErrand = getAppealedErrand(errand);

			if (!isNull(relatedErrand)) {
				final var appealedErrand = getErrand(municipalityId, namespace, relatedErrand.getErrandId());

				final var assets = partyAssetsService.getAssets(municipalityId, getAssetId(appealedErrand), getStakeholderPersonIdOfApplicant(appealedErrand), PARTY_ASSET_STATUS_ACTIVE);

				final var existingAsset = ofNullable(assets).orElse(emptyList()).stream()
					.findFirst();

				existingAsset.ifPresent(asset -> {
					logInfo("Asset already exists, create relation between case with id {} and asset with id {}", errand.getId(), asset.getId());
					relationService.createRelation(municipalityId, namespace, String.valueOf(errand.getId()), asset.getId());
				});
			}

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private String getStakeholderPersonIdOfApplicant(Errand errand) {
		return ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.filter(stakeholder -> stakeholder.getRoles().contains(ROLE_APPLICANT))
			.findFirst()
			.map(Stakeholder::getPersonId)
			.orElse(null);
	}

	private String getAssetId(Errand errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER.equals(extraParameter.getKey()))
			.findFirst()
			.map(ExtraParameter::getValues)
			.filter(values -> !values.isEmpty())
			.map(List::getFirst)
			.orElse(null);
	}

	private RelatedErrand getAppealedErrand(Errand errand) {
		return ofNullable(errand.getRelatesTo()).orElse(emptyList()).stream()
			.filter(relatedErrand -> CASE_DATA_REASON_APPEAL.equals(relatedErrand.getRelationReason()))
			.findFirst()
			.orElse(null);
	}
}
