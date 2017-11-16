# CI/CD generic pipeline

## Introduction 

	This repository describes and implements a generic Jenkins pipeline that could be used to perform a CI/CD workflow for your application on Openshift.
	The term generic that I used before is limited by the following characteristics:
	* The application is compiled and packaged by Maven
	* The static analysis is done on Sonarqube
	* The deliverable (artifact) is pushed in Nexus
        * The application exposes a REST API, so integration testing could be automated (I have used Postman + Newman)

	This means, this pipeline could be reused by any application that matches these characteristics. For the demonstration that I will describe next, I have used a simple application, that it is implemented with Spring Boot and exposing a very simple REST API. You can find it [here](https://github.com/dsanchor/demo-rest)

	The pipeline we are going to show is described by the following diagram. I will explain each stage in detail later in this document.

![Screenshot](cicd-pipeline.png)
	
## Environment

	This demonstration requires Openshift version >= 3.6 (Because we will make use of environment variables for the pipeline BuildConfig). 
	We also require a cluster admin user in Openshift (We need to provide some specific roles to certain service accounts)
	As part of the infrastructure, we will create:
        * Jenkins server, in charge of running the pipeline.
        * Nexus, where all deliverable artifacts will be pushed to. We will also use it as proxy and cache for third party libraries.
        * Sonarqube, used to analize the application against a set of quality rules defined for an standard java application.
	

## Preparation
	
### 1. Login to your Openshift cluster
```
oc login -u admin -p 123456 https://d-sancho.com:8443
```
### 2. Create a project called cicd. We will deploy all the CI/CD infrastructure in this project.
```
 oc new-project cicd 
```
	Notice that I will explain all the steps that must be done in order to have the infrastructure ready. I could have prepared an Openshift template with all these applications defined and configured already, but I prefered to show you how to manually do it, so you were aware about every single step.

### 3. Create a Nexus server (ephemeral for this demo) and expose it externally 
```
oc new-app --docker-image=sonatype/nexus
oc expose svc/nexus
```
**Access to your Nexus server**
	
	1) Get route
```
oc get route | grep nexus | awk '{print $2}'
nexus-cicd.apps.d-sancho.com
```		
	2) Access main dashboard and login with admin/admin123
	http://nexus-cicd.apps.d-sancho.com/nexus		

### 4. Do the same with Sonarqube
```
oc new-app --docker-image=openshiftdemos/sonarqube:6.5
oc expose svc/sonarqube
```
	
**Access to your Sonarqube server**

	1) Get route
```
oc get route | grep sonarqube | awk '{print $2}'
sonarqube-cicd.apps.d-sancho.com
```		
	2) Access main dashboard and login with admin/admin
	http://sonarqube-cicd.apps.d-sancho.com	

### 5. Jenkins server

	We are going to include some plugins that are not included by default in the current Jenkins image provided by Openshift (such as [jenkins-client-plugin](https://github.com/openshift/jenkins-client-plugin)). However, it is very likely that OCP 3.7 will provide this plugin by default (current version of OCP while writing this was 3.6).
	
**Create custom jenkins image with some additional plugins. This image will be created on the openshift namespace, so it will be available from any namespace**
```
oc new-build jenkins:2~https://github.com/dsanchor/jenkins.git --name=jenkins-custom -n openshift
```		
**Once the new image is built, deploy jenkins (ephemeral for this demo)**
```
oc new-app jenkins-ephemeral -p NAMESPACE=openshift -p=JENKINS_IMAGE_STREAM_TAG=jenkins-custom:latest -p MEMORY_LIMIT=2Gi -n cicd
oc expose svc/jenkins
```

**And finally, give self-provisioner cluster role to jenkins service account. For this operation, you will require cluster admin privileges (Example, from any master node: oc login -u system:admin)** 
```
oc adm policy add-cluster-role-to-user self-provisioner system:serviceaccount:cicd:jenkins
```

## Create pipeline

TODO describe demo project and BC 
```
oc create -f https://raw.githubusercontent.com/dsanchor/demo-rest/master/openshift/templates/bc-pipeline.yml -n cicd
oc start-build demo-rest-pipeline -n cicd
```

TODO next steps (such as modifying the code and version of the app and trigger the pipeline again)
Notes:
	Change pom.xml to avoid nexus duplications
