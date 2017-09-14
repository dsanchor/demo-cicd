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

return this
