package se.sundsvall.parkingpermit.util;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "texts")
public class TextProperties {

	private Map<String, ApprovalTextProperties> approvals;
	private Map<String, CommonTextProperties> commons;
	private Map<String, DenialTextProperties> denials;
	private Map<String, SimplifiedServiceTextProperties> simplifiedServices;

	public Map<String, ApprovalTextProperties> getApprovals() {
		return approvals;
	}

	public void setApprovals(Map<String, ApprovalTextProperties> approvals) {
		this.approvals = approvals;
	}

	public Map<String, CommonTextProperties> getCommons() {
		return commons;
	}

	public void setCommons(Map<String, CommonTextProperties> commons) {
		this.commons = commons;
	}

	public Map<String, DenialTextProperties> getDenials() {
		return denials;
	}

	public void setDenials(Map<String, DenialTextProperties> denials) {
		this.denials = denials;
	}

	public Map<String, SimplifiedServiceTextProperties> getSimplifiedServices() {
		return simplifiedServices;
	}

	public void setSimplifiedServices(Map<String, SimplifiedServiceTextProperties> simplifiedServices) {
		this.simplifiedServices = simplifiedServices;
	}
}
