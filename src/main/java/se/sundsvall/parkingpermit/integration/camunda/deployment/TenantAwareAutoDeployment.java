package se.sundsvall.parkingpermit.integration.camunda.deployment;

import feign.form.FormData;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import se.sundsvall.parkingpermit.integration.camunda.deployment.DeploymentProperties.ProcessArchive;
import se.sundsvall.parkingpermit.integration.engine.EngineClient;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.util.DigestUtils.md5DigestAsHex;
import static se.sundsvall.dept44.util.ResourceUtils.requireNotBlank;

@Configuration
public class TenantAwareAutoDeployment {

	private static final String DEFAULT_PATTERN_PREFIX = "classpath*:**/*.";
	private static final String FILETYPE_BPMN = "bpmn";
	private static final String FILETYPE_DMN = "dmn";
	private static final String FILETYPE_FORM = "form";
	private static final Resource[] NO_RESOURCES = {};
	private static final String DEPLOYMENT_CONTENT_TYPE = "application/octet-stream";

	private final EngineClient engineClient;

	private final DeploymentProperties deployments;

	private final ResourcePatternResolver patternResolver;

	TenantAwareAutoDeployment(EngineClient engineClient, DeploymentProperties deployments, ResourcePatternResolver patternResolver) {
		this.engineClient = engineClient;
		this.deployments = deployments;
		this.patternResolver = patternResolver;
	}

	@PostConstruct
	public void deployProcessResources() {
		if (isNull(deployments) || !deployments.isAutoDeployEnabled()) {
			return;
		}

		ofNullable(deployments.getProcesses()).orElse(emptyList()).forEach(processArchive -> {
			deployResources(processArchive, getResources(isNull(processArchive.bpmnResourcePattern()) ? DEFAULT_PATTERN_PREFIX + FILETYPE_BPMN : processArchive.bpmnResourcePattern()), FILETYPE_BPMN);
			deployResources(processArchive, getResources(isNull(processArchive.dmnResourcePattern()) ? DEFAULT_PATTERN_PREFIX + FILETYPE_DMN : processArchive.dmnResourcePattern()), FILETYPE_DMN);
			deployResources(processArchive, getResources(isNull(processArchive.formResourcePattern()) ? DEFAULT_PATTERN_PREFIX + FILETYPE_FORM : processArchive.formResourcePattern()), FILETYPE_FORM);
		});
	}

	private void deployResources(ProcessArchive processArchive, List<Resource> resourcesToDeploy, String type) {
		// Validate that name is present
		requireNotBlank(processArchive.name(), "Processname must be set");

		for (final Resource camundaResource : resourcesToDeploy) {
			try {
				/*
				 * The resource is read through an InputStream so that deployment also works from a jar-packed environment, and
				 * is handed to the client as in memory form data. The file name has to carry the correct extension, since that
				 * is what the deployer uses to recognize the resource as e.g. a BPMN file.
				 */
				final var content = readContent(camundaResource);

				engineClient.deploy(
					processArchive.tenant(), // tenantId
					camundaResource.getFilename(),
					true, // changedOnly
					true, // duplicateFiltering
					processArchive.name() + " (" + processArchive.tenant() + ") - " + camundaResource.getFilename(), // deploymentName
					null,
					new FormData(DEPLOYMENT_CONTENT_TYPE, getResourceFilename(camundaResource, type), content));
			} catch (final Exception e) {
				throw new DeploymentException(e);
			}
		}
	}

	private byte[] readContent(Resource camundaResource) throws IOException {
		try (var inputStream = camundaResource.getInputStream()) {
			return inputStream.readAllBytes();
		}
	}

	private List<Resource> getResources(String path) {
		try {
			return Arrays.asList(ofNullable(patternResolver.getResources(path)).orElse(NO_RESOURCES));
		} catch (final IOException e) {
			throw new DeploymentException(e);
		}
	}

	private String getResourceFilename(Resource camundaResource, String type) throws IOException {
		if (camundaResource.getFilename() != null) {
			return camundaResource.getFilename();
		}

		return md5DigestAsHex(camundaResource.getInputStream()) + '.' + type;
	}
}
