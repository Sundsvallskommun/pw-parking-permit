package se.sundsvall.parkingpermit.businesslogic.worker;

import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.RECOMMENDED;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPEAL;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_APPEAL;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.RelatedErrand;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@ExtendWith(MockitoExtension.class)
class CheckAppealTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@Mock
	private Errand errandMock;

	@Mock
	private Errand appealedErrandMock;

	@Mock
	private RelatedErrand relatedErrandMock;

	@InjectMocks
	private CheckAppealTaskWorker worker;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("CheckAppealTask");
	}

	@Test
	void executeWhenAnAppeal() {
		// Setup
		final var variables = new HashMap<String, Object>();
		variables.put(CAMUNDA_VARIABLE_IS_APPEAL, true);
		variables.put(CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW, true);

		final var relatedErrandId = 456L;

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_APPEAL);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getRelatesTo()).thenReturn(List.of(relatedErrandMock));
		when(errandMock.getApplicationReceived()).thenReturn(OffsetDateTime.now().minusDays(5));

		when(relatedErrandMock.getErrandId()).thenReturn(relatedErrandId);
		when(relatedErrandMock.getRelationReason()).thenReturn("APPEAL");
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId)).thenReturn(appealedErrandMock);
		when(appealedErrandMock.getDecisions()).thenReturn(List.of(new Decision().decisionType(FINAL).decidedAt(OffsetDateTime.now().minusDays(10))));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock).getCaseType();
		verify(relatedErrandMock).getErrandId();
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId);
		verify(appealedErrandMock).getDecisions();
		verify(externalTaskServiceMock).complete(externalTaskMock, variables);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenAnAppealAndNotInTimelinessReview() {
		// Setup
		final var variables = new HashMap<String, Object>();
		variables.put(CAMUNDA_VARIABLE_IS_APPEAL, true);
		variables.put(CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW, false);

		final var relatedErrandId = 456L;

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_APPEAL);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getRelatesTo()).thenReturn(List.of(relatedErrandMock));
		when(errandMock.getApplicationReceived()).thenReturn(OffsetDateTime.now());

		when(relatedErrandMock.getErrandId()).thenReturn(relatedErrandId);
		when(relatedErrandMock.getRelationReason()).thenReturn("APPEAL");
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId)).thenReturn(appealedErrandMock);
		when(appealedErrandMock.getDecisions()).thenReturn(List.of(new Decision().decisionType(FINAL).decidedAt(OffsetDateTime.now().minusDays(22))));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock).getCaseType();
		verify(relatedErrandMock).getErrandId();
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId);
		verify(appealedErrandMock).getDecisions();
		verify(externalTaskServiceMock).complete(externalTaskMock, variables);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenAnAppealAndAppealErrandDecisionIsMissingDecidedAt() {
		// Setup
		final var relatedErrandId = 456L;

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_APPEAL);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getRelatesTo()).thenReturn(List.of(relatedErrandMock));
		when(errandMock.getApplicationReceived()).thenReturn(OffsetDateTime.now());

		when(relatedErrandMock.getErrandId()).thenReturn(relatedErrandId);
		when(relatedErrandMock.getRelationReason()).thenReturn("APPEAL");
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId)).thenReturn(appealedErrandMock);
		when(appealedErrandMock.getDecisions()).thenReturn(List.of(new Decision().decisionType(FINAL)));
		when(appealedErrandMock.getId()).thenReturn(relatedErrandId);
		when(appealedErrandMock.getNamespace()).thenReturn(NAMESPACE);
		when(appealedErrandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock).getCaseType();
		verify(relatedErrandMock).getErrandId();
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId);
		verify(appealedErrandMock).getDecisions();
		verify(appealedErrandMock).getId();
		verify(appealedErrandMock).getNamespace();
		verify(appealedErrandMock).getMunicipalityId();
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Request: Decided at is missing in appealed decision with id '456' in namespace:'SBK_PARKING_PERMIT' for municipality with id:'2281'");
		verifyNoInteractions(externalTaskServiceMock);
	}

	@Test
	void executeWhenAnAppealAndNoRelatedErrand() {

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_APPEAL);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getRelatesTo()).thenReturn(emptyList());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock).getCaseType();
		verify(errandMock).getRelatesTo();
		verify(errandMock).getNamespace();
		verify(errandMock).getMunicipalityId();
		verify(relatedErrandMock, never()).getErrandId();
		verify(appealedErrandMock, never()).getDecisions();
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Not Found: Appeal with id '123' in namespace:'SBK_PARKING_PERMIT' for municipality with id:'2281' has no related errand");
		verifyNoInteractions(externalTaskServiceMock);
	}

	@Test
	void executeWhenAnAppealAndRelatedErrandDecisionNotFound() {
		// Setup
		final var relatedErrandId = 456L;

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_APPEAL);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getRelatesTo()).thenReturn(List.of(relatedErrandMock));
		when(errandMock.getApplicationReceived()).thenReturn(OffsetDateTime.now().minusDays(5));

		when(relatedErrandMock.getErrandId()).thenReturn(relatedErrandId);
		when(relatedErrandMock.getRelationReason()).thenReturn("APPEAL");
		when(appealedErrandMock.getId()).thenReturn(relatedErrandId);
		when(appealedErrandMock.getNamespace()).thenReturn(NAMESPACE);
		when(appealedErrandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId)).thenReturn(appealedErrandMock);
		// Final decision is missing
		when(appealedErrandMock.getDecisions()).thenReturn(List.of(new Decision().decisionType(RECOMMENDED)));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock).getCaseType();
		verify(relatedErrandMock).getErrandId();
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, relatedErrandId);
		verify(appealedErrandMock).getDecisions();
		verify(appealedErrandMock).getId();
		verify(appealedErrandMock).getNamespace();
		verify(appealedErrandMock).getMunicipalityId();
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Not Found: Appealed errand with id '456' in namespace:'SBK_PARKING_PERMIT' for municipality with id:'2281' is missing final decision");
		verifyNoInteractions(externalTaskServiceMock);
	}

	@Test
	void executeWhenNotAnAppeal() {
		// Setup
		final var variables = new HashMap<String, Object>();
		variables.put(CAMUNDA_VARIABLE_IS_APPEAL, false);
		variables.put(CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW, false);

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_PARKING_PERMIT);
		when(errandMock.getId()).thenReturn(ERRAND_ID);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock).getCaseType();
		verify(externalTaskServiceMock).complete(externalTaskMock, variables);
		verifyNoInteractions(failureHandlerMock, relatedErrandMock, appealedErrandMock);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var problem = Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");

		// Mock to simulate exception upon patching errand with new phase
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(problem);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(externalTaskServiceMock, never()).complete(any(), any());
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, problem.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
	}
}
