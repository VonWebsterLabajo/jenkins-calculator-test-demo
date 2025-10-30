pipeline {
	agent any

	tools {
		jdk 'jdk21'
		maven 'maven3'
	}

	triggers {
		githubPush()
	}

	environment {
		APP_REPO = 'https://github.com/VonWebsterLabajo/jenkins-calculator-demo.git'
		APP_DIR = 'app'
		TEST_DIR = 'tests'
		HEADLESS = 'true'
	}

	stages {

		stage('Java version') {
			steps {
				echo "Building with JDK 21..."
				sh 'java -version'
			}
		}

		stage('Checkout Repositories') {
			steps {
				echo "🔄 Checking out source and test repositories..."
				dir("${APP_DIR}") {
					git branch: 'main', url: "${APP_REPO}"
				}
				dir("${TEST_DIR}") {
					checkout scm
				}
			}
		}

		stage('Build App Under Test') {
			steps {
				script {
					echo "🚀 Starting local HTTP server for app..."
					dir("${APP_DIR}/src") {
						sh '''
              PORT=3000
              nohup npx http-server -p $PORT -a 0.0.0.0 -c-1 --silent > /tmp/http.log 2>&1 &
							echo $! > /tmp/http.pid
							for i in {1..10}; do
                echo "Waiting for app to start..."
								curl -fsS http://jenkins-lts:3000 && break || sleep 1
							done
						'''
					}
				}
			}
		}

		stage('Run Automated Tests') {
			steps {
				script {
					withEnv([
						"BASE_URL=http://jenkins-lts:3000",
						"SELENIUM_HUB=http://selenium-node:4444/wd/hub"
					]) {
						echo "🧪 Running Selenium Cucumber tests..."
						sh '''
							mvn -B clean test \
								-DbaseUrl=$BASE_URL \
								-Dselenium.hub=$SELENIUM_HUB
						'''
					}
				}
			}
			post {
				always {
					echo "🛑 Stopping local server..."
					sh '''
						if [ -f /tmp/http.pid ]; then
							kill "$(cat /tmp/http.pid)" || true
						fi
					'''
				}
			}
		}

		stage('Generate Allure Report') {
			steps {
				dir("${TEST_DIR}") {
					sh '''
						npm install -g allure-commandline@2
						allure generate target/allure-results --single-file --clean -o target/allure-single
					'''
				}
			}
		}

		stage('Archive Artifacts') {
			steps {
				dir("${TEST_DIR}") {
					archiveArtifacts artifacts: '**/target/allure-single/**', fingerprint: true
					junit '**/target/surefire-reports/*.xml'
				}
			}
		}

		stage('Deploy to Staging') {
			when {
				expression { currentBuild.currentResult == 'SUCCESS' }
			}
			steps {
				echo "🚀 Deploying to Staging..."
				sh 'echo "Deploying to staging..."'
			}
		}

		stage('Deploy to Production (Manual Approval)') {
			when {
				expression { currentBuild.currentResult == 'SUCCESS' }
			}
			steps {
				input message: 'Approve deployment to production?', ok: 'Deploy'
				echo "🚀 Deploying to Production..."
				sh 'echo "Production deployment complete."'
			}
		}

		stage('Notify Result') {
			steps {
				script {
					if (currentBuild.currentResult == 'SUCCESS') {
						echo "✅ Build and tests passed. Notification sent."
					} else {
						echo "❌ Build or test failed. Sending failure notification..."
					}
				}
			}
		}
	}

	post {
		always {
			echo "🧹 Cleaning up temporary files..."
			sh 'rm -f /tmp/http.pid || true'
		}
	}
}