# Eclipse TEA

Eclipse TEA™ is a tasking orchestration engine that can be run from within the Eclipse IDE or headlessly. It is immediately concerned with tasks related to building and managing plug-ins for the Eclipse Platform, but is general enough to support other types of tasks and builds.

## General Idea

Eclipse TEA's major component is the so called TaskingEngine. This engine can be created and run both from the IDE, as well as headlessly (with a headless presentation engine – thus the PlatformUI.getWorkbench() DOES work – in case a task requires “UI” but still should run headlessly). This allows to create reproducible and stable TaskChains that can be used by both developers and automated builds to create the same, reproducible results for certain tasks.

## More Information

See our Website at https://www.eclipse.org/tea/ and additional information can be found at https://projects.eclipse.org/projects/technology.tea
