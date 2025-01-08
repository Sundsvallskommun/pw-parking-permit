package se.sundsvall.parkingpermit.integration.rpa;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.rpa.configuration.RpaConfiguration.CLIENT_ID;

import generated.se.sundsvall.rpa.QueueItemDto;
import generated.se.sundsvall.rpa.QueuesAddQueueItemParameters;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import se.sundsvall.parkingpermit.integration.rpa.configuration.RpaConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.rpa.url}", configuration = RpaConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface RpaClient {

	@PostMapping(path = "/odata/Queues/UiPathODataSvc.AddQueueItem", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	QueueItemDto addQueueItem(@RequestHeader("X-UIPATH-OrganizationUnitId") String folderId, @RequestBody QueuesAddQueueItemParameters queueItem);
}
