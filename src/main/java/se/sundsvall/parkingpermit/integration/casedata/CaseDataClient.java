package se.sundsvall.parkingpermit.integration.casedata;

import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.casedata.NoteDTO;
import generated.se.sundsvall.casedata.PageErrandDTO;
import generated.se.sundsvall.casedata.PatchDecisionDTO;
import generated.se.sundsvall.casedata.PatchErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.parkingpermit.integration.casedata.configuration.CaseDataConfiguration;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.casedata.configuration.CaseDataConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.casedata.url}", configuration = CaseDataConfiguration.class)
public interface CaseDataClient {

	/**
	 * Updates a decision.
	 * 
	 * @param patchDecisionDTO for patching decision
	 * @param errandId           of case to update
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/errands/{errandId}/decisions", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchNewDecision(@PathVariable("errandId") Long errandId, @RequestBody DecisionDTO patchDecisionDTO);

	@PatchMapping(path = "/decisions/{decisionId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchDecisionWithId(@PathVariable("decisionId") Long decisionId, @RequestBody PatchDecisionDTO patchDecisionDTO);

	@DeleteMapping(path = "/errands/{errandId}/decisions/{decisionId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> deleteDecision(@PathVariable("errandId") Long errandId, @PathVariable("decisionId") Long decisionId);

	/**
	 * Gets an errand by id.
	 *
	 * @param errandId of errand to get
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/errands/{errandId}", produces = APPLICATION_JSON_VALUE)
	ErrandDTO getErrandById(@PathVariable(name = "errandId") Long errandId);

	/**
	 * Get errands with or without query.
	 * The query is very flexible and allows you as a client to control a lot yourself.
	 * Unfortunately you are not able to use the filter with extraParameter-fields.
	 * 
	 * filter example:
	 * caseType:'LOST_PARKING_PERMIT' and stakeholders.personId:'744e719d-aedc-45b8-b9a6-1ada0e087910'
	 *
	 * @param filter the filter to use
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/errands", produces = APPLICATION_JSON_VALUE)
	PageErrandDTO getErrandsByQueryFilter(@RequestParam(name = "filter") String filter);

	/**
	 * Updates an errand.
	 *
	 * @param patchErrandDTO for patching errand
	 * @param errandId       of errand to update
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/errands/{errandId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchErrand(@PathVariable(name = "errandId") Long errandId, @RequestBody PatchErrandDTO patchErrandDTO);

	/**
	 * Adds a new stakeholder to an errand.
	 *
	 * @param errandId       of errand to update
	 * @param stakeholderDTO the stakeholder to add to the errand
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/errands/{errandId}/stakeholders", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> addStakeholderToErrand(@PathVariable(name = "errandId") Long errandId, @RequestBody StakeholderDTO stakeholderDTO);

	/**
	 * Get stakeholder matching sent in id.
	 *
	 * @param stakeholderId of stakeholder to fetch
	 * @return StakeholderDTO containing information of the requested stakeholder
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/stakeholders/{stakeholderId}", produces = APPLICATION_JSON_VALUE)
	StakeholderDTO getStakeholder(@PathVariable(name = "stakeholderId") Long stakeholderId);

	@PutMapping("/errands/{errandId}/statuses")
	ResponseEntity<Void> putStatus(@PathVariable(name = "errandId") Long errandId, @RequestBody List<StatusDTO> statusDTOList);

	/**
	 * Add a message to an errand.
	 * 
	 * @param messageRequest containing information for message to add
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PostMapping(path = "/messages", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> addMessage(@RequestBody MessageRequest messageRequest);

	/**
	 * Gets notes by errand id.
	 *
	 * @param errandId of errand containing notes to get
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/notes/errand/{errandId}", produces = APPLICATION_JSON_VALUE)
	List<NoteDTO> getNotesByErrandId(@PathVariable(name = "errandId") Long errandId, @RequestParam(name = "noteType", required = false) String noteType);

	/**
	 * Delete note by note id.
	 *
	 * @param noteId of note to delete
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@DeleteMapping(path = "/notes/{noteId}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> deleteNoteById(@PathVariable(name = "noteId") Long noteId);
}
