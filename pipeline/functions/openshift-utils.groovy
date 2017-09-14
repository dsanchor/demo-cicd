// openshift-utils methods

def createProject(project, jenkinsProject) {
   try {
      // try to create the project
      echo "Creating project ${project} if it does not exist yet"
      openshift.newProject( project, "--display-name", project)
      echo "Project ${project} has been created"
   } catch ( e ) {
      echo "Check error.. but it could be that the project already exists... skkiping step"
      echo "${e}"
      // TODO To be decided.. => if the project was not created by jenkins sa, then, it is vey likely that its sa doesnt have admin or edit role. If it was created by jenkins, jenkins sa will have admin role
      //openshift.policy("add-role-to-user", "edit", "system:serviceaccount:${jenkinsProject}:jenkins", "-n", project)
   }
}


def applyTemplate(project, templateFile, appName, appVersion, customParameters) {
   echo "Applying template ${templateFile} in project ${project}. Application: ${appName}-${appVersion}"
   openshift.withProject( project ) {
      echo "Additional parameters for template are ${customParameters}"
      def models = openshift.process( readFile(file:templateFile), "-p NAME=${appName}", "-p APP_VERSION=${appVersion}", customParameters )
      echo "This template has ${models.size()} objects"
      def created = openshift.apply( models )
     // do we want to show "created"?
   }
}

def applyTemplate(project, templateFile, appName, appVersion, customParameters, skipObjects) {
   echo "Applying template ${templateFile} in project ${project}. Application: ${appName}-${appVersion}"
   openshift.withProject( project ) {
      echo "Additional parameters for template are ${customParameters}"
      def models = openshift.process( readFile(file:templateFile), "-p NAME=${appName}", "-p APP_VERSION=${appVersion}", customParameters )
      for ( o in models ) {
         // we will discard skipObjects
         def skip = false
         for ( skipObject in skipObjects ) {
           if (o.kind != skipObject) {
	      skip = true
	      break
           }
         }
         if (!skip) {
            // TODO consider not to override replica numbers in DC or any other parameter.. so it should be managed each type individually and save previous state when needed
            echo "Applying changes on ${o.kind}"
            def created = openshift.apply(o) 
           // do we want to show "created"?
         }
      }
   }
}

def startBuildFromFile(project, appName, file, watchUntilCompletion) {
   echo "Starting binary build in project ${project} for application ${appName}"
   openshift.withProject( project ) {
      echo "Using file ${file} in build"
      // start build
      def build = openshift.startBuild(appName,"--from-file=${file}")
      build.describe()
      if (watchUntilCompletion) {
         echo "user has requested to wait until build has finished"
         build.watch {
            return it.object().status.phase == "Complete"
         }    
      }              
   }
} 

def deploy(project, appName) {
   echo "Deploying application ${appName} in project ${project}"
   openshift.withProject( project ) {
      def dc = openshift.selector("dc", appName)

      def replicas = dc.object().spec.replicas
      def currentPods = dc.related('pods').count()
 
      def rm = dc.rollout() 
      def lastDeploy = rm.latest()
      echo "${lastDeploy.out}"
            
      dc.related( 'pods' ).watch {
         // End the watch only when rolling new pods
	 return it.count() > currentPods 
      }
      echo "Rolling out deployment"
      dc.related( 'pods' ).watch {
         // End the watch only once the exact number of replicas is back
         return it.count() == replicas 
      }
      // Let's wait until pods are Running
      dc.related( 'pods' ).untilEach {
         echo "Pod ${it.object().metadata.name}"
         return it.object().status.phase == 'Running'
      }
      echo "New deployment ready"
   }
}

return this
