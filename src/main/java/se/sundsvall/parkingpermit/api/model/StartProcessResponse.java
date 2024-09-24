package se.sundsvall.parkingpermit.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Start process response")
public class StartProcessResponse {

	@Schema(description = "Process ID", example = "5", accessMode = READ_ONLY)
	private String processId;

	public StartProcessResponse() {}

	public StartProcessResponse(String processId) {
		this.processId = processId;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(processId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final StartProcessResponse other)) { return false; }
		return Objects.equals(processId, other.processId);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("StartProcessResponse [processId=").append(processId).append("]");
		return builder.toString();
	}
}
