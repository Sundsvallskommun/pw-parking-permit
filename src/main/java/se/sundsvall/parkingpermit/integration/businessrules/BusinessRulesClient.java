package se.sundsvall.parkingpermit.integration.businessrules;

import generated.se.sundsvall.businessrules.RuleEngineRequest;
import generated.se.sundsvall.businessrules.RuleEngineResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import se.sundsvall.parkingpermit.integration.businessrules.configuration.BusinessRulesConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.businessrules.configuration.BusinessRulesConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.businessrules.url}", configuration = BusinessRulesConfiguration.class)
public interface BusinessRulesClient {

	/**
	 * Method for execute rules in BusinessRules.
	 *
	 * @param ruleEngineRequest request object with data for rules.
	 * @return response object with data from rules.
	 * @throws org.zalando.problem.ThrowableProblem when called service responds with error code.
	 */
	@PostMapping(path = "engine", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	RuleEngineResponse runRuleEngine(RuleEngineRequest ruleEngineRequest);
}
