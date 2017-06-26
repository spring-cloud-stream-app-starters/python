/*
 * Copyright 2017 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.springframework.cloud.stream.app.python.local.processor;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.python.jython.JythonScriptExecutor;
import org.springframework.cloud.stream.app.python.shell.PythonAppDeployer;
import org.springframework.cloud.stream.app.python.shell.PythonGitAppDeployerConfiguration;
import org.springframework.cloud.stream.app.python.shell.PythonShellCommandConfiguration;
import org.springframework.cloud.stream.app.python.shell.TcpProperties;
import org.springframework.cloud.stream.app.python.wrapper.JythonWrapperConfiguration;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.shell.ShellCommand;
import org.springframework.context.annotation.Import;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSocketFactorySupport;
import org.springframework.integration.ip.tcp.connection.TcpSocketFactorySupport;

import java.io.IOException;

/**
 * A Processor that forks a shell to run a Python app configured as processor, sending and receiving messages via
 * stdin/stdout. Optionally this may use a Jython wrapper script to transform data to and from the remote app. If no
 * wrapper is configured, the payload must be String.
 *
 * @author David Turanski
 **/

@Import({ PythonShellCommandConfiguration.class, JythonWrapperConfiguration.class,
		PythonGitAppDeployerConfiguration.class })
public class PythonLocalProcessorConfiguration implements InitializingBean {

	@Autowired(required = false)
	private PythonAppDeployer pythonAppDeployer;

	@Autowired
	private ShellCommand shellCommand;


	@Autowired
	@Qualifier("monitorAdapter")
	TcpReceivingChannelAdapter monitorAdapter;

	@Autowired
	private TcpProperties tcpProperties;

	@Autowired(required = false)
	//TODO: Implement this
	private JythonScriptExecutor jythonWrapper;

	@Override
	public void afterPropertiesSet() throws Exception {

		if (pythonAppDeployer != null) {
			pythonAppDeployer.deploy();
		}
		shellCommand.executeAsync();
		if (checkTcpConnection(tcpProperties.getMonitorPort())) {
			monitorAdapter.start();
		}
		else {
			throw new RuntimeException("Unable to connect to shell process " + shellCommand.getCommand());
		}
		if (checkTcpConnection(tcpProperties.getPort())) {
			//tcpAdapter.start();
		}
		else {
			throw new RuntimeException("Unable to connect to shell process " + shellCommand.getCommand());
		}

	}

	private boolean checkTcpConnection(int port) {
		int max_tries = 3;

		TcpSocketFactorySupport socketFactorySupport = new DefaultTcpNetSocketFactorySupport();
		int tries = 0;
		while (tries++ < max_tries) {
			try {
				socketFactorySupport.getSocketFactory().createSocket("localhost", port);
				return true;
			}
			catch (IOException e) {
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e1) {
					Thread.interrupted();
				}
			}
		}
		return false;
	}

}