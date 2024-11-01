package se.sundsvall.parkingpermit.integration.businessrules.mapper;

import generated.se.sundsvall.businessrules.Fact;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Stakeholder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.DefaultProblem;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.*;

@ExtendWith(MockitoExtension.class)
class BusinessRulesMapperTest {

	@Mock
	private Errand errandMock;
	@Mock
	private Stakeholder stakeholderMock;

	@Test
	void toRuleEngineRequestWhenDriverNewParkingPermit() {

		// Arrange
		final var applicantPersonId = "applicantPersonId";
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).addValuesItem("DRIVER"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_DURATION).addValuesItem("disabilityDuration"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_ABILITY).addValuesItem("walkingAbility"),
			//Test that empty values are not included
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX).addValuesItem("")));

		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));

		// Act
		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock);

		// Assert and verify
		assertThat(request.getContext()).isEqualTo("PARKING_PERMIT");
		assertThat(request.getFacts()).hasSize(5).
			extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", "PARKING_PERMIT"),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "DRIVER"),
				tuple(CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility"),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId)
			);
	}

	@Test
	void toRuleEngineRequestWhenPassengerNewParkingPermit() {

		// Arrange
		final var applicantPersonId = "applicantPersonId";
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).addValuesItem("PASSENGER"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_DURATION).addValuesItem("disabilityDuration"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING).addValuesItem("disabilityCanBeAloneWhileParking"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_ABILITY).addValuesItem("walkingAbility"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX).addValuesItem("walkingDistanceMax")));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));

		// Act
		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock);

		// Assert and verify
		assertThat(request.getContext()).isEqualTo("PARKING_PERMIT");
		assertThat(request.getFacts()).hasSize(7).
			extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", "PARKING_PERMIT"),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "PASSENGER"),
				tuple(CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration"),
				tuple(CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING, "disabilityCanBeAloneWhileParking"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId)
			);
	}

	@Test
	void toRuleEngineRequestWhenDriverRenewalParkingPermit() {

		// Arrange
		final var applicantPersonId = "applicantPersonId";
		final var extraParameters = List.of(
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).addValuesItem("DRIVER"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_DURATION).addValuesItem("disabilityDuration"),
			new ExtraParameter(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES).addValuesItem("changedCircumstances"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_ABILITY).addValuesItem("walkingAbility"),
			//Test that empty values are not included
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX).addValuesItem(""));

		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_PARKING_PERMIT_RENEWAL);
		when(errandMock.getExtraParameters()).thenReturn(extraParameters);
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));

		// Act
		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock);

		// Assert and verify
		assertThat(request.getContext()).isEqualTo("PARKING_PERMIT");
		assertThat(request.getFacts()).hasSize(6).
			extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", "PARKING_PERMIT_RENEWAL"),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "DRIVER"),
				tuple(CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration"),
				tuple(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES, "changedCircumstances"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility"),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId)
			);
	}

	@Test
	void toRuleEngineRequestWhenPassengerRenewalParkingPermit() {

		// Arrange
		final var applicantPersonId = "applicantPersonId";
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_PARKING_PERMIT_RENEWAL);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).addValuesItem("PASSENGER"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_DURATION).addValuesItem("disabilityDuration"),
			new ExtraParameter(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES).addValuesItem("changedCircumstances"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING).addValuesItem("disabilityCanBeAloneWhileParking"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_ABILITY).addValuesItem("walkingAbility"),
			new ExtraParameter(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX).addValuesItem("walkingDistanceMax")));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));

		// Act
		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock);

		// Assert and verify
		assertThat(request.getContext()).isEqualTo("PARKING_PERMIT");
		assertThat(request.getFacts()).hasSize(8).
			extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", "PARKING_PERMIT_RENEWAL"),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "PASSENGER"),
				tuple(CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration"),
				tuple(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES, "changedCircumstances"),
				tuple(CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING, "disabilityCanBeAloneWhileParking"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId)
			);
	}

	@Test
	void throwsExceptionWhenCaseTypeIsUnknown() {
		final var unknownCaseType = "UNKNOWN";
		// Arrange
		when(errandMock.getCaseType()).thenReturn(unknownCaseType);

		// Act and assert
		assertThatThrownBy(() -> BusinessRulesMapper.toRuleEngineRequest(errandMock))
			.isInstanceOf(DefaultProblem.class)
			.hasMessage("Bad Request: Unsupported case type " + unknownCaseType);
	}

	@Test
	void throwsExceptionWhenCaseTypeIsNull() {

		// Arrange
		when(errandMock.getCaseType()).thenReturn(null);

		// Act and assert
		assertThatThrownBy(() -> BusinessRulesMapper.toRuleEngineRequest(errandMock))
			.isInstanceOf(DefaultProblem.class)
			.hasMessage("Bad Request: Case type is null");
	}
}
