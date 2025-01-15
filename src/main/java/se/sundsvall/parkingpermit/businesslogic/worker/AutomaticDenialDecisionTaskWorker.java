package se.sundsvall.parkingpermit.businesslogic.worker;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.DISMISSAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE;
import static se.sundsvall.parkingpermit.Constants.CATEGORY_BESLUT;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toAttachment;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toDecision;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toLaw;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStakeholder;
import static se.sundsvall.parkingpermit.util.TimerUtil.getControlMessageTime;

import generated.se.sundsvall.casedata.Stakeholder;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Objects;
import org.apache.commons.lang3.math.NumberUtils;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.util.SimplifiedServiceTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

@Component
@ExternalTaskSubscription("AutomaticDenialDecisionTask")
public class AutomaticDenialDecisionTaskWorker extends AbstractTaskWorker {

	private static final String PROCESS_ENGINE_FIRST_NAME = "Process";
	private static final String PROCESS_ENGINE_LAST_NAME = "Engine";

	private final MessagingService messagingService;
	private final TextProvider textProvider;
	private final SimplifiedServiceTextProperties simplifiedServiceTextProperties;

	AutomaticDenialDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, MessagingService messagingService, TextProvider textProvider,
		SimplifiedServiceTextProperties simplifiedServiceTextProperties) {
		super(camundaClient, caseDataClient, failureHandler);
		this.messagingService = messagingService;
		this.textProvider = textProvider;
		this.simplifiedServiceTextProperties = simplifiedServiceTextProperties;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);
			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing automatic addition of dismissal to errand with id {}", errand.getId());

			// PE needs to be added as stakeholder to the errand (if not already present) and store for later use when setting
			// "decidedBy" on decision
			final var stakeholder = ofNullable(errand.getStakeholders()).orElse(emptyList())
				.stream()
				.filter(AutomaticDenialDecisionTaskWorker::isProcessEngineStakeholder)
				.findAny()
				.orElseGet(() -> createProcessEngineStakeholder(errand.getId(), municipalityId, namespace));

			final var pdf = messagingService.renderPdf(municipalityId, errand);
			final var decision = toDecision(FINAL, DISMISSAL, textProvider.getDenialTexts().description())
				.decidedBy(stakeholder)
				.decidedAt(OffsetDateTime.now())
				.addLawItem(toLaw(textProvider.getDenialTexts().lawHeading(), textProvider.getDenialTexts().lawSfs(), textProvider.getDenialTexts().lawChapter(), textProvider.getDenialTexts().lawArticle()))
				.addAttachmentsItem(toAttachment(CATEGORY_BESLUT, textProvider.getDenialTexts().filename(), "pdf", APPLICATION_PDF_VALUE, pdf));

			caseDataClient.patchNewDecision(municipalityId, namespace, errand.getId(), decision);

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE, getControlMessageTime(decision, simplifiedServiceTextProperties.delay()));

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private Stakeholder createProcessEngineStakeholder(final Long errandId, final String municipalityId, final String namespace) {
		final var id = extractStakeholderId(caseDataClient.addStakeholderToErrand(municipalityId, namespace, errandId, toStakeholder(ROLE_ADMINISTRATOR, PERSON, PROCESS_ENGINE_FIRST_NAME, PROCESS_ENGINE_LAST_NAME)));
		return caseDataClient.getStakeholder(municipalityId, namespace, errandId, id);
	}

	private Long extractStakeholderId(final ResponseEntity<Void> response) {
		return ofNullable(response.getHeaders().get(LOCATION)).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(locationValue -> locationValue.substring(locationValue.lastIndexOf('/') + 1))
			.filter(NumberUtils::isCreatable)
			.map(Long::valueOf)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(Status.BAD_GATEWAY, "CaseData integration did not return any location for created stakeholder"));
	}

	private static boolean isProcessEngineStakeholder(Stakeholder stakeholder) {
		return stakeholder.getRoles().contains(ROLE_ADMINISTRATOR) &&
			PROCESS_ENGINE_FIRST_NAME.equals(stakeholder.getFirstName()) &&
			PROCESS_ENGINE_LAST_NAME.equals(stakeholder.getLastName());
	}
}
