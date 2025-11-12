package se.sundsvall.parkingpermit.integration.casedata;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.casedata.configuration.CaseDataConfiguration.CLIENT_ID;

import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.casedata.Note;
import generated.se.sundsvall.casedata.PageErrand;
import generated.se.sundsvall.casedata.PatchDecision;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.casedata.Status;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.parkingpermit.integration.casedata.configuration.CaseDataConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.casedata.url}", configuration = CaseDataConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface CaseDataClient {

	/**
	 * Updates a decision.
	 *
	 * @param  patchDecision                        for patching decision
	 * @param  errandId                             of case to update
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchNewDecision(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody Decision patchDecision);

	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchDecisionWithId(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@PathVariable Long decisionId,
		@RequestBody PatchDecision patchDecision);

	@DeleteMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> deleteDecision(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@PathVariable Long decisionId);

	/**
	 * Gets an errand by id.
	 *
	 * @param  errandId                             of errand to get
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", produces = APPLICATION_JSON_VALUE)
	Errand getErrandById(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId);

	/**
	 * Gets attachment by errand id
	 *
	 * @param  municipalityId municipality id
	 * @param  namespace      namespace
	 * @param  errandId       errand id
	 * @return                list of attachments for errand
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/attachments", produces = APPLICATION_JSON_VALUE)
	List<Attachment> getErrandAttachments(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId);

	/**
	 * Get errands with or without query. The query is very flexible and allows you as a client to control a lot yourself.
	 * Unfortunately you are not able to use the filter with extraParameter-fields.
	 * <p>
	 * filter example: caseType:'LOST_PARKING_PERMIT' and stakeholders.personId:'744e719d-aedc-45b8-b9a6-1ada0e087910'
	 *
	 * @param  filter                               the filter to use
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands", produces = APPLICATION_JSON_VALUE)
	PageErrand getErrandsByQueryFilter(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@RequestParam String filter);

	/**
	 * Updates an errand.
	 *
	 * @param  patchErrand                          for patching errand
	 * @param  errandId                             of errand to update
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchErrand(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody PatchErrand patchErrand);

	/**
	 * Adds a new stakeholder to an errand.
	 *
	 * @param  errandId                             of errand to update
	 * @param  stakeholder                          the stakeholder to add to the errand
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/stakeholders", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> addStakeholderToErrand(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody Stakeholder stakeholder);

	/**
	 * Get stakeholder matching sent in id.
	 *
	 * @param  stakeholderId                        of stakeholder to fetch
	 * @return                                      Stakeholder containing information of the requested stakeholder
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/stakeholders/{stakeholderId}", produces = APPLICATION_JSON_VALUE)
	Stakeholder getStakeholder(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@PathVariable Long stakeholderId);

	@PatchMapping("/{municipalityId}/{namespace}/errands/{errandId}/status")
	ResponseEntity<Void> patchStatus(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody Status status);

	/**
	 * Add a message to an errand.
	 *
	 * @param  messageRequest                       containing information for message to add
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/messages", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> addMessage(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody MessageRequest messageRequest);

	/**
	 * Gets notes by errand id.
	 *
	 * @param  errandId                             of errand containing notes to get
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notes", produces = APPLICATION_JSON_VALUE)
	List<Note> getNotesByErrandId(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestParam(required = false) String noteType);

	/**
	 * Create and add note.
	 *
	 * @param  note                                 note to add
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notes", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> addNoteToErrand(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody Note note);

	/**
	 * Delete note by note id.
	 *
	 * @param  noteId                               of note to delete
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@DeleteMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notes/{noteId}")
	ResponseEntity<Void> deleteNoteById(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@PathVariable Long noteId);

	/**
	 * Adds new extra parameters to errand or updates value of existing ones.
	 *
	 * @param  municipalityId                       municipality id of the municipality that owns the errand
	 * @param  namespace                            namespace in which the errand resides
	 * @param  errandId                             id of the errand to update
	 * @param  extraParameters                      list of extra parameters to add or update errand with
	 * @throws org.zalando.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/extraparameters", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	List<ExtraParameter> patchErrandExtraParameters(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody List<ExtraParameter> extraParameters);
}
