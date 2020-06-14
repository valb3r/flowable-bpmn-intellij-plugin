[![codecov](https://codecov.io/gh/valb3r/flowable-bpmn-intellij-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/valb3r/flowable-bpmn-intellij-plugin)


# What is this

This project provides plugin for [Flowable BPMN engine](https://github.com/flowable/flowable-engine) process editor integration into IntelliJ,
aiming for code navigation support between BPMN diagram and your classes including Spring beans.

**Currently, it is work-in-progress.**


# Key features

1. BPMN process editing - adding/removing elements, changing their properties, undo/redo, bulk drag-n-drop, bulk removal of elements
1. Code navigation - jump from `Delegate Expression` or `Class` property directly to bean/function/class in code
1. IntelliJ refactorings propagation to backing XML file of the process (i.e. rename bean)
1. BPMN process 'debugging' by allowing to see steps (and their order) done for latest process execution directly in plugin


# Installation

You can install the plugin from ZIP file provided at releases page of this repository or from alpha-channel of JetBrains
plugin repository.

## From GitHub releases

Install the latest plugin version plugin from the releases page:
[Releases page](https://github.com/valb3r/flowable-bpmn-intellij-plugin/releases/)

**NOTE: The plugin requires 'Ultimate Edition' of IntelliJ for code navigation**

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


## Plugin usage:

[![Work with plugin](https://img.youtube.com/vi/4kCLCzj4gpg/0.jpg)](https://youtu.be/4kCLCzj4gpg)


## Debugging BPMN process with the plugin

[![Debug BPMN with plugin](https://img.youtube.com/vi/_zQ1zy_0Qfc/0.jpg)](https://youtu.be/_zQ1zy_0Qfc)


# Navigation guideline

1. To open BPMN diagram in the plugin - left mouse button on XML file and select `View BPMN Diagram`
1. To move diagram up/down/left/right - click mouse wheel and start moving your mouse - diagram will follow 
(like dragging with mouse wheel)
1. Zoom in/out - mouse wheel rotation
1. To select element click on it with mouse


# FAQ

**Q**: Some feature/bug was closed, but I can't find new release.

**A**: Check [this link](https://github.com/valb3r/flowable-bpmn-intellij-plugin/projects/1) for the status of your feature. 
If it is in 'Done' (or Closed) column this doesn't mean it is released - it must proceed to next columns to appear in 
any kind of release-artifact. After feature is 'Closed' it will wait for GitHub release 
at [Releases](https://github.com/valb3r/flowable-bpmn-intellij-plugin/releases) page and after that it will be 
published to JetBrains marketplace. It can take up to two days for plugin to be available on JetBrains marketplace.


# Flowable XML subset support

<details><summary><b>View summary table</b></summary>


|            XML element           | Supported |
|:--------------------------------:|:---------:|
|          adHocSubProcess         |     Y     |
|            association           |           |
|           boundaryEvent          |     P     |
|        bpmndi:BPMNDiagram        |     Y     |
|          bpmndi:BPMNEdge         |     Y     |
|         bpmndi:BPMNPlane         |     Y     |
|         bpmndi:BPMNShape         |     Y     |
|         businessRuleTask         |     Y     |
|           callActivity           |     Y     |
|       cancelEventDefinition      |     P     |
|     compensateEventDefinition    |     P     |
|        completionCondition       |     P     |
|             condition            |     P     |
|    conditionalEventDefinition    |     P     |
|        conditionExpression       |     Y     |
|            dataObject            |           |
|            definitions           |           |
|           documentation          |     Y     |
|             endEvent             |     Y     |
|       errorEventDefinition       |     P     |
|     escalationEventDefinition    |     P     |
|         eventBasedGateway        |     P     |
|         exclusiveGateway         |     Y     |
|         extensionElements        |           |
|      flowable:eventListener      |           |
|    flowable:executionListener    |           |
|          flowable:field          |           |
|            flowable:in           |           |
|           flowable:out           |           |
|          flowable:string         |           |
|          flowable:value          |           |
|         inclusiveGateway         |     Y     |
|      intermediateCatchEvent      |     P     |
|      intermediateThrowEvent      |     P     |
|          loopCardinality         |           |
|              message             |           |
|      messageEventDefinition      |           |
| multiInstanceLoopCharacteristics |           |
|           omgdc:Bounds           |     Y     |
|          omgdi:waypoint          |     Y     |
|          parallelGateway         |     P     |
|              process             |     P     |
|            receiveTask           |     P     |
|              script              |     Y     |
|            scriptTask            |     Y     |
|           sequenceFlow           |     Y     |
|            serviceTask           |     Y     |
|       signalEventDefinition      |     P     |
|            startEvent            |     Y     |
|            subProcess            |     Y     |
|     terminateEventDefinition     |     P     |
|               text               |     P     |
|          textAnnotation          |     P     |
|             timeDate             |     P     |
|       timerEventDefinition       |     P     |
|            transaction           |     Y     |
|             userTask             |     Y     |

**Legend**:

**Y** - Mostly or fully supported

**P** - Partially supported

**Blank** - Mostly unsupported

</details>
