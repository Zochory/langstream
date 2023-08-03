/**
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.sga.cli.commands;

import com.datastax.oss.sga.cli.commands.applications.AbstractDeployApplicationCmd;
import com.datastax.oss.sga.cli.commands.applications.DeleteApplicationCmd;
import com.datastax.oss.sga.cli.commands.applications.GetApplicationCmd;
import com.datastax.oss.sga.cli.commands.applications.GetApplicationLogsCmd;
import com.datastax.oss.sga.cli.commands.applications.ListApplicationCmd;
import com.datastax.oss.sga.cli.commands.gateway.ChatGatewayCmd;
import com.datastax.oss.sga.cli.commands.gateway.ConsumeGatewayCmd;
import com.datastax.oss.sga.cli.commands.gateway.ProduceGatewayCmd;
import lombok.Getter;
import picocli.CommandLine;

@CommandLine.Command(name = "gateway", mixinStandardHelpOptions = true, description = "Interact with a application gateway",
        subcommands = {
                ProduceGatewayCmd.class,
                ConsumeGatewayCmd.class,
                ChatGatewayCmd.class
        })
@Getter
public class RootGatewayCmd {
    @CommandLine.ParentCommand
    private RootCmd rootCmd;
}