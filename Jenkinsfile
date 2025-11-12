pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    triggers {
        githubPush()
    }

    environment {
        APP_REPO = 'https://github.com/VonWebsterLabajo/jenkins-calculator-demo.git'
        APP_DIR = 'app'
        TEST_DIR = 'tests'
        PORT = '3000'
        HEADLESS = 'true'
        SELENIUM_HUB = 'http://selenium-hub:4444/wd/hub'
        BASE_URL = "http://jenkins-lts:${PORT}"
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
          expression {
              // Only run if the previous stages succeeded
              currentBuild.currentResult == 'SUCCESS'
          }
      }
      steps {
          script {
              // Manual approval before deploying
              timeout(time: 15, unit: 'MINUTES') {
                  input message: 'Deploy to GitHub Pages?', ok: 'Deploy Now'
              }

              dir("${APP_DIR}") {
                  echo "🚀 Deploying static app to GitHub Pages..."

                  sh '''
                      # Configure Git
                      git config user.email "jenkins@local"
                      git config user.name "Jenkins CI"

                      # Save the files first
                      mkdir -p /tmp/deploy-src
                      cp -r src/* /tmp/deploy-src/

                      # Switch to gh-pages branch
                      git fetch origin
                      if git show-ref --quiet refs/remotes/origin/gh-pages; then
                          git checkout gh-pages
                      else
                          git checkout --orphan gh-pages
                      fi

                      # Clear old files and copy new ones
                      rm -rf *
                      cp -r /tmp/deploy-src/* .

                      git add .
                      git commit -m "CD: Deploy from Jenkins build ${BUILD_NUMBER}" || true
                      git push -f origin gh-pages

                      echo "✅ Deployed to GitHub Pages successfully!"
                  '''
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
  }
}