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
package ai.langstream.pulsar;

import ai.langstream.api.model.Application;
import ai.langstream.api.model.Connection;
import ai.langstream.api.model.Module;
import ai.langstream.api.model.TopicDefinition;
import ai.langstream.api.runtime.AgentNode;
import ai.langstream.api.runtime.ClusterRuntimeRegistry;
import ai.langstream.api.runtime.ExecutionPlan;
import ai.langstream.api.runtime.PluginsRegistry;
import ai.langstream.impl.common.DefaultAgentNode;
import ai.langstream.impl.deploy.ApplicationDeployer;
import ai.langstream.impl.parser.ModelBuilder;
import ai.langstream.pulsar.agents.PulsarAgentNodeMetadata;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class PulsarClusterRuntimeTest {

    @Test
    public void testMapCassandraSink() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(
                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "input-topic-cassandra"
                                    creation-mode: create-if-not-exists
                                    schema:
                                      type: avro                                      
                                      schema: '{"type":"record","namespace":"examples","name":"Product","fields":[{"name":"id","type":"string"},{"name":"name","type":"string"},{"name":"description","type":"string"},{"name":"price","type":"double"},{"name":"category","type":"string"},{"name":"item_vector","type":"bytes"}]}}'
                                pipeline:
                                  - name: "sink1"
                                    id: "sink-1-id"
                                    type: "cassandra-sink"
                                    input: "input-topic-cassandra"
                                    configuration:
                                      mappings: "id=value.id,name=value.name,description=value.description,item_vector=value.item_vector"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);
        assertTrue(implementation.getConnectionImplementation(module,
                Connection.fromTopic(TopicDefinition.fromName("input-topic-cassandra"))) instanceof PulsarTopic);
        PulsarName pulsarName = new PulsarName("public", "default", "input-topic-cassandra");
        assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));

        AgentNode agentImplementation = implementation.getAgentImplementation(module, "sink-1-id");
        assertNotNull(agentImplementation);
        DefaultAgentNode genericSink =
                (DefaultAgentNode) agentImplementation;
        PulsarAgentNodeMetadata pulsarSinkMetadata = genericSink.getCustomMetadata();
        assertEquals("cassandra-enhanced", pulsarSinkMetadata.getAgentType());
        assertEquals(new PulsarName("public", "default", "sink1"), pulsarSinkMetadata.getPulsarName());

    }

    @NotNull
    private static String buildInstanceYaml() {
        return """
                instance:
                  streamingCluster:
                    type: "pulsar"
                    configuration:
                      admin:                                      
                        serviceUrl: "http://localhost:8080"
                      defaultTenant: "public"
                      defaultNamespace: "default"
                  computeCluster:
                    type: "pulsar"
                    configuration:
                      admin:                                      
                        serviceUrl: "http://localhost:8080"
                      defaultTenant: "public"
                      defaultNamespace: "default"
                """;
    }

    @Test
    public void testMapGenericPulsarSink() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(

                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "input-topic"
                                    creation-mode: create-if-not-exists
                                    schema:
                                      type: avro
                                      schema: '{"type":"record","namespace":"examples","name":"Product","fields":[{"name":"id","type":"string"},{"name":"name","type":"string"},{"name":"description","type":"string"},{"name":"price","type":"double"},{"name":"category","type":"string"},{"name":"item_vector","type":"bytes"}]}}'
                                pipeline:
                                  - name: "sink1"
                                    id: "sink-1-id"
                                    type: "sink"
                                    input: "input-topic"
                                    configuration:
                                      sinkType: "some-sink-type-on-your-cluster"
                                      config1: "value"
                                      config2: "value2"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);
        assertTrue(implementation.getConnectionImplementation(module,
                Connection.fromTopic(TopicDefinition.fromName("input-topic"))) instanceof PulsarTopic);
        PulsarName pulsarName = new PulsarName("public", "default", "input-topic");
        assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));

        AgentNode agentImplementation = implementation.getAgentImplementation(module, "sink-1-id");
        assertNotNull(agentImplementation);
        DefaultAgentNode genericSink =
                (DefaultAgentNode) agentImplementation;
        PulsarAgentNodeMetadata pulsarSinkMetadata = genericSink.getCustomMetadata();
        assertEquals("some-sink-type-on-your-cluster", pulsarSinkMetadata.getAgentType());
        assertEquals(new PulsarName("public", "default", "sink1"), pulsarSinkMetadata.getPulsarName());

    }

    @Test
    public void testMapGenericPulsarSource() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(

                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "output-topic"
                                    creation-mode: create-if-not-exists
                                pipeline:
                                  - name: "source1"
                                    id: "source-1-id"
                                    type: "source"
                                    output: "output-topic"
                                    configuration:
                                      sourceType: "some-source-type-on-your-cluster"
                                      config1: "value"
                                      config2: "value2"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);
        assertTrue(implementation.getConnectionImplementation(module,
                Connection.fromTopic(TopicDefinition.fromName("output-topic"))) instanceof PulsarTopic);
        PulsarName pulsarName = new PulsarName("public", "default", "output-topic");
        assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));

        AgentNode agentImplementation = implementation.getAgentImplementation(module, "source-1-id");
        assertNotNull(agentImplementation);
        DefaultAgentNode genericSink =
                (DefaultAgentNode) agentImplementation;
        PulsarAgentNodeMetadata pulsarSourceMetadata = genericSink.getCustomMetadata();
        assertEquals("some-source-type-on-your-cluster", pulsarSourceMetadata.getAgentType());
        assertEquals(new PulsarName("public", "default", "source1"), pulsarSourceMetadata.getPulsarName());

    }


    @Test
    public void testMapGenericPulsarFunction() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(

                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "input-topic"
                                    creation-mode: create-if-not-exists
                                  - name: "output-topic"
                                    creation-mode: create-if-not-exists
                                pipeline:
                                  - name: "function1"
                                    id: "function-1-id"
                                    type: "function"
                                    input: "input-topic"
                                    output: "output-topic"
                                    configuration:
                                      functionType: "some-function-type-on-your-cluster"
                                      functionClassname: "a.b.c.ClassName"
                                      config1: "value"
                                      config2: "value2"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);
        {
            assertTrue(implementation.getConnectionImplementation(module,
                    Connection.fromTopic(TopicDefinition.fromName("input-topic"))) instanceof PulsarTopic);
            PulsarName pulsarName = new PulsarName("public", "default", "input-topic");
            assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));
        }
        {
            assertTrue(implementation.getConnectionImplementation(module,
                    Connection.fromTopic(TopicDefinition.fromName("output-topic"))) instanceof PulsarTopic);
            PulsarName pulsarName = new PulsarName("public", "default", "output-topic");
            assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));
        }

        AgentNode agentImplementation = implementation.getAgentImplementation(module, "function-1-id");
        assertNotNull(agentImplementation);
        DefaultAgentNode genericSink =
                (DefaultAgentNode) agentImplementation;
        PulsarAgentNodeMetadata pulsarSourceMetadata =
                genericSink.getCustomMetadata();
        assertEquals("some-function-type-on-your-cluster", pulsarSourceMetadata.getAgentType());
        assertEquals(new PulsarName("public", "default", "function1"), pulsarSourceMetadata.getPulsarName());

    }


    @Test
    public void testMapGenericPulsarFunctionsChain() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(

                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "input-topic"
                                    creation-mode: create-if-not-exists
                                  - name: "output-topic"
                                    creation-mode: create-if-not-exists
                                pipeline:
                                  - name: "function1"
                                    id: "function-1-id"
                                    type: "function"
                                    input: "input-topic"
                                    # the output is implicitly an intermediate topic                                    
                                    configuration:
                                      functionType: "some-function-type-on-your-cluster"
                                      functionClassname: "a.b.c.ClassName"
                                      config1: "value"
                                      config2: "value2"
                                  - name: "function2"
                                    id: "function-2-id"
                                    type: "function"
                                    # the input is implicitly an intermediate topic                                    
                                    output: "output-topic"
                                    configuration:
                                      functionType: "some-function-type-on-your-cluster"
                                      functionClassname: "a.b.c.ClassName"
                                      config1: "value"
                                      config2: "value2"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);
        {
            assertTrue(implementation.getConnectionImplementation(module,
                    Connection.fromTopic(TopicDefinition.fromName("input-topic"))) instanceof PulsarTopic);
            PulsarName pulsarName = new PulsarName("public", "default", "input-topic");
            assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));
        }
        {
            assertTrue(implementation.getConnectionImplementation(module,
                    Connection.fromTopic(TopicDefinition.fromName("output-topic"))) instanceof PulsarTopic);
            PulsarName pulsarName = new PulsarName("public", "default", "output-topic");
            assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));
        }

        {
            assertTrue(implementation.getConnectionImplementation(module, Connection.fromTopic(
                    TopicDefinition.fromName("agent-function-2-id-input"))) instanceof PulsarTopic);
            PulsarName pulsarName = new PulsarName("public", "default", "agent-function-2-id-input");
            assertTrue(implementation.getTopics().values().stream().anyMatch( t-> ((PulsarTopic) t).name().equals(pulsarName)));
        }


        assertEquals(3, implementation.getTopics().size());

        {
            AgentNode agentImplementation = implementation.getAgentImplementation(module, "function-1-id");
            assertNotNull(agentImplementation);
            DefaultAgentNode genericSink =
                    (DefaultAgentNode) agentImplementation;
            PulsarAgentNodeMetadata pulsarSourceMetadata =
                    genericSink.getCustomMetadata();
            assertEquals("some-function-type-on-your-cluster", pulsarSourceMetadata.getAgentType());
            assertEquals(new PulsarName("public", "default", "function1"), pulsarSourceMetadata.getPulsarName());
        }

        {
            AgentNode agentImplementation = implementation.getAgentImplementation(module, "function-2-id");
            assertNotNull(agentImplementation);
            DefaultAgentNode genericSink =
                    (DefaultAgentNode) agentImplementation;
            PulsarAgentNodeMetadata pulsarSourceMetadata =
                    genericSink.getCustomMetadata();
            assertEquals("some-function-type-on-your-cluster", pulsarSourceMetadata.getAgentType());
            assertEquals(new PulsarName("public", "default", "function2"), pulsarSourceMetadata.getPulsarName());
        }

    }


    @Test
    public void testOpenAIComputeEmbeddingFunction() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(

                        "configuration.yaml",
                        """
                                configuration:  
                                  resources:
                                    - name: open-ai
                                      type: open-ai-configuration
                                      configuration:
                                        url: "http://something"                                
                                        access-key: "xxcxcxc"
                                        provider: "azure"
                                  """,
                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "input-topic"
                                    creation-mode: create-if-not-exists
                                    schema:
                                      type: avro
                                      schema: '{"type":"record","namespace":"examples","name":"Product","fields":[{"name":"id","type":"string"},{"name":"name","type":"string"},{"name":"description","type":"string"},{"name":"price","type":"double"},{"name":"category","type":"string"},{"name":"item_vector","type":"bytes"}]}}'
                                  - name: "output-topic"
                                    creation-mode: create-if-not-exists                                    
                                pipeline:
                                  - name: "compute-embeddings"
                                    id: "step1"
                                    type: "compute-ai-embeddings"
                                    input: "input-topic"
                                    output: "output-topic"
                                    configuration:                                      
                                      model: "text-embedding-ada-002"
                                      embeddings-field: "value.embeddings"
                                      text: "{{% value.name }} {{% value.description }}"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);
        assertTrue(implementation.getConnectionImplementation(module,
                Connection.fromTopic(TopicDefinition.fromName("input-topic"))) instanceof PulsarTopic);
        assertTrue(implementation.getConnectionImplementation(module,
                Connection.fromTopic(TopicDefinition.fromName("output-topic"))) instanceof PulsarTopic);

        AgentNode agentImplementation = implementation.getAgentImplementation(module, "step1");
        // use the standard toolkit
        assertEquals("ai-tools", agentImplementation.getAgentType());
        assertNotNull(agentImplementation);
        DefaultAgentNode step =
                (DefaultAgentNode) agentImplementation;
        Map<String, Object> configuration = step.getConfiguration();
        log.info("Configuration: {}", configuration);
        Map<String, Object> openAIConfiguration = (Map<String, Object>) configuration.get("openai");
        log.info("openAIConfiguration: {}", openAIConfiguration);
        assertEquals("http://something", openAIConfiguration.get("url"));
        assertEquals("xxcxcxc", openAIConfiguration.get("access-key"));
        assertEquals("azure", openAIConfiguration.get("provider"));


        List<Map<String, Object>> steps = (List<Map<String, Object>>) configuration.get("steps");
        assertEquals(1, steps.size());
        Map<String, Object> step1 = steps.get(0);
        assertEquals("text-embedding-ada-002", step1.get("model"));
        assertEquals("value.embeddings", step1.get("embeddings-field"));
        assertEquals("{{ value.name }} {{ value.description }}", step1.get("text"));


    }

    @Test
    public void testSanitizePipelineName() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(

                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "input-topic"
                                    creation-mode: create-if-not-exists
                                pipeline:
                                  - name: "My function name with spaces"
                                    id: "step1"
                                    type: "compute-ai-embeddings"
                                    input: "input-topic"
                                    output: "input-topic"
                                    configuration:                                      
                                      model: "text-embedding-ada-002"
                                      embeddings-field: "value.embeddings"
                                      text: "{{% value.name }} {{% value.description }}"
                                  - name: "My sink name with spaces"
                                    id: "sink1"
                                    type: "sink"
                                    input: "input-topic"
                                    configuration:
                                      sinkType: "some-sink-type-on-your-cluster"
                                      config1: "value"
                                      config2: "value2"
                                  - name: "My source name with spaces"
                                    id: "source1"
                                    type: "source"
                                    output: "input-topic"
                                    configuration:
                                      sourceType: "some-source-type-on-your-cluster"
                                      config1: "value"
                                      config2: "value2"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);
        final DefaultAgentNode functionPhysicalImpl =
                (DefaultAgentNode) implementation.getAgentImplementation(module,
                        "step1");
        assertEquals("my-function-name-with-spaces",
                ((PulsarAgentNodeMetadata) functionPhysicalImpl.getCustomMetadata()).getPulsarName()
                        .name());

        final DefaultAgentNode sinkPhysicalImpl =
                (DefaultAgentNode) implementation.getAgentImplementation(module,
                        "sink1");
        assertEquals("my-sink-name-with-spaces",
                ((PulsarAgentNodeMetadata) sinkPhysicalImpl.getCustomMetadata()).getPulsarName()
                        .name());

        final DefaultAgentNode sourcePhysicalImpl =
                (DefaultAgentNode) implementation.getAgentImplementation(module,
                        "source1");
        assertEquals("my-source-name-with-spaces",
                ((PulsarAgentNodeMetadata) sourcePhysicalImpl.getCustomMetadata()).getPulsarName()
                        .name());
    }


    @Test
    public void testMergeGenAIToolKitAgents() throws Exception {
        Application applicationInstance = ModelBuilder
                .buildApplicationInstance(Map.of(

                        "configuration.yaml",
                        """
                                configuration:  
                                  resources:
                                    - name: open-ai
                                      type: open-ai-configuration
                                      configuration:
                                        url: "http://something"                                
                                        access-key: "xxcxcxc"
                                        provider: "azure"
                                  """,
                        "module.yaml", """
                                module: "module-1"
                                id: "pipeline-1"                                
                                topics:
                                  - name: "input-topic"
                                    creation-mode: create-if-not-exists
                                    schema:
                                      type: avro
                                      schema: '{"type":"record","namespace":"examples","name":"Product","fields":[{"name":"id","type":"string"},{"name":"name","type":"string"},{"name":"description","type":"string"},{"name":"price","type":"double"},{"name":"category","type":"string"},{"name":"item_vector","type":"bytes"}]}}'
                                  - name: "output-topic"
                                    creation-mode: create-if-not-exists                                    
                                pipeline:
                                  - name: "compute-embeddings"
                                    id: "step1"
                                    type: "compute-ai-embeddings"
                                    input: "input-topic"                                    
                                    configuration:                                      
                                      model: "text-embedding-ada-002"
                                      embeddings-field: "value.embeddings"
                                      text: "{{% value.name }} {{% value.description }}"
                                  - name: "compute-embeddings"
                                    id: "step2"
                                    type: "compute-ai-embeddings"                                    
                                    output: "output-topic"
                                    configuration:                                      
                                      model: "text-embedding-ada-003"
                                      embeddings-field: "value.embeddings2"
                                      text: "{{% value.name }}"
                                """), buildInstanceYaml(), null).getApplication();

        @Cleanup ApplicationDeployer deployer = ApplicationDeployer
                .builder()
                .registry(new ClusterRuntimeRegistry())
                .pluginsRegistry(new PluginsRegistry())
                .build();

        Module module = applicationInstance.getModule("module-1");

        ExecutionPlan implementation = deployer.createImplementation("app", applicationInstance);

        AgentNode agentImplementation = implementation.getAgentImplementation(module, "step1");
        assertNotNull(agentImplementation);
        DefaultAgentNode step =
                (DefaultAgentNode) agentImplementation;
        Map<String, Object> configuration = step.getConfiguration();
        log.info("Configuration: {}", configuration);
        Map<String, Object> openAIConfiguration = (Map<String, Object>) configuration.get("openai");
        log.info("openAIConfiguration: {}", openAIConfiguration);
        assertEquals("http://something", openAIConfiguration.get("url"));
        assertEquals("xxcxcxc", openAIConfiguration.get("access-key"));
        assertEquals("azure", openAIConfiguration.get("provider"));


        List<Map<String, Object>> steps = (List<Map<String, Object>>) configuration.get("steps");
        assertEquals(2, steps.size());
        Map<String, Object> step1 = steps.get(0);
        assertEquals("text-embedding-ada-002", step1.get("model"));
        assertEquals("value.embeddings", step1.get("embeddings-field"));
        assertEquals("{{ value.name }} {{ value.description }}", step1.get("text"));

        Map<String, Object> step2 = steps.get(1);
        assertEquals("text-embedding-ada-003", step2.get("model"));
        assertEquals("value.embeddings2", step2.get("embeddings-field"));
        assertEquals("{{ value.name }}", step2.get("text"));


        // verify that the intermediate topic is not created
        log.info("topics {}", implementation.getTopics());
        assertTrue(implementation.getConnectionImplementation(module,
                Connection.fromTopic(TopicDefinition.fromName("input-topic"))) instanceof PulsarTopic);
        assertTrue(implementation.getConnectionImplementation(module,
                Connection.fromTopic(TopicDefinition.fromName("output-topic"))) instanceof PulsarTopic);
        assertEquals(2, implementation.getTopics().size());

    }

}