package se.sundsvall.parkingpermit;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.engine.variable.type.ValueType;

public final class Constants {

	public static final String PROCESS_KEY = "process-parking-permit"; // Must match ID of process defined in bpmn schema
	public static final String TENANTID_TEMPLATE = "PARKING_PERMIT"; // Namespace where process is deployed, a.k.a tenant (must match setting in application.yaml)

	public static final String NAMESPACE_REGEXP = "[\\w|\\-]+";
	public static final String NAMESPACE_VALIDATION_MESSAGE = "can only contain A-Z, a-z, 0-9, -, and _";

	public static final String PARTY_ASSET_ORIGIN = "CASEDATA";
	public static final String PARTY_ASSET_TYPE = "PARKINGPERMIT";
	public static final String PARTY_ASSET_DESCRIPTION = "Parkeringstillstånd";
	public static final String PARTY_ASSET_STATUS_ACTIVE = "ACTIVE";

	public static final String CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY = "applicantNotResidentOfMunicipality";
	public static final String CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR = "assignedToAdministrator";
	public static final String CAMUNDA_VARIABLE_CASE_NUMBER = "caseNumber";
	public static final String CAMUNDA_VARIABLE_MUNICIPALITY_ID = "municipalityId";
	public static final String CAMUNDA_VARIABLE_NAMESPACE = "namespace";
	public static final String CAMUNDA_VARIABLE_MESSAGE_ID = "messageId";
	public static final String CAMUNDA_VARIABLE_REQUEST_ID = "requestId";
	public static final String CAMUNDA_VARIABLE_UPDATE_AVAILABLE = "updateAvailable";
	public static final String CAMUNDA_VARIABLE_FINAL_DECISION = "finalDecision";
	public static final String CAMUNDA_VARIABLE_IS_APPROVED = "isApproved";
	public static final String CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE = "ruleEngineResponse";
	public static final String CAMUNDA_VARIABLE_SANITY_CHECK_PASSED = "sanityCheckPassed";
	public static final String CAMUNDA_VARIABLE_PHASE = "phase";
	public static final String CAMUNDA_VARIABLE_PHASE_STATUS = "phaseStatus";
	public static final String CAMUNDA_VARIABLE_PHASE_ACTION = "phaseAction";
	public static final String CAMUNDA_VARIABLE_DISPLAY_PHASE = "displayPhase";
	public static final String CAMUNDA_VARIABLE_CARD_EXISTS = "cardExists";
	public static final String CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE = "timeToSendControlMessage";
	public static final String CAMUNDA_VARIABLE_IS_APPEAL = "isAppeal";
	public static final String CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW = "isInTimelinessReview";
	public static final VariableValueDto TRUE = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(true);
	public static final VariableValueDto FALSE = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false);

	public static final String CASEDATA_PHASE_ACTUALIZATION = "Aktualisering";
	public static final String CASEDATA_PHASE_INVESTIGATION = "Utredning";
	public static final String CASEDATA_PHASE_DECISION = "Beslut";
	public static final String CASEDATA_PHASE_CANCELED = "Canceled";
	public static final String CASEDATA_PHASE_EXECUTION = "Verkställa";
	public static final String CASEDATA_PHASE_FOLLOW_UP = "Uppföljning";

	public static final String CASEDATA_STATUS_CASE_RECEIVED = "Ärende inkommit";
	public static final String CASEDATA_STATUS_AWAITING_COMPLETION = "Väntar på komplettering";
	public static final String CASEDATA_STATUS_COMPLETION_RECEIVED = "Komplettering inkommen";
	public static final String CASEDATA_STATUS_CASE_PROCESS = "Under utredning";
	public static final String CASEDATA_STATUS_CASE_DECIDE = "Under beslut";
	public static final String CASEDATA_STATUS_DECISION_EXECUTED = "Beslut verkställt";
	public static final String CASEDATA_STATUS_CASE_DECIDED = "Beslutad";
	public static final String CASEDATA_STATUS_CASE_FINALIZED = "Ärende avslutat";

	public static final String CASEDATA_KEY_PHASE_STATUS = "process.phaseStatus";
	public static final String CASEDATA_KEY_PHASE_ACTION = "process.phaseAction";
	public static final String CASEDATA_KEY_DISPLAY_PHASE = "process.displayPhase";
	public static final String CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY = "application.applicant.capacity";
	public static final String CASEDATA_KEY_APPLICATION_APPLICANT_SIGNING_ABILITY = "application.applicant.signingAbility";
	public static final String CASEDATA_KEY_LOST_PERMIT_POLICE_REPORT_NUMBER = "application.lostPermit.policeReportNumber";
	public static final String CASEDATA_KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES = "application.renewal.changedCircumstances";
	public static final String CASEDATA_KEY_DISABILITY_CAN_BE_ALONE_WHILE_PARKING = "disability.canBeAloneWhileParking";
	public static final String CASEDATA_KEY_DISABILITY_DURATION = "disability.duration";
	public static final String CASEDATA_KEY_DISABILITY_WALKING_ABILITY = "disability.walkingAbility";
	public static final String CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER = "artefact.permit.number";
	public static final String CASEDATA_KEY_ARTEFACT_LOST_PERMIT_NUMBER = "artefact.lost.permit.number";
	public static final String CASEDATA_KEY_DISABILITY_WALKING_DISTANCE_MAX = "disability.walkingDistance.max";
	public static final String CASEDATA_KEY_ARTEFACT_PERMIT_STATUS = "artefact.permit.status";

	public static final String CASEDATA_ATTACHMENT_CATEGORY_PASSPORT_PHOTO = "PASSPORT_PHOTO";
	public static final String CASEDATA_ATTACHMENT_CATEGORY_MEDICAL_CONFIRMATION = "MEDICAL_CONFIRMATION";
	public static final String CASEDATA_ATTACHMENT_CATEGORY_SIGNATURE = "SIGNATURE";

	public static final String BUSINESS_RULES_KEY_STAKEHOLDERS_APPLICANT_PERSON_ID = "stakeholders.applicant.personid";
	public static final String BUSINESS_RULES_KEY_ATTACHMENT_MEDICAL_CONFIRMATION = "attachment.medicalConfirmation";
	public static final String BUSINESS_RULES_KEY_ATTACHMENT_PASSPORT_PHOTO = "attachment.passportPhoto";
	public static final String BUSINESS_RULES_KEY_ATTACHMENT_SIGNATURE = "attachment.signature";

	public static final String PHASE_ACTION_COMPLETE = "COMPLETE";
	public static final String PHASE_ACTION_AUTOMATIC = "AUTOMATIC";
	public static final String PHASE_ACTION_CANCEL = "CANCEL";
	public static final String PHASE_ACTION_UNKNOWN = "UNKNOWN";
	public static final String PHASE_STATUS_COMPLETED = "COMPLETED";
	public static final String PHASE_STATUS_ONGOING = "ONGOING";
	public static final String PHASE_STATUS_WAITING = "WAITING";
	public static final String PHASE_STATUS_CANCELED = "CANCELED";

	public static final String CASEDATA_PARKING_PERMIT_STATUS_ACTIVE = "Aktivt";
	public static final String CASEDATA_PARKING_PERMIT_STATUS_BLOCKED = "Spärrat";
	public static final String CASEDATA_PARKING_PERMIT_STATUS_EXPIRED = "Utgånget";

	public static final String CATEGORY_BESLUT = "BESLUT";

	public static final String ROLE_APPLICANT = "APPLICANT";
	public static final String ROLE_ADMINISTRATOR = "ADMINISTRATOR";

	public static final String CASE_TYPE_PARKING_PERMIT = "PARKING_PERMIT";
	public static final String CASE_TYPE_PARKING_PERMIT_RENEWAL = "PARKING_PERMIT_RENEWAL";
	public static final String CASE_TYPE_LOST_PARKING_PERMIT = "LOST_PARKING_PERMIT";
	public static final String CASE_TYPE_APPEAL = "APPEAL";

	public static final String CASE_DATA_REASON_APPEAL = "APPEAL";

	public static final String MESSAGING_KEY_FLOW_INSTANCE_ID = "flowInstanceId";

	public static final String SM_STATUS_NEW = "NEW";

	public static final String SM_CATEGORY_URBAN_DEVELOPMENT = "URBAN_DEVELOPMENT";
	public static final String SM_TYPE_PARKING_PERMIT = "PARKING_PERMIT";
	public static final String SM_LABEL_MAILING = "MAILING";
	public static final String SM_LABEL_CARD_MANAGEMENT = "CARD_MANAGEMENT";
	public static final String SM_SUBJECT_MAILING_PARKING_PERMIT = "Utskick av parkeringstillstånd";
	public static final String SM_DESCRIPTION_MAILING_PARKING_PERMIT = "Utskick av bifogat beslut om parkeringstillstånd ska ske via post av kontaktcenter. Gäller ärende: %s";
	public static final String SM_DESCRIPTION_CARD_MANAGEMENT = "Hantering av kortet gällande parkeringstillstånd ska ske av kontaktcenter: %s";
	public static final String SM_SUBJECT_CARD_MANAGEMENT_PARKING_PERMIT = "Korthantering av parkeringstillstånd";
	public static final String SM_ROLE_CONTACT_PERSON = "CONTACT";
	public static final String SM_EXTERNAL_ID_TYPE_PRIVATE = "PRIVATE";
	public static final String SM_CONTACT_CHANNEL_TYPE_EMAIL = "Email";
	public static final String SM_CONTACT_CHANNEL_TYPE_PHONE = "Phone";

	public static final String SM_NAMESPACE_CONTACTANGE = "CONTACTANGE";

	// TODO: Remove this and move to configuration
	public static final String TEMPLATE_IDENTIFIER = "sbk.prh.decision.all.rejection.municipality";

	private Constants() {}
}
