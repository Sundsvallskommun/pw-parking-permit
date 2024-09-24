package se.sundsvall.parkingpermit.integration.businessrules.mapper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.parkingpermit.Constants.BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_DURATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_WALKING_ABILITY;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT_RENEWAL;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.zalando.problem.Problem;

import generated.se.sundsvall.businessrules.Fact;
import generated.se.sundsvall.businessrules.RuleEngineRequest;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;

public class BusinessRulesMapper {

	private static final String KEY_TYPE = "type";
	private static final String CONTEXT_PARKING_PERMIT = "PARKING_PERMIT";
	private static final String APPLICANT_DRIVER = "DRIVER";

	private static final List<String> KEYS_PARKING_PERMIT_DRIVER = List.of(
		CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY,
		CASEDATA_KEY_DISABILITY_DURATION,
		CASEDATA_KEY_DISABILITY_WALKING_ABILITY,
		CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX);

	private static final List<String> KEYS_PARKING_PERMIT_PASSENGER = List.of(
		CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY,
		CASEDATA_KEY_DISABILITY_DURATION,
		CASEDATA_KEY_DISABILITY_WALKING_ABILITY,
		CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX,
		CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING);

	private static final List<String> KEYS_PARKING_PERMIT_RENEWAL_DRIVER = List.of(
		CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY,
		CASEDATA_KEY_DISABILITY_DURATION,
		CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES,
		CASEDATA_KEY_DISABILITY_WALKING_ABILITY,
		CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX);

	private static final List<String> KEYS_PARKING_PERMIT_RENEWAL_PASSENGER = List.of(
		CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY,
		CASEDATA_KEY_DISABILITY_DURATION,
		CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES,
		CASEDATA_KEY_DISABILITY_WALKING_ABILITY,
		CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX,
		CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING);

	private static final List<String> KEYS_LOST_PARKING_PERMIT = List.of(
		CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER);

	private BusinessRulesMapper() {}

	public static RuleEngineRequest toRuleEngineRequest(ErrandDTO errandDTO) {
		if (errandDTO.getCaseType() == null) {
			throw Problem.valueOf(BAD_REQUEST, "Case type is null");
		}

		return switch (errandDTO.getCaseType()) {
			case CASE_TYPE_PARKING_PERMIT -> toRuleEngineRequestParkingPermit(errandDTO);
			case CASE_TYPE_PARKING_PERMIT_RENEWAL -> toRuleEngineRequestParkingPermitRenewal(errandDTO);
			case CASE_TYPE_LOST_PARKING_PERMIT -> toRuleEngineRequestLostParkingPermit(errandDTO);
			default -> throw Problem.valueOf(BAD_REQUEST, "Unsupported case type " + errandDTO.getCaseType());
		};
	}

	private static RuleEngineRequest toRuleEngineRequestParkingPermit(ErrandDTO errandDTO) {
		final var ruleEngineRequest = new RuleEngineRequest();
		ruleEngineRequest.setContext(CONTEXT_PARKING_PERMIT);
		ruleEngineRequest.addFactsItem(toFact(KEY_TYPE, CASE_TYPE_PARKING_PERMIT));
		ruleEngineRequest.addFactsItem(toFact(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, getApplicantPersonId(errandDTO)));

		if (isDriver(errandDTO)) {
			toFacts(KEYS_PARKING_PERMIT_DRIVER, errandDTO).forEach(ruleEngineRequest::addFactsItem);
		} else {
			toFacts(KEYS_PARKING_PERMIT_PASSENGER, errandDTO).forEach(ruleEngineRequest::addFactsItem);
		}

		return ruleEngineRequest;
	}

	private static RuleEngineRequest toRuleEngineRequestParkingPermitRenewal(ErrandDTO errandDTO) {
		final var ruleEngineRequest = new RuleEngineRequest();
		ruleEngineRequest.setContext(CONTEXT_PARKING_PERMIT);
		ruleEngineRequest.addFactsItem(toFact(KEY_TYPE, CASE_TYPE_PARKING_PERMIT_RENEWAL));
		ruleEngineRequest.addFactsItem(toFact(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, getApplicantPersonId(errandDTO)));

		if (isDriver(errandDTO)) {
			toFacts(KEYS_PARKING_PERMIT_RENEWAL_DRIVER, errandDTO).forEach(ruleEngineRequest::addFactsItem);
		} else {
			toFacts(KEYS_PARKING_PERMIT_RENEWAL_PASSENGER, errandDTO).forEach(ruleEngineRequest::addFactsItem);
		}

		return ruleEngineRequest;
	}

	private static RuleEngineRequest toRuleEngineRequestLostParkingPermit(ErrandDTO errandDTO) {
		final var ruleEngineRequest = new RuleEngineRequest();
		ruleEngineRequest.setContext(CONTEXT_PARKING_PERMIT);
		ruleEngineRequest.addFactsItem(toFact(KEY_TYPE, CASE_TYPE_LOST_PARKING_PERMIT));
		ruleEngineRequest.addFactsItem(toFact(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, getApplicantPersonId(errandDTO)));

		toFacts(KEYS_LOST_PARKING_PERMIT, errandDTO).forEach(ruleEngineRequest::addFactsItem);

		return ruleEngineRequest;
	}

	private static List<Fact> toFacts(List<String> keys, ErrandDTO errandDTO) {
		return keys.stream()
			.map(key -> toFact(key, getExtraParameterValue(errandDTO, key)))
			.filter(Objects::nonNull)
			.toList();
	}

	private static Fact toFact(String key, String value) {
		if (isEmpty(value)) {
			return null;
		}
		return new Fact().key(key).value(value);
	}

	private static String getExtraParameterValue(ErrandDTO errandDTO, String key) {
		return ofNullable(errandDTO.getExtraParameters()).orElse(emptyMap()).get(key);
	}

	private static boolean isDriver(ErrandDTO errandDTO) {
		return APPLICANT_DRIVER.equals(getExtraParameterValue(errandDTO, CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY));
	}

	private static String getApplicantPersonId(ErrandDTO errandDTO) {
		return Optional.ofNullable(errandDTO.getStakeholders()).orElse(emptyList()).stream()
			.filter(stakeholder -> stakeholder.getRoles().stream().anyMatch(ROLE_APPLICANT::equals))
			.findFirst()
			.map(StakeholderDTO::getPersonId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "No applicant found in errand: " + errandDTO.getErrandNumber()));
	}
}
