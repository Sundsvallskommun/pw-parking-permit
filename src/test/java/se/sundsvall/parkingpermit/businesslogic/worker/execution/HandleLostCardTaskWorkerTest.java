package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Note;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.partyassets.Asset;
import java.util.List;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.PartyAssetsService;

import static generated.se.sundsvall.casedata.NoteType.PUBLIC;
import static generated.se.sundsvall.partyassets.Status.ACTIVE;
import static generated.se.sundsvall.partyassets.Status.BLOCKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_LOST_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_STATUS_ACTIVE;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_TYPE;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

@ExtendWith(MockitoExtension.class)
class HandleLostCardTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private Errand errandMock;

	@Mock
	private PartyAssetsService partyAssetsServiceMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@Captor
	private ArgumentCaptor<PatchErrand> patchErrandArgumentCaptor;

	@Captor
	private ArgumentCaptor<Note> noteArgumentCaptor;

	@InjectMocks
	private HandleLostCardTaskWorker worker;

	@Test
	void execute() {
		// Arrange
		final var personId = "PersonId";
		final var assetId = "AssetId";
		final var idOfAsset = "idOfAsset";
		final var stakeholder = new Stakeholder().personId(personId).addRolesItem(ROLE_APPLICANT);
		final var assetParkingPermit = new Asset().id(idOfAsset).assetId(assetId).status(ACTIVE).type(PARTY_ASSET_TYPE);
		final var assetOther = new Asset().id(idOfAsset).assetId(assetId).status(ACTIVE).type("OTHER");

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_LOST_PARKING_PERMIT);
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholder));
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(partyAssetsServiceMock.getAssets(MUNICIPALITY_ID, personId, PARTY_ASSET_STATUS_ACTIVE)).thenReturn(List.of(assetParkingPermit, assetOther));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(partyAssetsServiceMock).getAssets(MUNICIPALITY_ID, personId, PARTY_ASSET_STATUS_ACTIVE);
		verify(partyAssetsServiceMock).updateAssetWithNewStatus(MUNICIPALITY_ID, idOfAsset, BLOCKED, "LOST");
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandArgumentCaptor.capture());
		verify(caseDataClientMock).addNoteToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), noteArgumentCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);

		assertThat(patchErrandArgumentCaptor.getValue().getExtraParameters()).hasSize(1).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactly(tuple(CASEDATA_KEY_ARTEFACT_LOST_PERMIT_NUMBER, List.of(assetId)));
		assertThat(noteArgumentCaptor.getValue()).isEqualTo(new Note()
			.municipalityId(MUNICIPALITY_ID)
			.namespace(NAMESPACE)
			.noteType(PUBLIC)
			.title("Asset blocked")
			.text("The asset with ID idOfAsset has been blocked."));
		verifyNoMoreInteractions(caseDataClientMock, partyAssetsServiceMock, externalTaskServiceMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Arrange
		final var errand = new Errand().id(ERRAND_ID).caseType(CASE_TYPE_LOST_PARKING_PERMIT);

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);

		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		doThrow(thrownException).when(partyAssetsServiceMock).getAssets(any(), any(), any());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
	}
}
