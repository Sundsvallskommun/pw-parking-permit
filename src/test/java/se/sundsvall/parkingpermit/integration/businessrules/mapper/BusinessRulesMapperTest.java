package se.sundsvall.parkingpermit.integration.businessrules.mapper;

import generated.se.sundsvall.businessrules.Fact;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.DefaultProblem;

import java.util.List;
import java.util.Map;

import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.ANMALAN_ATTEFALL;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT_RENEWAL;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_DURATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_WALKING_ABILITY;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX;

@ExtendWith(MockitoExtension.class)
class BusinessRulesMapperTest {

	@Mock
	private ErrandDTO errandMock;
	@Mock
	private StakeholderDTO stakeholderMock;

	@Test
	void toRuleEngineRequestWhenDriverNewParkingPermit() {

		// Arrange
		final var applicantPersonId = "applicantPersonId";
		when(errandMock.getCaseType()).thenReturn(PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
				CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "DRIVER",
				CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration",
				CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility",
				CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"
		));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));

		// Act
		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock);

		// Assert and verify
		assertThat(request.getContext()).isEqualTo("PARKING_PERMIT");
		assertThat(request.getFacts()).hasSize(6).
			extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", "PARKING_PERMIT"),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "DRIVER"),
				tuple(CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId)
			);
	}

	@Test
	void toRuleEngineRequestWhenPassengerNewParkingPermit() {

		// Arrange
		final var applicantPersonId = "applicantPersonId";
		when(errandMock.getCaseType()).thenReturn(PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
			CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "PASSENGER",
			CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration",
			CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING, "disabilityCanBeAloneWhileParking",
			CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility",
			CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"
		));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
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
		when(errandMock.getCaseType()).thenReturn(PARKING_PERMIT_RENEWAL);
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
			CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "DRIVER",
			CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration",
			CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES, "changedCircumstances",
			CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility",
			CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"
		));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));

		// Act
		final var request = BusinessRulesMapper.toRuleEngineRequest(errandMock);

		// Assert and verify
		assertThat(request.getContext()).isEqualTo("PARKING_PERMIT");
		assertThat(request.getFacts()).hasSize(7).
			extracting(Fact::getKey, Fact::getValue)
			.containsExactlyInAnyOrder(
				tuple("type", "PARKING_PERMIT_RENEWAL"),
				tuple(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "DRIVER"),
				tuple(CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration"),
				tuple(CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES, "changedCircumstances"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility"),
				tuple(CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"),
				tuple(BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID, applicantPersonId)
			);
	}

	@Test
	void toRuleEngineRequestWhenPassengerRenewalParkingPermit() {

		// Arrange
		final var applicantPersonId = "applicantPersonId";
		when(errandMock.getCaseType()).thenReturn(PARKING_PERMIT_RENEWAL);
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
			CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY, "PASSENGER",
			CASEDATA_KEY_DISABILITY_DURATION, "disabilityDuration",
			CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES, "changedCircumstances",
			CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING, "disabilityCanBeAloneWhileParking",
			CASEDATA_KEY_DISABILITY_WALKING_ABILITY, "walkingAbility",
			CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"
		));
		when(stakeholderMock.getPersonId()).thenReturn(applicantPersonId);
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
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

		// Arrange
		when(errandMock.getCaseType()).thenReturn(ANMALAN_ATTEFALL);

		// Act and assert
		assertThatThrownBy(() -> BusinessRulesMapper.toRuleEngineRequest(errandMock))
			.isInstanceOf(DefaultProblem.class)
			.hasMessage("Bad Request: Unsupported case type " + ANMALAN_ATTEFALL.name());
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
