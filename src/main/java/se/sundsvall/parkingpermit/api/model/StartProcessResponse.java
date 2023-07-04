package se.sundsvall.parkingpermit.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		StartProcessResponse that = (StartProcessResponse) o;
		return Objects.equals(processId, that.processId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(processId);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("StartProcessResponse{");
		sb.append("processId='").append(processId).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
