package se.sundsvall.parkingpermit.util;

import org.springframework.stereotype.Component;

@Component
public class TextProvider {

	private final ApprovalTextProperties approvalTexts;
	private final DenialTextProperties denialTexts;
	private final CommonTextProperties commonTexts;

	TextProvider(ApprovalTextProperties approvalTexts, DenialTextProperties denialTexts, CommonTextProperties commonTexts) {
		this.approvalTexts = approvalTexts;
		this.denialTexts = denialTexts;
		this.commonTexts = commonTexts;
	}

	public ApprovalTextProperties getApprovalTexts() {
		return approvalTexts;
	}

	public DenialTextProperties getDenialTexts() {
		return denialTexts;
	}

	public CommonTextProperties getCommonTexts() {
		return commonTexts;
	}
}
