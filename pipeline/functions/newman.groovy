// newman utility operations

def runTest(testCollection, testEnvironment) {
   echo "Testing collection ${testCollection} against environment ${testEnvironment}"
   // install newman if not present 
   sh "npm install -g newman"
   sh "newman run ${testCollection} -e ${testEnvironment}"
}

return this
