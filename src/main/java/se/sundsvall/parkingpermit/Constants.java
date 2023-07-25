package se.sundsvall.parkingpermit;

import org.camunda.bpm.engine.variable.type.ValueType;

import generated.se.sundsvall.camunda.VariableValueDto;

public class Constants {
	private Constants() {}

	public static final String PROCESS_KEY = "process-parking-permit"; // Must match ID of process defined in bpmn schema
	public static final String TENANTID_TEMPLATE = "PARKING_PERMIT"; // Namespace where process is deployed, a.k.a tenant (must match setting in application.yaml)

	public static final String CAMUNDA_VARIABLE_CASE_NUMBER = "caseNumber";
	public static final String CAMUNDA_VARIABLE_MESSAGE_ID = "messageId";
	public static final String CAMUNDA_VARIABLE_UPDATE_AVAILABLE = "updateAvailable";
	public static final VariableValueDto TRUE = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(true);
	public static final VariableValueDto FALSE = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false);

	public static final String CASEDATA_PHASE_ACTUALIZATION = "Aktualisering";
	public static final String CASEDATA_PHASE_INVESTIGATION = "Utreda";
	public static final String CASEDATA_PHASE_DECISION = "Beslut";

	public static final String CASEDATA_STATUS_CASE_RECEIVED = "Ärende inkommit";
	public static final String CASEDATA_STATUS_AWAITING_COMPLETION = "Väntar på komplettering";
	public static final String CASEDATA_STATUS_CASE_DECIDED = "Beslutad";
	public static final String CASEDATA_STATUS_COMPLETION_RECEIVED = "Komplettering inkommen";
	public static final String CASEDATA_STATUS_CASE_PROCESSED = "Under utredning";
	public static final String CASEDATA_STATUS_DECISION_EXECUTED = "Beslut verkställt";
}
