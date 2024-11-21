package se.sundsvall.parkingpermit.util;

import org.springframework.stereotype.Component;

@Component
public class TextProvider {

	private final ApprovalTextProperties approvalTexts;
	private final DenialTextProperties denialTexts;
	private final CommonTextProperties commonTexts;
	private final SimplifiedServiceTextProperties simplifiedServiceTexts;

	TextProvider(ApprovalTextProperties approvalTexts, DenialTextProperties denialTexts, CommonTextProperties commonTexts, SimplifiedServiceTextProperties simplifiedServiceTexts) {
		this.approvalTexts = approvalTexts;
		this.denialTexts = denialTexts;
		this.commonTexts = commonTexts;
		this.simplifiedServiceTexts = simplifiedServiceTexts;
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

	public SimplifiedServiceTextProperties getSimplifiedServiceTexts() {
		return simplifiedServiceTexts;
	}
}
