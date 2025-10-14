package se.sundsvall.parkingpermit.util;

public class DenialTextProperties {

	private String message;
	private String subject;
	private String htmlBody;
	private String plainBody;
	private String description;
	private String templateId;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	public void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}

	public String getPlainBody() {
		return plainBody;
	}

	public void setPlainBody(String plainBody) {
		this.plainBody = plainBody;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}
}
