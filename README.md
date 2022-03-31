# SoftwareCognitiveComplexityReducer

This repository contains the developed source code to reduce software cognitive complexity of Java projects. It is developed as a head-less Eclipse plug-in and uses [JDT](https://www.eclipse.org/jdt/) to create and process abstract syntax trees (AST) from Java source code.

### Abstract
Different cognitive complexity measures have been proposed to quantify the understandability of a piece of code and, therefore, its maintainability. However, the cognitive complexity metric provided by SonarSource and integrated in SonarCloud and SonarQube is quickly spreading in the software industry due to the popularity of these well-known static code tools for evaluating software quality. Despite SonarQube suggests to keep method's cognitive complexity no greater than 15, reducing method's complexity is challenging for a human programmer and there are no approaches to assist developers on this task.

In the paper [Automatizing Software Cognitive Complexity Reduction](https://ieeexplore.ieee.org/document/9686676), which is published in the IEEE Access journal, we model the cognitive complexity reduction of a method as an optimization problem where the search space contains all sequences of Extract Method refactoring opportunities.  We then propose a novel approach that searches for feasible code extractions allowing developers to apply them, all in an automated way. This will allow software developers to make informed decisions while reducing the complexity of their code. We have evaluated the proposed approach over 10 open-source software projects and was able to fix 78% of the 1,050 existing cognitive complexity issues reported by SonarQube.

### Software projects under study
In order to ease the replication of the study, next we show detailed information of the 10 open-source software projects used in our experiments.

User | Repository | Commit | Project link
--- | --- | --- | --- 
AIoTES | DataLayer-DataLake-QueryExecution | c032e5a | https://github.com/AIoTES/DataLayer-DataLake-QueryExecution
alibaba | fastjson | 93d8c01e9 | https://github.com/alibaba/fastjson
fiware-cybercaptor | cybercaptor-server | b6b1f10 | https://github.com/fiware-cybercaptor/cybercaptor-server
jMetal | jMetal | e6baf75aa | https://github.com/jMetal/jMetal
Konloch | bytecode-viewer | 55bfc32 | https://github.com/Konloch/bytecode-viewer/
KnowageLabs | Knowage-Server | dfed28a869 | https://github.com/KnowageLabs/Knowage-Server/tree/master/knowage-core
mobius-software-ltd | iotbroker.cloud-java-client | 98eeceb | https://github.com/mobius-software-ltd/iotbroker.cloud-java-client
MOEAFramework | MOEAFramework | 223393fd | https://github.com/MOEAFramework/MOEAFramework
redis | jedis | cfc227f7 | https://github.com/redis/jedis
telefonicaid | fiware-commons | f83b342 | https://github.com/telefonicaid/fiware-commons

### How to cite this work
R. Saborido, J. Ferrer, F. Chicano and E. Alba, "Automatizing Software Cognitive Complexity Reduction" in IEEE Access, vol. 10, pp. 11642-11656, 2022, doi: 10.1109/ACCESS.2022.3144743.

#### Open science
The paper is open access and the full text is available at https://ieeexplore.ieee.org/document/9686676 

### Software dependencies (used versions)
- JDK 11
- jGrapht-1.5.0
- jHeaps-0.14
