package se.sundsvall.parkingpermit.integration.camunda.deployment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.camunda.deployment.DeploymentProperties.ProcessArchive;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

	@Autowired
	private CamundaClient camundaClient;

	@Autowired
	private DeploymentProperties deployments;

	@Autowired
	private ResourcePatternResolver patternResolver;

	@Value("${spring.application.name:spring-app}")
	private String applicationName;

	@PostConstruct
	public void deployCamundaResources() {
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

		for (Resource camundaResource : resourcesToDeploy) {
			try {
				// We have to create a tmpFile because we need to read the files via InputStream to work also in a jar-packed
				// environment but the OpenAPI will need a File. We still have to set the file ending correct in the temp file
				// (because otherwise the deployer will not pick it up as e.g. BPMN file)
				String tmpDirectoryName = FileUtils.getTempDirectory().getAbsolutePath();
				String filename = getResourceFilename(camundaResource, type);
				final File tmpFile = new File(tmpDirectoryName + File.separator + filename);
				tmpFile.deleteOnExit();
				try (FileOutputStream out = new FileOutputStream(tmpFile)) {
					IOUtils.copy(camundaResource.getInputStream(), out);
				}

				camundaClient.deploy(
					processArchive.tenant(), // tenantId
					camundaResource.getFilename(),
					true, // changedOnly
					true, // duplicateFiltering
					processArchive.name() + " (" + processArchive.tenant() + ") - " + camundaResource.getFilename(), // deploymentName
					null,
					tmpFile);
			} catch (Exception e) {
				throw new DeploymentException(e);
			}
		}
	}

	private List<Resource> getResources(String path) {
		try {
			return Arrays.asList(ofNullable(patternResolver.getResources(path)).orElse(NO_RESOURCES));
		} catch (IOException e) {
			throw new DeploymentException(e);
		}
	}

	private String getResourceFilename(Resource camundaResource, String type) throws IOException {
		if (camundaResource.getFilename() != null) {
			return camundaResource.getFilename();
		} else {
			return md5DigestAsHex(camundaResource.getInputStream()) + '.' + type;
		}
	}
}
