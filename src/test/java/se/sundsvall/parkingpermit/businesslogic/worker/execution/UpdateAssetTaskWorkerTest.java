package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static generated.se.sundsvall.partyassets.Status.ACTIVE;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.RelatedErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.partyassets.Asset;
import java.util.List;
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
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.PartyAssetsService;

@ExtendWith(MockitoExtension.class)
class UpdateAssetTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";
	private static final long ERRAND_ID = 123L;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private PartyAssetsService partyAssetsServiceMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private Errand errandMock;

	@Mock
	private Errand appealedErrandMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private UpdateAssetTaskWorker worker;

	@Test
	void execute() {
		final var asset = new Asset().assetId("assetId").id("id").status(ACTIVE);
		final var stakeholder = new Stakeholder().roles(List.of(ROLE_APPLICANT)).type(PERSON).personId("personId");
		final var relatedErrandId = 456L;
		final var relatedErrand = new RelatedErrand().errandId(relatedErrandId).relationReason("APPEAL");
		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getRelatesTo()).thenReturn(List.of(relatedErrand));
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId)).thenReturn(appealedErrandMock);
		when(appealedErrandMock.getStakeholders()).thenReturn(List.of(stakeholder));
		when(appealedErrandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter().key(CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER).values(List.of("assetId"))));
		when(partyAssetsServiceMock.getAssets(anyString(), anyString(), anyString(), anyString())).thenReturn(List.of(asset));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(appealedErrandMock).getStakeholders();
		verify(appealedErrandMock).getExtraParameters();
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId);
		verify(partyAssetsServiceMock).getAssets(MUNICIPALITY_ID, "assetId", "personId", "ACTIVE");
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenNoRelatedErrand() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getRelatesTo()).thenReturn(emptyList());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		doThrow(thrownException).when(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verifyNoInteractions(partyAssetsServiceMock);
	}
}
