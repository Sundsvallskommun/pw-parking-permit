package se.sundsvall.parkingpermit.service;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;
import static se.sundsvall.parkingpermit.integration.templating.mapper.TemplatingMapper.toRenderDecisionRequest;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getStakeholder;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.integration.messaging.MessagingClient;
import se.sundsvall.parkingpermit.integration.messaging.mapper.MessagingMapper;
import se.sundsvall.parkingpermit.integration.templating.TemplatingClient;

@Service
public class MessagingService {

	private final MessagingClient messagingClient;

	private final TemplatingClient templatingClient;

	private final MessagingMapper messagingMapper;

	MessagingService(MessagingClient messagingClient, TemplatingClient templatingClient, MessagingMapper messagingMapper) {
		this.messagingClient = messagingClient;
		this.templatingClient = templatingClient;
		this.messagingMapper = messagingMapper;
	}

	// TODO: Get template identifier from configuration
	public RenderResponse renderPdfDecision(String municipalityId, Errand errand, String templateIdentifier) {

		return templatingClient.renderPdf(municipalityId, toRenderDecisionRequest(errand, templateIdentifier));
	}

	public UUID sendMessageToNonCitizen(String municipalityId, Errand errand, RenderResponse pdf) {
		final var partyId = getStakeholder(errand, PERSON, ROLE_APPLICANT).getPersonId();

		if (isNotEmpty(errand.getExternalCaseId())) {
			final var messageResult = messagingClient.sendWebMessage(municipalityId, messagingMapper.toWebMessageRequestDenial(pdf, partyId, errand.getExternalCaseId(), municipalityId));
			return extractId(List.of(messageResult));
		}
		final var messageResult = messagingClient.sendLetter(municipalityId, messagingMapper.toLetterRequestDenial(pdf, partyId, municipalityId));
		return extractId(messageResult.getMessages());
	}

	public UUID sendDecisionMessage(String municipalityId, Errand errand, RenderResponse pdf, boolean isApproved) {
		final var partyId = getStakeholder(errand, PERSON, ROLE_APPLICANT).getPersonId();

		final var messageResult = messagingClient.sendDigitalMail(municipalityId, messagingMapper.toDigitalMailRequest(pdf, partyId, municipalityId, isApproved));

		return extractId(messageResult.getMessages());
	}

	public UUID sendMessageSimplifiedService(String municipalityId, Errand errand) {
		final var partyId = getStakeholder(errand, PERSON, ROLE_APPLICANT).getPersonId();

		if (isNotEmpty(errand.getExternalCaseId())) {
			final var messageResult = messagingClient.sendWebMessage(municipalityId, messagingMapper.toWebMessageRequestSimplifiedService(partyId, errand.getExternalCaseId(),
				municipalityId));
			return extractId(List.of(messageResult));
		}
		final var messageResult = messagingClient.sendLetter(municipalityId, messagingMapper.toLetterRequestSimplifiedService(partyId, municipalityId));

		return extractId(messageResult.getMessages());
	}

	private UUID extractId(List<MessageResult> messageResults) {
		return ofNullable(messageResults).orElse(emptyList()).stream()
			.map(MessageResult::getMessageId)
			.filter(Objects::nonNull)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "No message id received from messaging service"));
	}
}
