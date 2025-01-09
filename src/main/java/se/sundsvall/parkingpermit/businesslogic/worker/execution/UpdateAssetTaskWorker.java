package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASE_DATA_REASON_APPEAL;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_STATUS_ACTIVE;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.RelatedErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.Collections;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.PartyAssetsService;

@Component
@ExternalTaskSubscription("UpdateAssetTask")
public class UpdateAssetTaskWorker extends AbstractTaskWorker {

	private final PartyAssetsService partyAssetsService;

	UpdateAssetTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, PartyAssetsService partyAssetService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.partyAssetsService = partyAssetService;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for UpdateAssetTask");
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final String namespace = externalTask.getVariable(CAMUNDA_VARIABLE_NAMESPACE);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var relatedErrand = getAppealedErrand(errand);

			if (!isNull(relatedErrand)) {
				final var appealedErrand = getErrand(municipalityId, namespace, relatedErrand.getErrandId());

				final var assets = partyAssetsService.getAssets(municipalityId, getAssetId(appealedErrand), getStakeholderPersonIdOfApplicant(appealedErrand), PARTY_ASSET_STATUS_ACTIVE);

				final var existingAsset = Optional.ofNullable(assets).orElse(emptyList()).stream()
					.findFirst();

				existingAsset.ifPresent(asset -> {
					logInfo("Asset already exists, updating asset with id {}", asset.getId());
					partyAssetsService.updateAssetWithNewAdditionalParameter(municipalityId, asset, caseNumber);
				});
			}

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private String getStakeholderPersonIdOfApplicant(Errand errand) {
		return Optional.ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.filter(stakeholder -> stakeholder.getRoles().contains(ROLE_APPLICANT))
			.findFirst()
			.map(Stakeholder::getPersonId)
			.orElse(null);
	}

	private String getAssetId(Errand errand) {
		return Optional.ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER.equals(extraParameter.getKey()))
			.findFirst()
			.map(extraParameter -> Optional.ofNullable(extraParameter.getValues()).orElse(emptyList()).getFirst())
			.orElse(null);
	}

	private RelatedErrand getAppealedErrand(Errand errand) {
		return ofNullable(errand.getRelatesTo()).orElse(Collections.emptyList()).stream()
			.filter(relatedErrand -> CASE_DATA_REASON_APPEAL.equals(relatedErrand.getRelationReason()))
			.findFirst()
			.orElse(null);
	}
}
