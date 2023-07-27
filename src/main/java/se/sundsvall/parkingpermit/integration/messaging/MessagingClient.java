package se.sundsvall.parkingpermit.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.messaging.configuration.MessagingConfiguration.CLIENT_ID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import generated.se.sundsvall.messaging.LetterRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.WebMessageRequest;
import se.sundsvall.parkingpermit.integration.messaging.configuration.MessagingConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.messaging.url}", configuration = MessagingConfiguration.class)
public interface MessagingClient {

	/**
	 * Send a single web-message
	 *
	 * @param  webMessageRequest                    request containing message to send
	 * @return                                      a MessageResult with delivery results and id for sent message
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PostMapping(path = "/webmessage", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendWebMessage(@RequestBody WebMessageRequest webMessageRequest);

	/**
	 * Send a single letter as digital mail with fallback as snail mail if
	 * recipient does not have digital mail
	 *
	 * @param  letterRequest                        request containing message to send
	 * @return                                      a MessageBatchResult with delivery results and id for sent message
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PostMapping(path = "/letter", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageBatchResult sendLetter(@RequestBody LetterRequest letterRequest);
}
