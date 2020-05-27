[![codecov](https://codecov.io/gh/valb3r/flowable-bpmn-intellij-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/valb3r/flowable-bpmn-intellij-plugin)


# What is this

This project provides plugin for [Flowable BPMN engine](https://github.com/flowable/flowable-engine) process editor integration into IntelliJ,
aiming for code navigation support between BPMN diagram and your classes including Spring beans.

Currently, it is work-in-progress.

# Installation

You can install the plugin from ZIP file provided at releases page of this repository or from alpha-channel of JetBrains
plugin repository.

## From GitHub releases

Install a plugin from:
[0.1 MVP](https://github.com/valb3r/flowable-bpmn-intellij-plugin/releases/download/v0.1.0-b2/bpmn-intellij-plugin.zip) release page

**NOTE: The plugin requires 'Ultimate Edition' of InteliJ for code navigation**

Installation guideline video:

[![Install plugin](https://img.youtube.com/vi/tfSAEMSIrqA/0.jpg)](https://www.youtube.com/watch?v=tfSAEMSIrqA)


## From IntelliJ plugins alpha-channel repository

1. Open `File -> Settings -> Plugins`
1. In opened window, click on `Gear` button (Manage plugins, repositories) -> `Manage Plugin Repositories`
1. Add Alpha releases channel - `https://plugins.jetbrains.com/plugins/alpha/list` to repositories list
1. Now you are able to list and install `Flowable bpmn visualizer` plugin directly from plugin search window

IntelliJ docs on this topic:

[How to configure and use alpha channel](https://plugins.jetbrains.com/docs/marketplace/custom-release-channels.html#CustomReleaseChannels-ConfiguringaCustomChannelinIntelliJPlatformBasedIDEs)

# Workflow

MVP workflow demo:

[![Work with plugin](https://img.youtube.com/vi/b0swfexfiMg/0.jpg)](https://youtu.be/b0swfexfiMg)


# Navigation guideline

1. To open BPMN diagram in the plugin - left mouse button on XML file and select `View BPMN Diagram`
1. To move diagram up/down/left/right - click mouse wheel and start moving your mouse - diagram will follow 
(like dragging with mouse wheel)
1. Zoom in/out - mouse wheel rotation
1. To select element click on it with mouse


# Flowable XML subset support

<details><summary><b>View summary table</b></summary>


|            XML element           | Supported |
|:--------------------------------:|:---------:|
|          adHocSubProcess         |           |
|            association           |           |
|           boundaryEvent          |           |
|        bpmndi:BPMNDiagram        |     Y     |
|          bpmndi:BPMNEdge         |     Y     |
|         bpmndi:BPMNPlane         |     Y     |
|         bpmndi:BPMNShape         |     Y     |
|         businessRuleTask         |     Y     |
|           callActivity           |     Y     |
|       cancelEventDefinition      |           |
|     compensateEventDefinition    |           |
|        completionCondition       |           |
|             condition            |           |
|    conditionalEventDefinition    |           |
|        conditionExpression       |     Y     |
|            dataObject            |           |
|            definitions           |           |
|           documentation          |     Y     |
|             endEvent             |     Y     |
|       errorEventDefinition       |           |
|     escalationEventDefinition    |           |
|         eventBasedGateway        |           |
|         exclusiveGateway         |     Y     |
|         extensionElements        |           |
|      flowable:eventListener      |           |
|    flowable:executionListener    |           |
|          flowable:field          |           |
|            flowable:in           |           |
|           flowable:out           |           |
|          flowable:string         |           |
|          flowable:value          |           |
|         inclusiveGateway         |           |
|      intermediateCatchEvent      |           |
|      intermediateThrowEvent      |           |
|          loopCardinality         |           |
|              message             |           |
|      messageEventDefinition      |           |
| multiInstanceLoopCharacteristics |           |
|           omgdc:Bounds           |     Y     |
|          omgdi:waypoint          |     Y     |
|          parallelGateway         |           |
|              process             |           |
|            receiveTask           |           |
|              script              |     Y     |
|            scriptTask            |     Y     |
|           sequenceFlow           |     Y     |
|            serviceTask           |     Y     |
|       signalEventDefinition      |           |
|            startEvent            |     Y     |
|            subProcess            |           |
|     terminateEventDefinition     |           |
|               text               |           |
|          textAnnotation          |           |
|             timeDate             |           |
|       timerEventDefinition       |           |
|            transaction           |           |
|             userTask             |     Y     |

</details>
