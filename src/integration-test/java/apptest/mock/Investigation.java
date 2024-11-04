package apptest.mock;

import java.util.Map;

import static apptest.mock.api.BusinessRules.mockBusinessRulesPost;
import static apptest.mock.api.CaseData.mockCaseDataDecisionPatch;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Investigation {

    public static String mockInvestigation(String caseId, String scenarioName) {
        var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, "actualization_check-phase-action_task-worker---api-casedata-patch-errand");
        var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
        var scenarioAfterSanityChecks = mockInvestigationSanityChecks(caseId, scenarioName, scenarioAfterUpdateStatus);
        var scenarioAfterExecuteRules = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterSanityChecks);
        var scenarioAfterConstructDecision = mockInvestigationConstructDecision(caseId, scenarioName, scenarioAfterExecuteRules);
        return mockInvestigationCheckPhaseAction(caseId, scenarioName, scenarioAfterConstructDecision);
    }

    public static String mockInvestigationUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "investigation_update-phase-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Aktualisering",
                        "phaseStatusParameter", "COMPLETE",
                        "phaseActionParameter", "COMPLETED",
                        "displayPhaseParameter", "Aktualisering"));

        return mockCaseDataPatch(caseId, scenarioName, state,
                "investigation_update-phase-task-worker---api-casedata-patch-errand",
                equalToJson("""
                            {
                                "externalCaseId": "2971",
                                "phase": "Utredning",
                                "extraParameters" : [ {
                                    "key" : "process.phaseStatus",
                                    "values" : [ "ONGOING" ]
                                }, {
                                    "key" : "process.phaseAction",
                                    "values" : [ "UNKNOWN" ]
                                }, {
                                    "key" : "process.displayPhase",
                                    "values" : [ "Utredning" ]
                                } ]
                            }
                            """));
    }

    public static String mockInvestigationUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "investigation_update-status-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "statusTypeParameter", "Ärende inkommit",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));

        return mockCaseDataPutStatus(caseId, scenarioName, state,
                "investigation_update-status-task-worker---api-casedata-put-status",
                equalToJson("""
                        [
                          {
                            "statusType": "Under utredning",
                            "description": "Ärendet utreds",
                            "dateTime": "${json-unit.any-string}"
                          }
                        ]
                        """));
    }

    public static String mockInvestigationSanityChecks(String caseId, String scenarioName, String requiredScenarioState) {
        return mockInvestigationSanityChecks(caseId, scenarioName, requiredScenarioState, null);
    }

    public static String mockInvestigationSanityChecks(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix) {
        var newScenarioState = "investigation_sanity-checks-task-worker---api-casedata-get-errand";
        if(newScenarioStateSuffix != null) {
            newScenarioState = newScenarioState.concat(newScenarioStateSuffix);
        }
        return mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioState,
                Map.of("decisionTypeParameter", "FINAL",
                        "caseTypeParameter", "PARKING_PERMIT",
                        "statusTypeParameter", "Ärende inkommit",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));
    }

    public static String mockInvestigationExecuteRules(String caseId, String scenarioName, String requiredScenarioState) {
        return mockInvestigationExecuteRules(caseId, scenarioName, requiredScenarioState, null, true);
    }


    public static String mockInvestigationExecuteRules(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix, boolean validResponse) {
        var newScenarioStateGet = "investigation_execute-rules-task-worker---api-casedata-get-errand";
        if(newScenarioStateSuffix != null) {
            newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
        }
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
                Map.of("decisionTypeParameter", "FINAL",
                        "caseTypeParameter", "PARKING_PERMIT",
                        "statusTypeParameter", "Ärende inkommit",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));

        var newScenarioStatePost = "investigation_execute-rules-task-worker---api-businessrules-engine";
        if(newScenarioStateSuffix != null) {
            newScenarioStatePost = newScenarioStatePost.concat(newScenarioStateSuffix);
        }
        return mockBusinessRulesPost(scenarioName, state, newScenarioStatePost,
                equalToJson("""
                        {
                            "context": "PARKING_PERMIT",
                            "facts": [
                                {
                                    "key": "type",
                                    "value": "PARKING_PERMIT"
                                },
                                {
                                    "key": "stakeholders.applicant.personid",
                                    "value": "6b8928bb-9800-4d52-a9fa-20d88c81f1d6"
                                },
                                {
                                    "key": "application.applicant.capacity",
                                    "value": "DRIVER"
                                },
                                {
                                    "key": "disability.duration",
                                    "value": "P6M"
                                },
                                {
                                    "key": "disability.walkingAbility",
                                    "value": "false"
                                }
                            ]
                        }
                        """),
        Map.of(),
        validResponse);
    }

    public static String mockInvestigationConstructDecision(String caseId, String scenarioName, String requiredScenarioState) {
        return mockInvestigationConstructDecision(caseId, scenarioName, requiredScenarioState, null);
    }

    public static String mockInvestigationConstructDecision(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix) {
        var newScenarioStateGet = "construct-recommended-decision-task-worker---api-casedata-get-errand";
        if(newScenarioStateSuffix != null) {
            newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
        }
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));

        var newScenarioStatePatch = "investigation_construct-recommended-decision_task-worker---api-casedata-patch-decision";
        if(newScenarioStateSuffix != null) {
            newScenarioStatePatch = newScenarioStatePatch.concat(newScenarioStateSuffix);
        }
        return mockCaseDataDecisionPatch(caseId, scenarioName, state, newScenarioStatePatch,
                equalToJson("""
                        {
                            "version": 2,
                            "created": "${json-unit.any-string}",
                            "decisionType": "RECOMMENDED",
                            "decisionOutcome": "APPROVAL",
                            "description": "Rekommenderat beslut är bevilja. Den sökande är helt rullstolsburen, funktionsnedsättningens varaktighet är 6 månader eller längre och den sökande har inga aktiva parkeringstillstånd.",
                            "law": [],
                            "attachments": [],
                            "extraParameters": {}
                        }
                        """));
    }

    public static String mockInvestigationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState) {
        return mockInvestigationCheckPhaseAction(caseId, scenarioName, requiredScenarioState, null);
    }

    public static String mockInvestigationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix) {
        var newScenarioStateGet = "investigation_check-phase-action_task-worker---api-casedata-get-errand";
        if(newScenarioStateSuffix != null) {
            newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
        }
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "COMPLETE",
                        "displayPhaseParameter", "Utredning"));

        var newScenarioStatePatch = "investigation_check-phase-action_task-worker---api-casedata-patch-errand";
        if(newScenarioStateSuffix != null) {
            newScenarioStatePatch = newScenarioStatePatch.concat(newScenarioStateSuffix);
        }
        return mockCaseDataPatch(caseId, scenarioName, state, newScenarioStatePatch,
                equalToJson("""
                            {
                                "externalCaseId": "2971",
                                "phase": "Utredning",
                                "extraParameters" : [ {
                                    "key" : "process.phaseStatus",
                                    "values" : [ "COMPLETED" ]
                                }, {
                                    "key" : "process.phaseAction",
                                    "values" : [ "COMPLETE" ]
                                }, {
                                    "key" : "process.displayPhase",
                                    "values" : [ "Utredning" ]
                                } ]
                            }
                            """));
    }
}
