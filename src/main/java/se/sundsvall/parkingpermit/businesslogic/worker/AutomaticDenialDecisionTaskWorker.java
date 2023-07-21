package se.sundsvall.parkingpermit.businesslogic.worker;

import static generated.se.sundsvall.casedata.AttachmentDTO.CategoryEnum.BESLUT;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.DISMISSAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toAttachment;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toDecision;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toLaw;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStakeholder;

import java.util.Objects;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import se.sundsvall.parkingpermit.service.MessagingService;

@Component
@ExternalTaskSubscription("AutomaticDenialDecisionTask")
public class AutomaticDenialDecisionTaskWorker extends AbstractWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticDenialDecisionTaskWorker.class);
	private static final String PROCESS_ENGINE_FIRST_NAME = "Process";
	private static final String PROCESS_ENGINE_LAST_NAME = "Engine";

	@Autowired
	private MessagingService messagingService;

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var errand = getErrand(externalTask);
			LOGGER.info("Executing automatic addition of dismissal to errand with id {}", errand.getId());

			// PE needs to be added as stakeholder to the errand (if not already present) and store for later use when setting
			// "decidedBy" on decision
			final var stakeholder = ofNullable(errand.getStakeholders()).orElse(emptyList())
				.stream()
				.filter(AutomaticDenialDecisionTaskWorker::isProcessEngineStakeholder)
				.findAny()
				.orElseGet(() -> createProcessEngineStakeholder(errand));

			final var props = messagingService.getProperties();
			final var pdf = messagingService.renderPdf(errand);
			final var decision = toDecision(FINAL, DISMISSAL, props.dismissalDescription())
				.decidedBy(stakeholder)
				.addLawItem(toLaw(props.lawHeading(), props.lawSfs(), props.lawChapter(), props.lawArticle()))
				.addAttachmentsItem(toAttachment(BESLUT, props.filename(), "pdf", APPLICATION_PDF_VALUE, pdf));

			caseDataClient.patchNewDecision(errand.getId(), decision);

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			LOGGER.error("Exception occurred in {} for task with id {} and businesskey {}", this.getClass().getSimpleName(), externalTask.getId(), externalTask.getBusinessKey(), exception);

			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private StakeholderDTO createProcessEngineStakeholder(final ErrandDTO errand) {
		final var id = extractStakeholderId(caseDataClient.addStakeholderToErrand(errand.getId(), toStakeholder(ADMINISTRATOR, PERSON, PROCESS_ENGINE_FIRST_NAME, PROCESS_ENGINE_LAST_NAME)));
		return caseDataClient.getStakeholder(id);
	}

	private Long extractStakeholderId(final ResponseEntity<Void> response) {
		return ofNullable(response.getHeaders().get(LOCATION)).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(locationValue -> locationValue.substring(locationValue.lastIndexOf('/') + 1))
			.map(Long::valueOf)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(Status.BAD_GATEWAY, "CaseData integration did not return any location for created stakeholder"));
	}

	private static boolean isProcessEngineStakeholder(StakeholderDTO stakeholder) {
		return stakeholder.getRoles().contains(ADMINISTRATOR) &&
			PROCESS_ENGINE_FIRST_NAME.equals(stakeholder.getFirstName()) &&
			PROCESS_ENGINE_LAST_NAME.equals(stakeholder.getLastName());
	}
}
