package se.sundsvall.parkingpermit.integration.businessrules.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.parkingpermit.Constants.BUSINESS_RULES_KEY_ATTACHMENT_MEDICAL_CONFIRMATION;
import static se.sundsvall.parkingpermit.Constants.BUSINESS_RULES_KEY_ATTACHMENT_PASSPORT_PHOTO;
import static se.sundsvall.parkingpermit.Constants.BUSINESS_RULES_KEY_ATTACHMENT_SIGNATURE;
import static se.sundsvall.parkingpermit.Constants.BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_ATTACHMENT_CATEGORY_MEDICAL_CONFIRMATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_ATTACHMENT_CATEGORY_PASSPORT_PHOTO;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_ATTACHMENT_CATEGORY_SIGNATURE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_APPLICANT_SIGNING_ABILITY;
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

import generated.se.sundsvall.businessrules.Fact;
import generated.se.sundsvall.businessrules.RuleEngineRequest;
import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.zalando.problem.Problem;

public final class BusinessRulesMapper {

	private static final String KEY_TYPE = "type";
	private static final String CONTEXT_PARKING_PERMIT = "PARKING_PERMIT";
	private static final String APPLICANT_DRIVER = "DRIVER";

	private static final List<String> KEYS_PARKING_PERMIT = List.of(
		CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY,
		CASEDATA_KEY_DISABILITY_DURATION,
		CASEDATA_KEY_DISABILITY_WALKING_ABILITY,
		CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX,
		CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING,
		CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES,
		CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER,
		CASEDATA_KEY_APPLICATION_APPLICANT_SIGNING_ABILITY);

	private static final List<String> KEYS_PARKING_PERMIT_ATTACHMENTS = List.of(

	);

	private BusinessRulesMapper() {}

	public static RuleEngineRequest toRuleEngineRequest(Errand errand, List<Attachment> attachments) {
		if (errand.getCaseType() == null) {
			throw Problem.valueOf(BAD_REQUEST, "Case type is null");
		}

		final var ruleEngineRequest = new RuleEngineRequest();
		ruleEngineRequest.setContext(CONTEXT_PARKING_PERMIT);

		switch (errand.getCaseType()) {
			case CASE_TYPE_PARKING_PERMIT -> ruleEngineRequest.addFactsItem(toFact(KEY_TYPE, CASE_TYPE_PARKING_PERMIT));
			case CASE_TYPE_PARKING_PERMIT_RENEWAL -> ruleEngineRequest.addFactsItem(toFact(KEY_TYPE, CASE_TYPE_PARKING_PERMIT_RENEWAL));
			case CASE_TYPE_LOST_PARKING_PERMIT -> ruleEngineRequest.addFactsItem(toFact(KEY_TYPE, CASE_TYPE_LOST_PARKING_PERMIT));
			default -> throw Problem.valueOf(BAD_REQUEST, "Unsupported case type " + errand.getCaseType());
		}

		ruleEngineRequest.addFactsItem(toFact(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, getApplicantPersonId(errand)));
		toFactsFromExtraParameter(KEYS_PARKING_PERMIT, errand).forEach(ruleEngineRequest::addFactsItem);
		toFactsFromAttachments(attachments).forEach(ruleEngineRequest::addFactsItem);
		return ruleEngineRequest;
	}

	private static List<Fact> toFactsFromAttachments(List<Attachment> attachments) {
		return List.of(
			toFact(BUSINESS_RULES_KEY_ATTACHMENT_MEDICAL_CONFIRMATION, attachmentWithCategoryExists(attachments, CASEDATA_ATTACHMENT_CATEGORY_MEDICAL_CONFIRMATION).toString()),
			toFact(BUSINESS_RULES_KEY_ATTACHMENT_PASSPORT_PHOTO, attachmentWithCategoryExists(attachments, CASEDATA_ATTACHMENT_CATEGORY_PASSPORT_PHOTO).toString()),
			toFact(BUSINESS_RULES_KEY_ATTACHMENT_SIGNATURE, attachmentWithCategoryExists(attachments, CASEDATA_ATTACHMENT_CATEGORY_SIGNATURE).toString()));
	}

	private static Boolean attachmentWithCategoryExists(List<Attachment> attachments, String category) {
		return ofNullable(attachments).orElse(emptyList()).stream().anyMatch(attachment -> category.equals(attachment.getCategory()));
	}

	private static List<Fact> toFactsFromExtraParameter(List<String> keys, Errand errand) {
		return keys.stream()
			.map(key -> toFact(key, getExtraParameterValue(errand, key)))
			.filter(Objects::nonNull)
			.toList();
	}

	private static Fact toFact(String key, String value) {
		if (isEmpty(value)) {
			return null;
		}
		return new Fact().key(key).value(value);
	}

	private static String getExtraParameterValue(Errand errand, String key) {
		final var parameter = errand.getExtraParameters();
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> key.equals(extraParameter.getKey()))
			.findFirst()
			.flatMap(extraParameter -> extraParameter.getValues().stream().filter(Objects::nonNull).findFirst())
			.orElse(null);
	}

	private static String getApplicantPersonId(Errand errand) {
		return Optional.ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.filter(stakeholder -> stakeholder.getRoles().stream().anyMatch(ROLE_APPLICANT::equals))
			.findFirst()
			.map(Stakeholder::getPersonId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "No applicant found in errand: " + errand.getErrandNumber()));
	}
}
