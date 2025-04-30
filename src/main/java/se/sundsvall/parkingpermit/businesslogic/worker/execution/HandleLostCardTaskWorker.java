package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import static generated.se.sundsvall.casedata.NoteType.PUBLIC;
import static generated.se.sundsvall.partyassets.Status.BLOCKED;
import static java.util.Collections.emptyList;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_LOST_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_STATUS_ACTIVE;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_TYPE;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Note;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.ArrayList;
import java.util.List;
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
@ExternalTaskSubscription("HandleLostCardTask")
public class HandleLostCardTaskWorker extends AbstractTaskWorker {

	private static final String NOTE_TEXT = "The asset with id %s has been blocked.";
	private static final String NOTE_TITLE = "Asset blocked";
	private static final String PARTY_ASSET_STATUS_REASON = "LOST";

	private final PartyAssetsService partyAssetsService;

	HandleLostCardTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, PartyAssetsService partyAssetsService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.partyAssetsService = partyAssetsService;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			if (CASE_TYPE_LOST_PARKING_PERMIT.equals(errand.getCaseType())) {
				final var assets = partyAssetsService.getAssets(municipalityId, getStakeholderPersonIdOfApplicant(errand), PARTY_ASSET_STATUS_ACTIVE);

				final var existingAsset = Optional.ofNullable(assets).orElse(emptyList()).stream()
					.filter(asset -> PARTY_ASSET_TYPE.equals(asset.getType()))
					.findFirst();

				existingAsset.ifPresent(asset -> {
					partyAssetsService.updateAssetWithNewStatus(municipalityId,
						asset.getId(), BLOCKED, PARTY_ASSET_STATUS_REASON);

					final var extraParameters = Optional.ofNullable(errand.getExtraParameters()).orElse(new ArrayList<>());

					if (extraParameters.stream().noneMatch(extraParameter -> CASEDATA_KEY_ARTEFACT_LOST_PERMIT_NUMBER.equals(extraParameter.getKey()))) {
						extraParameters.add(new ExtraParameter().key(CASEDATA_KEY_ARTEFACT_LOST_PERMIT_NUMBER).values(List.of(asset.getAssetId())));
					} else {
						extraParameters.stream()
							.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_LOST_PERMIT_NUMBER.equals(extraParameter.getKey()))
							.findFirst()
							.ifPresent(extraParameter -> extraParameter.setValues(List.of(asset.getAssetId())));
					}

					caseDataClient.patchErrand(municipalityId, namespace, errand.getId(), new PatchErrand().extraParameters(extraParameters).facilities(null));

					caseDataClient.addNoteToErrand(municipalityId, namespace, errand.getId(),
						new Note()
							.municipalityId(municipalityId)
							.namespace(namespace)
							.noteType(PUBLIC)
							.title(NOTE_TITLE)
							.text(String.format(NOTE_TEXT, asset.getId())));
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
}
