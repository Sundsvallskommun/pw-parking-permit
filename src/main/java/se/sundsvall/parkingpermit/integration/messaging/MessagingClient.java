package se.sundsvall.parkingpermit.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.messaging.configuration.MessagingConfiguration.CLIENT_ID;

import generated.se.sundsvall.messaging.LetterRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.WebMessageRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.parkingpermit.integration.businessrules.configuration.BusinessRulesConfiguration;
import se.sundsvall.parkingpermit.integration.messaging.configuration.MessagingConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.messaging.url}", configuration = MessagingConfiguration.class)
@CircuitBreaker(name = BusinessRulesConfiguration.CLIENT_ID)
public interface MessagingClient {

	/**
	 * Send a single web-message
	 *
	 * @param  municipalityId                       id of municipality
	 * @param  webMessageRequest                    request containing message to send
	 * @return                                      a MessageResult with delivery results and id for sent message
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PostMapping(path = "/{municipalityId}/webmessage", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendWebMessage(@PathVariable("municipalityId") final String municipalityId,
		@RequestBody final WebMessageRequest webMessageRequest);

	/**
	 * Send a single letter as digital mail with fallback as snail mail if recipient does not have digital mail
	 *
	 * @param  municipalityId                       id of municipality
	 * @param  letterRequest                        request containing message to send
	 * @return                                      a MessageBatchResult with delivery results and id for sent message
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PostMapping(path = "/{municipalityId}/letter", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageBatchResult sendLetter(@PathVariable("municipalityId") final String municipalityId,
		@RequestBody final LetterRequest letterRequest);
}
