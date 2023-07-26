package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.DOCTOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.businesslogic.worker.actualization.VerifyResidentOfMunicipalityTaskWorker.MAIN_ADDRESS_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.citizen.CitizenClient;

@ExtendWith(MockitoExtension.class)
class VerifyResidentOfMunicipalityTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final UUID PERSON_ID = randomUUID();
	private static final long ERRAND_ID = 123L;
	private static final String THIS_MUNICIPALITY_ID = "2281";
	private static final String OTHER_MUNICIPALITY_ID = "1234";

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ErrandDTO errandMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@Mock
	private CitizenClient citizenClientMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@InjectMocks
	private VerifyResidentOfMunicipalityTaskWorker worker;

	@BeforeEach
	void setup() {
		ReflectionTestUtils.setField(worker, "requiredMunicipalityId", THIS_MUNICIPALITY_ID);
	}

	@Test
	void executeForExistingCitizenThatBelongsToCorrectMunicipality() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, THIS_MUNICIPALITY_ID)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, DOCTOR, "Dr", "Who"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeForExistingCitizenThatBelongsToWrongMunicipality() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, OTHER_MUNICIPALITY_ID)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, DOCTOR, "Dr", "Who"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, true));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeForNonExistingCitizen() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.empty());
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, DOCTOR, "Dr", "Who"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeForExistingCitizenWithoutPopulationregistrationAddress() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, OTHER_MUNICIPALITY_ID).addresses(null)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, DOCTOR, "Dr", "Who"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, THIS_MUNICIPALITY_ID)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, DOCTOR, "Dr", "Who"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ADMINISTRATOR, "Administrator", "Dude")));

		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock);
	}

	private StakeholderDTO createStakeholder(String personId, RolesEnum role, String firstName, String lastName) {
		return new StakeholderDTO()
			.personId(personId)
			.firstName(firstName)
			.lastName(lastName)
			.roles(List.of(role))
			.type(PERSON);
	}

	private CitizenExtended createCitizen(UUID personId, String municipalityId) {
		return new CitizenExtended()
			.personId(personId)
			.addresses(List.of(new CitizenAddress()
				.addressType(MAIN_ADDRESS_TYPE)
				.municipality(municipalityId)));
	}
}
