pipeline {
	options {
		timeout(time: 80, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label "ubuntu-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk21-latest'
	}
	stages {
		stage('Build') {
			steps {
				xvnc(useXauthority: true) {
					sh """
					mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs -Papi-check -Pjavadoc \
						-Dcompare-version-with-baselines.skip=false \
						-Dmaven.test.failure.ignore=true \
						-T 1C
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '*.log,**/target/**/*.log', allowEmptyArchive: true
					discoverGitReferenceBuild referenceJob: 'p2/master'
					junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
					recordIssues publishAllIssues: true, ignoreQualityGate: true, enabledForFailure: true, tools: [
							eclipse(name: 'Compiler', pattern: '**/target/compilelogs/*.xml'),
							issues(name: 'API Tools', id: 'apitools', pattern: '**/target/apianalysis/*.xml'),
						], qualityGates: [[threshold: 1, type: 'DELTA', unstable: true]]
					recordIssues tools: [javaDoc(), mavenConsole()]
				}
			}
		}
	}
}
