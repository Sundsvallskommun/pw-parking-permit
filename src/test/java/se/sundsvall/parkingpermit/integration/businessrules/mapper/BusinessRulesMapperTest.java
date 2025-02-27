package se.sundsvall.parkingpermit.integration.businessrules.mapper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.when;
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
import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.DefaultProblem;

@ExtendWith(MockitoExtension.class)
class BusinessRulesMapperTest {

	@Mock
	private Errand errandMock;
	@Mock
	private Stakeholder stakeholderMock;

	@ParameterizedTest
	@ValueSource(strings = {
		CASE_TYPE_PARKING_PERMIT, CASE_TYPE_PARKING_PERMIT_RENEWAL, CASE_TYPE_LOST_PARKING_PERMIT
	})
	void toRuleEngineRequestAllFacts(String type) {
		final var applicantPersonId = "applicantPersonId";
		final var attachments = List.of(
			new Attachment().category(CASEDATA_ATTACHMENT_CATEGORY_MEDICAL_CONFIRMATION),
			new Attachment().category(CASEDATA_ATTACHMENT_CATEGORY_PASSPORT_PHOTO),
			new Attachment().category(CASEDATA_ATTACHMENT_CATEGORY_SIGNATURE));

		when(errandMock.getCaseType()).thenReturn(type);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).addValuesItem("applicationApplicantCapacity"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_DURATION).addValuesItem("disabilityDuration"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_ABILITY).addValuesItem("walkingAbility"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX).addValuesItem("walkingDistanceMax"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING).addValuesItem("disabilityCanBeAloneWhileParking"),
			new ExtraParameter(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES).addValuesItem("changedCircumstances"),
			new ExtraParameter(CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER).addValuesItem("policeReportNumber"),
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_SIGNING_ABILITY).addValuesItem("applicantSigningAbility")));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));

		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock, attachments);

		assertThat(request.getContext()).isEqualTo("PARKING_PERMIT");
		assertThat(request.getFacts()).hasSize(13).extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", type),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "applicationApplicantCapacity"),
				tuple(CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"),
				tuple(CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING, "disabilityCanBeAloneWhileParking"),
				tuple(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES, "changedCircumstances"),
				tuple(CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER, "policeReportNumber"),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_SIGNING_ABILITY, "applicantSigningAbility"),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_MEDICAL_CONFIRMATION, "true"),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_PASSPORT_PHOTO, "true"),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_SIGNATURE, "true"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		CASE_TYPE_PARKING_PERMIT, CASE_TYPE_PARKING_PERMIT_RENEWAL, CASE_TYPE_LOST_PARKING_PERMIT
	})
	void toRuleEngineRequestNoExtraParametersOrAttachments(String type) {
		final var applicantPersonId = "applicantPersonId";
		when(errandMock.getCaseType()).thenReturn(type);
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));

		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock, emptyList());

		assertThat(request.getFacts()).hasSize(5).extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", type),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_MEDICAL_CONFIRMATION, "false"),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_PASSPORT_PHOTO, "false"),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_SIGNATURE, "false"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		CASE_TYPE_PARKING_PERMIT, CASE_TYPE_PARKING_PERMIT_RENEWAL, CASE_TYPE_LOST_PARKING_PERMIT
	})
	void toRuleEngineRequestExtraParametersAllEmpty(String type) {
		final var applicantPersonId = "applicantPersonId";
		final var attachments = List.of(new Attachment().category(CASEDATA_ATTACHMENT_CATEGORY_MEDICAL_CONFIRMATION));

		when(errandMock.getCaseType()).thenReturn(type);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_DURATION),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_ABILITY),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING),
			new ExtraParameter(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES),
			new ExtraParameter(CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER),
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_SIGNING_ABILITY)));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));

		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock, attachments);

		assertThat(request.getFacts()).hasSize(5).extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", type),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_MEDICAL_CONFIRMATION, "true"),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_PASSPORT_PHOTO, "false"),
				tuple(BUSINESS_RULES_KEY_ATTACHMENT_SIGNATURE, "false"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		CASE_TYPE_PARKING_PERMIT, CASE_TYPE_PARKING_PERMIT_RENEWAL, CASE_TYPE_LOST_PARKING_PERMIT
	})
	void throwsExceptionWhenApplicantIsMissing(String type) {
		when(errandMock.getCaseType()).thenReturn(type);

		assertThatThrownBy(() -> BusinessRulesMapper.toRuleEngineRequest(errandMock, emptyList()))
			.isInstanceOf(DefaultProblem.class)
			.hasMessage("Bad Request: No applicant found in errand: null");

	}

	@Test
	void throwsExceptionWhenCaseTypeIsUnknown() {
		final var unknownCaseType = "UNKNOWN";
		// Arrange
		when(errandMock.getCaseType()).thenReturn(unknownCaseType);

		// Act and assert
		assertThatThrownBy(() -> BusinessRulesMapper.toRuleEngineRequest(errandMock, emptyList()))
			.isInstanceOf(DefaultProblem.class)
			.hasMessage("Bad Request: Unsupported case type " + unknownCaseType);
	}

	@Test
	void throwsExceptionWhenCaseTypeIsNull() {

		// Arrange
		when(errandMock.getCaseType()).thenReturn(null);

		// Act and assert
		assertThatThrownBy(() -> BusinessRulesMapper.toRuleEngineRequest(errandMock, emptyList()))
			.isInstanceOf(DefaultProblem.class)
			.hasMessage("Bad Request: Case type is null");
	}
}
