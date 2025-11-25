pipeline {
	agent any

	tools {
		jdk 'jdk21'
	}

	triggers {
		githubPush()
	}

	environment {
		APP_REPO = 'https://github.com/avidcutlet/jenkins-calculator-demo.git'
		APP_DIR = 'app'
		TEST_DIR = 'tests'
		PORT = '3001'
		HEADLESS = 'true'
		SELENIUM_HUB = 'http://selenium-hub:4444/wd/hub'
		BASE_URL = "http://jenkins:${PORT}"
		HTTP_PID_FILE = '/tmp/http.pid'
		HTTP_LOG = '/tmp/http.log'
	}

	stages {

    stage('📦 Checkout Repositories') {
      steps {
        echo "Cloning static app (Repo A) and tests (Repo B)..."
        dir("${APP_DIR}") {
          git branch: 'staging', url: "${APP_REPO}"
        }
        dir("${TEST_DIR}") {
          checkout scm
        }
      }
    }

    stage('🚀 Start Local App Server') {
      steps {
        script {
          dir("${APP_DIR}/src") {
            echo "Starting static app server for index.html on port ${PORT}..."
            sh '''
              # Install http-server locally (no root needed)
              npm install http-server

              # Kill anything that might already use the port (ignore if not found)
              command -v fuser >/dev/null 2>&1 && fuser -k ${PORT}/tcp || true

              # Start http-server in background using nohup
              nohup npx http-server -p ${PORT} -a 0.0.0.0 -c-1 --silent > ${HTTP_LOG} 2>&1 &
              SERVER_PID=$!
              echo $SERVER_PID > ${HTTP_PID_FILE}
              echo "Server started with PID: $SERVER_PID"

              # Wait up to 15 seconds for it to become available
              echo "Waiting for server to respond on http://localhost:${PORT} ..."
              for i in $(seq 1 15); do
                if curl -fsS http://localhost:${PORT} > /dev/null; then
                  echo "✅ Server is up!"
                  exit 0
                fi
                echo "Attempt $i/15: not yet available..."
                sleep 1
              done

              echo "❌ Server failed to start within 15 seconds."
              echo "Logs:"
              cat ${HTTP_LOG} || true
              exit 1
            '''
          }
        }
      }
    }

    stage('🔍 Verify App Server Running') {
      steps {
        sh '''
          echo "Verifying app server..."
          curl -I http://localhost:${PORT} || true
        '''
      }
    }

    stage('🧪 Run Automated Tests') {
      steps {
        dir("${TEST_DIR}") {
          echo "Running Selenium + Cucumber tests..."
          // Prevent stage from aborting pipeline on test failure
          sh '''
            set +e
            mvn -B clean test \
              -DbaseUrl=${BASE_URL} \
              -Dselenium.hub=${SELENIUM_HUB} \
              -Dheadless=${HEADLESS}
            TEST_EXIT_CODE=$?
            echo "Maven exit code: $TEST_EXIT_CODE"
            exit 0  # Always succeed so next stages run
          '''
        }
      }
      post {
        always {
          echo "Stopping HTTP server..."
          sh '''
            if [ -f ${HTTP_PID_FILE} ]; then
              kill "$(cat ${HTTP_PID_FILE})" || true
            fi
          '''
        }
      }
    }

    stage('📈 Push Metrics to InfluxDB') {
      steps {
        script {
          try {
            // Count total tests and failures from Surefire XML
            def totalTests = sh(
              script: "xmllint --xpath 'sum(//testsuite/@tests)' ${TEST_DIR}/target/surefire-reports/*.xml",
              returnStdout: true
            ).trim().toInteger()
            
            def totalFailures = sh(
              script: "xmllint --xpath 'sum(//testsuite/@failures)' ${TEST_DIR}/target/surefire-reports/*.xml",
              returnStdout: true
            ).trim().toInteger()
            
            def passedTests = totalTests - totalFailures

            echo "Total Tests: ${totalTests}, Passed: ${passedTests}, Failures: ${totalFailures}"

            // Push metrics to InfluxDB using a Map
            influxDbPublisher(
              selectedTarget: 'jenkins-influxdb',
              customData: "tests total=${totalTests},passed=${passedTests},failed=${totalFailures}"
            )


          } catch (Exception e) {
            echo "⚠️ InfluxDB push failed: ${e.getMessage()}"
          }
        }
      }
    }

    stage('📊 Generate Allure Report') {
      steps {
        dir("${TEST_DIR}") {
          echo "Generating Allure report..."
          sh '''
            if [ -d target/allure-results ]; then
              npm install allure-commandline@2 --save-dev
              npx allure generate target/allure-results --single-file --clean -o target/allure-single || true
            else
              echo "⚠️ No allure-results found, skipping report generation."
            fi
          '''
        }
      }
    }

    stage('📁 Archive Results') {
      steps {
        dir("${TEST_DIR}") {
          echo "Archiving Allure and test reports..."
          archiveArtifacts artifacts: '**/target/allure-single/**', fingerprint: true
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('🟢 Deploy to GitHub Pages') {
      when {
        expression { currentBuild.currentResult == 'SUCCESS' }
      }
      steps {
        script {
          timeout(time: 15, unit: 'MINUTES') {
            input message: 'Deploy to GitHub Pages?', ok: 'Deploy Now'
          }

          dir("${APP_DIR}") {
            echo "🚀 Deploying static app to GitHub Pages..."
            withCredentials([usernamePassword(credentialsId: 'GITHUB_PAT', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
              sh '''
                git config user.email "jenkins@local"
                git config user.name "Jenkins CI"

                # Save app files before switching branches
                mkdir -p /tmp/deploy-src
                cp -r src/* /tmp/deploy-src/

                # Fetch latest from origin
                git fetch origin

                # Remove untracked files (dangerous if you have important files!)
                git clean -fdx

                # Handle gh-pages branch
                if git show-ref --verify --quiet refs/heads/gh-pages; then
                  # Local branch exists
                  git checkout gh-pages
                elif git ls-remote --exit-code --heads origin gh-pages; then
                  # Remote branch exists, create local tracking branch
                  git checkout -b gh-pages origin/gh-pages
                else
                  # Branch does not exist, create orphan branch
                  git checkout --orphan gh-pages
                fi

                # Clean existing files and copy new build
                git rm -rf . || true
                cp -r /tmp/deploy-src/* .

                # Commit and push using PAT authentication
                git add .
                git commit -m "CD: Deploy from Jenkins build ${BUILD_NUMBER}" || true
                git push -f https://${GIT_USER}:${GIT_PASS}@github.com/VonWebsterLabajo/jenkins-calculator-demo.git gh-pages

                echo "✅ Deployed to GitHub Pages successfully!"
              '''
            }
          }
        }
      }
    }
	}

	post {
		always {
			echo "🧹 Cleanup..."
			sh 'rm -f ${HTTP_PID_FILE} ${HTTP_LOG} || true'
		}

		success {
			emailext(
				subject: "✅ Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
				body: "Build succeeded!<br>Console Output:<br>${env.BUILD_URL}",
				to: "cheqtest.0017@gmail.com",
        from: "vonwebster.ste@gmail.com",
				mimeType: 'text/html',
				attachLog: true
			)
		}

		failure {
			emailext(
				subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
				body: "Build failed!<br>Console Output:<br>${env.BUILD_URL}",
				to: "cheqtest.0017@gmail.com",
        from: "vonwebster.ste@gmail.com",
				mimeType: 'text/html',
				attachLog: true
			)
		}

		unstable {
			emailext(
				subject: "⚠️ Build Unstable: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
				body: "The build is unstable (some tests failed or thresholds not met).<br>Console Output:<br>${env.BUILD_URL}",
				to: "cheqtest.0017@gmail.com",
        from: "vonwebster.ste@gmail.com",
				mimeType: 'text/html',
				attachLog: true
			)
		}
	}
}
