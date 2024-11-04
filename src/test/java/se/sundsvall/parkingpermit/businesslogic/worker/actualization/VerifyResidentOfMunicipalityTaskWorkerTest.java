package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.citizen.CitizenClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;
import static se.sundsvall.parkingpermit.businesslogic.worker.actualization.VerifyResidentOfMunicipalityTaskWorker.MAIN_ADDRESS_TYPE;

@ExtendWith(MockitoExtension.class)
class VerifyResidentOfMunicipalityTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final UUID PERSON_ID = randomUUID();
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKINGPERMIT";
	private static final String OTHER_MUNICIPALITY_ID = "1234";
	private static final String ROLE_DOCTOR = "DOCTOR";

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private Errand errandMock;

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

	@Test
	void executeForExistingCitizenThatBelongsToCorrectMunicipality() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, MUNICIPALITY_ID)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, ROLE_DOCTOR, "Dr", "Who"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), ROLE_APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeForExistingCitizenThatBelongsToWrongMunicipality() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, OTHER_MUNICIPALITY_ID)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, ROLE_DOCTOR, "Dr", "Who"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), ROLE_APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, true));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeForNonExistingCitizen() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.empty());
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, ROLE_DOCTOR, "Dr", "Who"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), ROLE_APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeForExistingCitizenWithoutPopulationregistrationAddress() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, OTHER_MUNICIPALITY_ID).addresses(null)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, ROLE_DOCTOR, "Dr", "Who"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), ROLE_APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Dude")));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(citizenClientMock.getCitizen(PERSON_ID.toString())).thenReturn(Optional.of(createCitizen(PERSON_ID, MUNICIPALITY_ID)));
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, ROLE_DOCTOR, "Dr", "Who"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Lady"),
			createStakeholder(PERSON_ID.toString(), ROLE_APPLICANT, "Mr", "Applicant"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Administrator", "Dude")));

		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(citizenClientMock).getCitizen(PERSON_ID.toString());
		verify(errandMock).getStakeholders();
		verifyNoInteractions(camundaClientMock);
	}

	private Stakeholder createStakeholder(String personId, String role, String firstName, String lastName) {
		return new Stakeholder()
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
