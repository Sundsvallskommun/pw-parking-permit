package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.camunda.VariableValueDto;
import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.engine.EngineClient;

import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.FALSE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_UPDATE_AVAILABLE;

public abstract class AbstractTaskWorker implements ExternalTaskHandler {

	private final Logger logger;

	private final EngineClient engineClient;
	protected final CaseDataClient caseDataClient;
	protected final FailureHandler failureHandler;

	protected AbstractTaskWorker(final EngineClient engineClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {
		this.logger = LoggerFactory.getLogger(getClass());
		this.engineClient = engineClient;
		this.caseDataClient = caseDataClient;
		this.failureHandler = failureHandler;
	}

	protected void clearUpdateAvailable(final ExternalTask externalTask) {
		/*
		 * Clearing process variable has to be a blocking operation.
		 * Using ExternalTaskService.setVariables() will not work without creating race conditions.
		 */
		engineClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), PROCESS_VARIABLE_UPDATE_AVAILABLE, FALSE);
	}

	protected void setProcessInstanceVariable(final ExternalTask externalTask, final String variableName, final VariableValueDto variableValue) {
		engineClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), variableName, variableValue);
	}

	protected Errand getErrand(final String municipalityId, final String namespace, final Long caseNumber) {
		return caseDataClient.getErrandById(municipalityId, namespace, caseNumber);
	}

	protected List<Attachment> getErrandAttachments(final String municipalityId, final String namespace, final Long caseNumber) {
		return caseDataClient.getErrandAttachments(municipalityId, namespace, caseNumber);
	}

	protected void logInfo(final String msg, final Object... arguments) {
		logger.info(msg, arguments);
	}

	protected void logException(final ExternalTask externalTask, final Exception exception) {
		logger.error("Exception occurred in {} for task with id {} and businesskey {}", this.getClass().getSimpleName(), externalTask.getId(), externalTask.getBusinessKey(), exception);
	}

	protected abstract void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService);

	@Override
	public void execute(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		/*
		 * RequestId.init() only writes to the MDC when the thread local counter is zero and increments it afterwards.
		 * Without a matching reset() the counter never returns to zero, which would make every task after the first one on
		 * a given worker thread log under the request id of that first task.
		 */
		RequestId.init(externalTask.getVariable(PROCESS_VARIABLE_REQUEST_ID));
		try {
			executeBusinessLogic(externalTask, externalTaskService);
		} finally {
			RequestId.reset();
		}
	}

	protected boolean isCancel(final Errand errand) {
		return findExtraParameterValue(errand, CASEDATA_KEY_PHASE_ACTION)
			.filter(PHASE_ACTION_CANCEL::equals)
			.isPresent();
	}

	protected boolean isAutomatic(final Errand errand) {
		return findExtraParameterValue(errand, CASEDATA_KEY_PHASE_ACTION)
			.filter(PHASE_ACTION_AUTOMATIC::equals)
			.isPresent();
	}

	protected String getMunicipalityId(final ExternalTask externalTask) {
		return externalTask.getVariable(PROCESS_VARIABLE_MUNICIPALITY_ID);
	}

	protected String getNamespace(final ExternalTask externalTask) {
		return externalTask.getVariable(PROCESS_VARIABLE_NAMESPACE);
	}

	protected Long getCaseNumber(final ExternalTask externalTask) {
		return externalTask.getVariable(PROCESS_VARIABLE_CASE_NUMBER);
	}

	protected Decision getFinalDecision(final Errand errand) {
		return ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
			.filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst()
			.orElse(null);
	}

	protected String getPhaseAction(final Errand errand) {
		return findExtraParameterValue(errand, CASEDATA_KEY_PHASE_ACTION)
			.orElse(PHASE_ACTION_UNKNOWN);
	}

	/**
	 * Returns the first value of the extra parameter matching the provided key, or an empty optional if the parameter is
	 * absent or holds no values.
	 */
	protected Optional<String> findExtraParameterValue(final Errand errand, final String key) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> key.equals(extraParameter.getKey()))
			.findFirst()
			.map(ExtraParameter::getValues)
			.filter(CollectionUtils::isNotEmpty)
			.map(List::getFirst);
	}
}
