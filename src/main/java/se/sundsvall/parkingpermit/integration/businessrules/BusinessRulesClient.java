package se.sundsvall.parkingpermit.integration.businessrules;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.businessrules.configuration.BusinessRulesConfiguration.CLIENT_ID;

import generated.se.sundsvall.businessrules.RuleEngineRequest;
import generated.se.sundsvall.businessrules.RuleEngineResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import se.sundsvall.parkingpermit.integration.businessrules.configuration.BusinessRulesConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.businessrules.url}", configuration = BusinessRulesConfiguration.class)
public interface BusinessRulesClient {

	/**
	 * Method for execute rules in BusinessRules.
	 *
	 * @param  municipalityId                       the municipalityId.
	 * @param  ruleEngineRequest                    request object with data for rules.
	 * @return                                      response object with data from rules.
	 * @throws org.zalando.problem.ThrowableProblem when called service responds with error code.
	 */
	@PostMapping(path = "/{municipalityId}/engine", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	RuleEngineResponse runRuleEngine(@PathVariable("municipalityId") String municipalityId, RuleEngineRequest ruleEngineRequest);
}
