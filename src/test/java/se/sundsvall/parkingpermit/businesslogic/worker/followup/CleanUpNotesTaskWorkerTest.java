package se.sundsvall.parkingpermit.businesslogic.worker.followup;

import generated.se.sundsvall.casedata.Note;
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

import java.util.List;

import static generated.se.sundsvall.casedata.NoteType.INTERNAL;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;

@ExtendWith(MockitoExtension.class)
class CleanUpNotesTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";
	private static final String VARIABLE_CASE_NUMBER = "caseNumber";
	private static final String VARIABLE_REQUEST_ID = "requestId";
	private static final String VARIABLE_MUNICIPALITY_ID = "municipalityId";

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private CleanUpNotesTaskWorker worker;

	@Test
	void execute() {
		//Arrange
		final var notes = List.of(new Note().id(1L).noteType(INTERNAL), new Note().id(2L).noteType(INTERNAL));
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getNotesByErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, INTERNAL.getValue())).thenReturn(notes);
		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(caseDataClientMock).getNotesByErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, INTERNAL.getValue());
		verify(caseDataClientMock).deleteNoteById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1L);
		verify(caseDataClientMock).deleteNoteById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,2L);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoMoreInteractions(externalTaskMock, externalTaskServiceMock, caseDataClientMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Arrange
		final var notes = List.of(new Note().id(1L).noteType(INTERNAL), new Note().id(2L).noteType(INTERNAL));
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getNotesByErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, INTERNAL.getValue())).thenReturn(notes);
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		doThrow(thrownException).when(caseDataClientMock).deleteNoteById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,1L);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(VARIABLE_MUNICIPALITY_ID);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
	}
}
