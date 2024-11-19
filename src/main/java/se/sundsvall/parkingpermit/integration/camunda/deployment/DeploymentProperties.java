package se.sundsvall.parkingpermit.integration.camunda.deployment;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("camunda.bpm.deployment")
public class DeploymentProperties {

	private boolean autoDeployEnabled = true;

	private List<ProcessArchive> processes;

	public boolean isAutoDeployEnabled() {
		return autoDeployEnabled;
	}

	public void setAutoDeployEnabled(boolean autoDeployEnabled) {
		this.autoDeployEnabled = autoDeployEnabled;
	}

	public List<ProcessArchive> getProcesses() {
		return this.processes;
	}

	public void setProcesses(List<ProcessArchive> processes) {
		this.processes = processes;
	}

	public record ProcessArchive(String name, String tenant, String bpmnResourcePattern, String dmnResourcePattern, String formResourcePattern) {
	}
}
