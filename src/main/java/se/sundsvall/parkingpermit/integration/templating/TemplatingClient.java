package se.sundsvall.parkingpermit.integration.templating;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.templating.configuration.TemplatingConfiguration.CLIENT_ID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import generated.se.sundsvall.templating.RenderRequest;
import generated.se.sundsvall.templating.RenderResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import se.sundsvall.parkingpermit.integration.templating.configuration.TemplatingConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.templating.url}", configuration = TemplatingConfiguration.class)
public interface TemplatingClient {

	/**
	 * Render a stored template (with optional parameters) as a PDF
	 * 
	 * @param pdfRequest containing information regarding what template and version to use
	 * @return a RenderResponse containing the rendered PDF
	 */
	@PostMapping(path = "/render/pdf", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	RenderResponse renderPdf(@RequestBody RenderRequest pdfRequest);
}
