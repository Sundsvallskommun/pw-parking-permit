package apptest.verification;

import static org.assertj.core.api.Assertions.tuple;

public class ProcessPathway {

    public static Tuples actualizationPathway() {
        return Tuples.create()
                .with(tuple("Actualization", "actualization_phase"))
                .with(tuple("Start actualization phase", "start_actualization_phase"))
                .with(tuple("Update phase", "external_task_actualization_update_phase"))
                .with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
                .with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
                .with(tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"))
                .with(tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"))
                .with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
                .with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
                .with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
                .with(tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"))
                .with(tuple("End actualization phase", "end_actualization_phase"));
    }

    public static Tuples investigationPathway() {
        return Tuples.create()
                .with(tuple("Investigation", "investigation_phase"))
                .with(tuple("Start investigation phase", "start_investigation_phase"))
                .with(tuple("Update phase", "external_task_investigation_update_phase"))
                .with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Execute rules", "external_task_investigation_execute_rules"))
                .with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
                .with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
                .with(tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"))
                .with(tuple("End investigation phase", "end_investigation_phase"));
    }

    public static Tuples decisionPathway() {
        return Tuples.create()
                .with(tuple("Decision", "decision_phase"))
                .with(tuple("Start decision phase", "start_decision_phase"))
                .with(tuple("Update phase on errand", "external_task_decision_update_phase"))
                .with(tuple("Update errand status", "external_task_decision_update_errand_status"))
                .with(tuple("Check if decision is made", "external_task_check_decision_task"))
                .with(tuple("Gateway is decision final", "gateway_is_decision_final"))
                .with(tuple("End decision phase", "end_decision_phase"));
    }

    public static Tuples handlingPathway() {
        return Tuples.create()
                .with(tuple("Handling", "call_activity_handling"))
                .with(tuple("Start handling phase", "start_handling_phase"))
                .with(tuple("End handling phase", "end_handling_phase"));
    }

    public static Tuples executionPathway() {
        return Tuples.create()
                .with(tuple("Execution", "call_activity_execution"))
                .with(tuple("Start execution phase", "start_execution_phase"))
                .with(tuple("Update phase", "external_task_execution_update_phase"))
                .with(tuple("Order card", "external_task_execution_order_card_task"))
                .with(tuple("Check if card exists", "external_task_execution_check_if_card_exists"))
                .with(tuple("Is card manufactured", "gateway_card_exists"))
                .with(tuple("Create Asset", "external_task_execution_create_asset"))
                .with(tuple("End execution phase", "end_execution_phase"));
    }

    public static Tuples followUpPathway() {
        return Tuples.create()
                .with(tuple("Follow up", "call_activity_follow_up"))
                .with(tuple("Start follow up phase", "start_follow_up_phase"))
                .with(tuple("Update phase", "external_task_follow_up_update_phase"))
                .with(tuple("Clean up notes", "external_task_follow_up_clean_up_notes"))
                .with(tuple("End follow up phase", "end_follow_up_phase"));
    }
}
