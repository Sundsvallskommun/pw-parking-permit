package se.sundsvall.parkingpermit;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.engine.variable.type.ValueType;

public class Constants {

	private Constants() {}

	public static final String PROCESS_KEY = "process-parking-permit"; // Must match ID of process defined in bpmn schema
	public static final String TENANTID_TEMPLATE = "PARKING_PERMIT"; // Namespace where process is deployed, a.k.a tenant (must match setting in application.yaml)

	public static final String CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY = "applicantNotResidentOfMunicipality";
	public static final String CAMUNDA_VARIABLE_CASE_NUMBER = "caseNumber";
	public static final String CAMUNDA_VARIABLE_MESSAGE_ID = "messageId";
	public static final String CAMUNDA_VARIABLE_REQUEST_ID = "requestId";
	public static final String CAMUNDA_VARIABLE_UPDATE_AVAILABLE = "updateAvailable";
	public static final String CAMUNDA_VARIABLE_FINAL_DECISION = "finalDecision";
	public static final String CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE = "ruleEngineResponse";
	public static final String CAMUNDA_VARIABLE_SANITY_CHECK_PASSED = "sanityCheckPassed";
	public static final String CAMUNDA_VARIABLE_PHASE = "phase";
	public static final String CAMUNDA_VARIABLE_PHASE_STATUS = "phaseStatus";
	public static final String CAMUNDA_VARIABLE_PHASE_ACTION = "phaseAction";
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
	public static final String CASEDATA_KEY_PHASE_STATUS = "process.phaseStatus";
	public static final String CASEDATA_KEY_PHASE_ACTION = "process.phaseAction";
	public static final String CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY = "application.applicant.capacity";
	public static final String CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER = "application.lostPermit.policeReportNumber";
	public static final String CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES = "application.renewal.changedCircumstances";
	public static final String CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING = "disability.canBeAloneWhileParking";
	public static final String CASEDATA_KEY_DISABILITY_DURATION = "disability.duration";
	public static final String CASEDATA_KEY_DISABILITY_WALKING_ABILITY = "disability.walkingAbility";
	public static final String CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX = "disability.walkingDistance.max";
	public static final String BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID = "stakeholders.applicant.personid";
	public static final String PHASE_ACTION_COMPLETE = "COMPLETE";
	public static final String PHASE_ACTION_CANCEL = "CANCEL";
	public static final String PHASE_ACTION_UNKNOWN = "UNKNOWN";
	public static final String PHASE_STATUS_COMPLETED = "COMPLETED";
	public static final String PHASE_STATUS_ONGOING = "ONGOING";
	public static final String PHASE_STATUS_WAITING = "WAITING";
	public static final String PHASE_STATUS_CANCELED = "CANCELED";
}
