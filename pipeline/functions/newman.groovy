// newman utility operations

def runTest(testCollection, testEnvironment) {
   echo "Testing collection ${testCollection} against environment ${testEnvironment}"
   // TODO create a jenkins slave image with newman install
   // meanwhile, install it everytime...
   sh "npm install -g newman"
   sh "newman run ${testCollection} -e ${testEnvironment}"
}

return this
