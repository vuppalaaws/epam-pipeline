def call(body) {
	List<String> artifactsList = []
	def pipelineParams=[:]
	body.resolveStrategy=Closure.DELEGATE_FIRST
	body.delegate=pipelineParams
	body()
		node{
			def step=""
			git branch:pipelineParams.branch,
				credentialsId:pipelineParams.credentials,
				url: pipelineParams.gitUrl
			def datas = readYaml file: pipelineParams.configurationFile
			try {			
				print(datas["notifications"]["email"]["recipients"])
				//notifications, build, database, deploy, test
				stage('Build'){
					try{
						dir(datas["build"]["projectFolder"]) {
							print("Build")
							def command=datas["build"]["buildCommand"]
							sh "$command"
						}
					}catch  (exc) {
					   step="Build"
					   currentBuild.result = 'FAILURE'
					   //error "Build Failed"
					}
				}//end of Build
				stage('Database'){
					try{
						dir(datas["database"]["databaseFolder"]) {
							print("Database")
							def command=datas["database"]["databaseCommand"]
							sh "$command"
						}
					}catch  (exc) {
					   step="Database"
					   currentBuild.result = 'FAILURE'
					   //error "Database Failed"
					}
				}//end of Database
				stage('Deploy'){
					try{
						dir(datas["build"]["projectFolder"]) {
							print("Deploy")
							def command=datas["deploy"]["deployCommand"]
							sh "$command"
						  //throw error("Build failed")
						}
					}catch  (exc) {
					   step="Deploy"
					   currentBuild.result = 'FAILURE'
					   //error "Deploy Failed"
					}
				}//end of Deploy
				stage('Test'){
					try{
					  print("Test")
					    parallel('Test1': {dir(datas["test"][0]["testFolder"]) {
						   try{
								def command=datas["test"][0]["testCommand"]
								sh "$command"
						   }catch  (exc) {
							   error "Build Failed"
							   print(exc)
						   }
					   }},
					   'Test2': {dir(datas["test"][1]["testFolder"]) {
						   try{
								def command=datas["test"][1]["testCommand"]
								sh "$command"
						   }catch  (exc) {
							   error "Build Failed"
							   print(exc)
						   }
					   }},
					   'Test3': {dir(datas["test"][2]["testFolder"]) {
						  try{
								def command=datas["test"][2]["testCommand"]
								sh "$command"
						   }catch  (exc) {
							   error "Build Failed"
							   print(exc)
						   }
					   }})
					}catch  (exc) {
					   step="Test"
					   currentBuild.result = 'FAILURE'
					   error "Test Failed"
					}
				}//end of Test
			}
			catch (exc) {
			   // print("Exception")
				error "Jenkins Build Failed"
				
			}
			finally {
				if(step != "" && currentBuild.result == 'FAILURE'){
					print("Build failed in "+step+" stage")
				}else{
					print("Jenkins Build successful")
				}
			}
		}
}

