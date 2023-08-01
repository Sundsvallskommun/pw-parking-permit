# PwParkingPermit

<p>ParkingPermit is a service which integrates with Camunda process engine for starting and updating processes regarding parking permits. It also support processes with business logic and integration to other services.</p>

<h3>Automatic deployment</h3>

<p>The automatic deployment interprets properties present in the application yaml file. The following settings are used to configure the automatic deployment mechanism:</p>

<table class="settings">
	<thead>
		<tr>
			<th>Setting</th>
			<th>Description</th>
			<th>Default&nbsp;value</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="code">camunda.bpm.client.base-url</td>
			<td>URL address to Camunda instance rest engine to use</td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">camunda.bpm.deployment</td>
			<td>The node contains information about the processes that shall be deployed</td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">camunda.bpm.deployment.autoDeployEnabled</td>
			<td>When set to <strong>false</strong> then autodeploy is disabled</td>
			<td><strong>true</strong></td>
		</tr>
		<tr>
			<td class="code">camunda.bpm.deployment.processes</td>
			<td>When deployment node is present, the processes node should contain a list<br />
			of one or more processes to deploy (in one or more tenant namespaces)</td>
			<td><strong>emtpy list</strong></td>
		</tr>
	</tbody>
</table>

<p>The following attributes are possible to configure for each entry in the list of processes:</p>

<table class="settings">
	<thead>
		<tr>
			<th>Setting</th>
			<th>Description</th>
			<th>Default&nbsp;value</th>
		</tr>
		<tr>
			<td class="code">name</td>
			<td>Human readable name of the process, must not be null or empty</td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">tenant</td>
			<td>
				The tenant id that owns the process which will affect in which namespace the process will be deployed.<br />
				If no id is present, the process will be deployed to the default namespace (making it a shared process, usable<br />
				for all tenants in Camunda)
			</td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">bpmnResourcePattern</td>
			<td>
				Pattern to match when searching for bpmn resources in the service.<br />
				For example&nbsp;<span class="code">classpath*:processmodels/*.bpmn</span>
			</td>
			<td><strong>classpath*:**/*.bpmn</strong></td>
		</tr>
		<tr>
			<td class="code">dmnResourcePattern</td>
			<td>
				Pattern to match when searching for dmn resources in the service.<br />
				For example&nbsp;<span class="code">classpath*:processmodels/*.dmn</span>
			</td>
			<td><strong>classpath*:**/*.dmn</strong></td>
		</tr>
		<tr>
			<td class="code">formResourcePattern</td>
			<td>
				Pattern to match when searching for form resources in the service.<br />
				For example&nbsp;<span class="code">classpath*:processmodels/*.form</span>
			</td>
			<td><strong>classpath*:**/*.form</strong></td>
		</tr>
	</thead>
</table>

<p>Below is an example definition for a single process for tenant id "my_namespace" with defined process models in the awesome directory:</p>

<table class="settings">
	<tbody>
		<tr>
			<th>
				Example
			</th>
		</tr>
		<tr>
			<td class="code">
			<span class="code">
				&nbsp; bpm:<br />
				&nbsp; &nbsp; client:<br />
				&nbsp; &nbsp; &nbsp; base-url: http://localhost:8080/engine-rest<br />
				&nbsp; &nbsp; deployment:<br />
				&nbsp; &nbsp; &nbsp; processes:<br />
				&nbsp; &nbsp; &nbsp; &nbsp; - name: My awesome process<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; tenant: my_namespace<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; bpmnResourcePattern: classpath*:processmodels/awesome/*.bpmn<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; dmnResourcePattern: classpath*:processmodels/awesome/*.dmn<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; formResourcePattern: classpath*:processmodels/awesome/*.form
			</span>
			</td>
		</tr>
	</tbody>
</table>

## Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-parking-permit&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-parking-permit)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-parking-permit&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-parking-permit)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-parking-permit&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-parking-permit)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-parking-permit&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-parking-permit)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-parking-permit&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-parking-permit)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-parking-permit&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-parking-permit)

## 
Copyright (c) 2021 Sundsvalls kommun