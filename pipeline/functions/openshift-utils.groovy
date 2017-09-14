// openshift-utils methods

def createProject(project, jenkinsProject) {
   try {
      // try to create the project
      openshift.newProject( project, "--display-name", project)
   } catch ( e ) {
      echo "Check error.. but it could be that the project already exists... skkiping step"
      echo "${e}"
      // TODO if the project was not created by jenkins sa, then, it is vey likely that its sa doesnt have admin or edit role. If it was created by jenkins, jenkins sa will have admin role
      //openshift.policy("add-role-to-user", "edit", "system:serviceaccount:${jenkinsProject}:jenkins", "-n", project)
   }
}

def test(){ echo "It is loaded.."}
