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
        BASE_URL = "http://calculator-app:${PORT}"
        HTTP_PID_FILE = '/tmp/http.pid'
        HTTP_LOG = '/tmp/http.log'
    }

    stages {

        stage('📦 Checkout Repositories') {
            steps {
                echo "Cloning static app (Repo A) and tests (Repo B)..."
                dir("${APP_DIR}") {
                    git branch: 'main', url: "${APP_REPO}"
                }
                dir("${TEST_DIR}") {
                    checkout scm
                }
            }
        }

        stage('Verify App Service') {
            steps {
                echo "Checking if the app service is reachable..."
                sh '''
                    for i in {1..15}; do
                        curl -fsS ${BASE_URL} && break
                        echo "Attempt \$i: app not yet available..."
                        sleep 1
                    done
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
        mimeType: 'text/html',
        attachLog: true
        )
    }
    failure {
      emailext(
        subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: "Build failed!<br>Console Output:<br>${env.BUILD_URL}",
        to: "cheqtest.0017@gmail.com",
        mimeType: 'text/html',
        attachLog: true
      )
    }
    unstable {
      emailext(
        subject: "⚠️ Build Unstable: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: "The build is unstable (some tests failed or thresholds not met).<br>Console Output:<br>${env.BUILD_URL}",
        to: "cheqtest.0017@gmail.com",
        mimeType: 'text/html',
        attachLog: true
      )
    }
  }
}