package se.sundsvall.parkingpermit.integration.casedata;

import generated.se.sundsvall.casedata.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.sundsvall.parkingpermit.integration.casedata.configuration.CaseDataConfiguration;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.casedata.configuration.CaseDataConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.casedata.url}", configuration = CaseDataConfiguration.class)
public interface CaseDataClient {

	/**
	 * Updates a decision.
	 *
	 * @param patchDecision for patching decision
	 * @param errandId of case to update
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchNewDecision(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable("errandId") Long errandId,
		@RequestBody Decision patchDecision);

	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchDecisionWithId(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable("errandId") Long errandId,
		@PathVariable("decisionId") Long decisionId,
		@RequestBody PatchDecision patchDecision);

	@DeleteMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> deleteDecision(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable("errandId") Long errandId,
		@PathVariable("decisionId") Long decisionId);

	/**
	 * Gets an errand by id.
	 *
	 * @param errandId of errand to get
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", produces = APPLICATION_JSON_VALUE)
	Errand getErrandById(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable(name = "errandId") Long errandId);

	/**
	 * Get errands with or without query.
	 * The query is very flexible and allows you as a client to control a lot yourself.
	 * Unfortunately you are not able to use the filter with extraParameter-fields.
	 * <p>
	 * filter example:
	 * caseType:'LOST_PARKING_PERMIT' and stakeholders.personId:'744e719d-aedc-45b8-b9a6-1ada0e087910'
	 *
	 * @param filter the filter to use
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands", produces = APPLICATION_JSON_VALUE)
	PageErrand getErrandsByQueryFilter(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@RequestParam(name = "filter") String filter);

	/**
	 * Updates an errand.
	 *
	 * @param patchErrand for patching errand
	 * @param errandId of errand to update
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchErrand(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable(name = "errandId") Long errandId,
		@RequestBody PatchErrand patchErrand);

	/**
	 * Adds a new stakeholder to an errand.
	 *
	 * @param errandId of errand to update
	 * @param stakeholder the stakeholder to add to the errand
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/stakeholders", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> addStakeholderToErrand(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable(name = "errandId") Long errandId,
		@RequestBody Stakeholder stakeholder);

	/**
	 * Get stakeholder matching sent in id.
	 *
	 * @param stakeholderId of stakeholder to fetch
	 * @return Stakeholder containing information of the requested stakeholder
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/stakeholders/{stakeholderId}", produces = APPLICATION_JSON_VALUE)
	Stakeholder getStakeholder(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable("errandId") Long errandId,
		@PathVariable(name = "stakeholderId") Long stakeholderId);

	@PutMapping("/{municipalityId}/{namespace}/errands/{errandId}/statuses")
	ResponseEntity<Void> putStatus(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable(name = "errandId") Long errandId,
		@RequestBody List<Status> statusList);

	/**
	 * Add a message to an errand.
	 *
	 * @param messageRequest containing information for message to add
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/messages", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> addMessage(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable("errandId") Long errandId,
		@RequestBody MessageRequest messageRequest);

	/**
	 * Gets notes by errand id.
	 *
	 * @param errandId of errand containing notes to get
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notes", produces = APPLICATION_JSON_VALUE)
	List<Note> getNotesByErrandId(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable(name = "errandId") Long errandId,
		@RequestParam(name = "noteType", required = false) String noteType);

	/**
	 * Delete note by note id.
	 *
	 * @param noteId of note to delete
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@DeleteMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notes/{noteId}")
	ResponseEntity<Void> deleteNoteById(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "namespace") String namespace,
		@PathVariable(name = "errandId") Long errandId,
		@PathVariable(name = "noteId") Long noteId);
}
