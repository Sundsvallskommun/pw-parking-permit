package se.sundsvall.parkingpermit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.CONFLICT;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import generated.se.sundsvall.rpa.QueuesAddQueueItemParameters;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.DefaultProblem;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.integration.rpa.RpaClient;
import se.sundsvall.parkingpermit.integration.rpa.configuration.RpaProperties;

@ExtendWith(MockitoExtension.class)
class RpaServiceTest {
	private static final String FOLDER_ID = "50";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private RpaClient rpaClientMock;

	@Mock
	private RpaProperties rpaPropertiesMock;

	@Captor
	private ArgumentCaptor<QueuesAddQueueItemParameters> queueArgumentCaptor;

	@InjectMocks
	private RpaService rpaService;

	@Test
	void addQueueItems() {
		// Arrange
		final var queues = List.of("Queue-1", "Queue-2", "Queue-3");
		final var id = 123L;
		when(rpaPropertiesMock.folderIds()).thenReturn(Map.of(MUNICIPALITY_ID, FOLDER_ID));

		// Act
		rpaService.addQueueItems(queues, id, MUNICIPALITY_ID);

		// Assert and verify
		verify(rpaClientMock, times(3)).addQueueItem(eq(FOLDER_ID), queueArgumentCaptor.capture());

		final AtomicInteger i = new AtomicInteger(1);
		queueArgumentCaptor.getAllValues()
			.forEach(param -> {
				assertThat(param.getItemData().getName()).isEqualTo("Queue-" + i.getAndIncrement());
				assertThat(param.getItemData().getReference()).isEqualTo(String.valueOf(id));
			});
	}

	@ParameterizedTest
	@NullAndEmptySource
	void addQueueItemsWithNullOrEmptyString(List<String> queues) {
		// Arrange
		final var id = 123L;
		when(rpaPropertiesMock.folderIds()).thenReturn(Map.of(MUNICIPALITY_ID, FOLDER_ID));

		// Act
		rpaService.addQueueItems(queues, id, MUNICIPALITY_ID);

		// Verify
		verifyNoInteractions(rpaClientMock);
	}

	@Test
	void returnConflictErrorWhenAddingQueueItem() {
		// Arrange
		when(rpaClientMock.addQueueItem(eq(FOLDER_ID), queueArgumentCaptor.capture())).thenThrow(Problem.valueOf(CONFLICT, "rpa error: {detail=1016, status=409 Conflict, title=Error creating Transaction. Duplicate Reference."));
		when(rpaPropertiesMock.folderIds()).thenReturn(Map.of(MUNICIPALITY_ID, FOLDER_ID));
		final var queues = List.of("Queue-1");

		// Act
		rpaService.addQueueItems(queues, null, MUNICIPALITY_ID);

		// Verify
		verify(rpaClientMock).addQueueItem(eq(FOLDER_ID), queueArgumentCaptor.capture());
	}

	@Test
	void returnOtherErrorWhenAddingQueueItem() {
		// Arrange
		when(rpaClientMock.addQueueItem(eq(FOLDER_ID), queueArgumentCaptor.capture())).thenThrow(Problem.valueOf(INTERNAL_SERVER_ERROR, "Other error"));
		when(rpaPropertiesMock.folderIds()).thenReturn(Map.of(MUNICIPALITY_ID, FOLDER_ID));
		final var queues = List.of("Queue-1", "Queue-2", "Queue-3");

		// Act
		final var e = assertThrows(DefaultProblem.class, () -> rpaService.addQueueItems(queues, null, MUNICIPALITY_ID));

		// Assert and verify
		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		verify(rpaClientMock).addQueueItem(eq(FOLDER_ID), queueArgumentCaptor.capture());
	}

	@Test
	void addQueueItemsThrowsExceptionWhenNoFolderIdFound() {
		// Arrange
		final var queues = List.of("Queue-1");
		final var id = 123L;
		when(rpaPropertiesMock.folderIds()).thenReturn(Map.of());

		// Act & Assert
		final var exception = assertThrows(DefaultProblem.class, () -> rpaService.addQueueItems(queues, id, MUNICIPALITY_ID));
		assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(exception.getDetail()).isEqualTo("No folder ID found for municipality ID: " + MUNICIPALITY_ID);
	}
}
