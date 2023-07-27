package se.sundsvall.parkingpermit.service;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.parkingpermit.integration.templating.mapper.TemplatingMapper.toRenderRequestWhenNotMemberOfMunicipality;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getStakeholder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.templating.RenderResponse;
import se.sundsvall.parkingpermit.integration.messaging.MessagingClient;
import se.sundsvall.parkingpermit.integration.messaging.mapper.MessagingMapper;
import se.sundsvall.parkingpermit.integration.templating.TemplatingClient;

@Service
public class MessagingService {

	@Autowired
	private MessagingClient messagingClient;

	@Autowired
	private TemplatingClient templatingClient;

	@Autowired
	private MessagingMapper messagingMapper;

	public RenderResponse renderPdf(ErrandDTO errand) {
		return templatingClient.renderPdf(toRenderRequestWhenNotMemberOfMunicipality(errand));
	}

	public UUID sendMessageToNonCitizen(ErrandDTO errand, RenderResponse pdf) {
		final var partyId = getStakeholder(errand, PERSON, APPLICANT).getPersonId();

		if (isNotEmpty(errand.getExternalCaseId())) {
			final var messageResult = messagingClient.sendWebMessage(messagingMapper.toWebMessageRequest(pdf, partyId));
			return extractId(List.of(messageResult));
		}
		final var messageResult = messagingClient.sendLetter(messagingMapper.toLetterRequest(pdf, partyId));
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
