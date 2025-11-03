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
        SELENIUM_HUB = 'http://selenium-node:4444/wd/hub'
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

        stage('🌐 Serve Static App') {
            steps {
                script {
                    dir("${APP_DIR}") {
                      echo "Starting static app server for index.html on port ${PORT}..."
                      sh '''
                          # Install http-server locally (no sudo/global permission issue)
                          npm install http-server

                          # Kill any process using the port
                          fuser -k ${PORT}/tcp || true

                          # Start the server in background
                          npx http-server -p ${PORT} -a 0.0.0.0 -c-1 --silent &
                          SERVER_PID=$!

                          echo $SERVER_PID > ${HTTP_PID_FILE}
                          echo "Server PID: $SERVER_PID"
                          
                          # Wait for it to be ready
                          echo "Waiting for server to respond..."
                          for i in {1..15}; do
                              if curl -fsS http://localhost:${PORT} > /dev/null; then
                                  echo "✅ Server is up!"
                                  exit 0
                              fi
                              echo "Attempt $i/15: server not ready yet..."
                              sleep 1
                          done

                          echo "❌ Server failed to start in time."
                          exit 1
                      '''
                    }
                }
            }
        }

        stage('🔍 Verify App Server Running') {
            steps {
                sh '''
                    echo "Checking app homepage:"
                    curl -I http://localhost:${PORT} || true
                '''
            }
        }

        stage('🧪 Run Automated Tests') {
            steps {
                dir("${TEST_DIR}") {
                    echo "Running Selenium + Cucumber tests..."
                    sh '''
                        mvn -B clean test \
                            -DbaseUrl=${BASE_URL} \
                            -Dselenium.hub=${SELENIUM_HUB} \
                            -Dheadless=${HEADLESS}
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
                        npm install allure-commandline@2 --save-dev
                        npx allure generate target/allure-results --single-file --clean -o target/allure-single
                    '''
                }
            }
        }

        stage('📁 Archive Results') {
            steps {
                dir("${TEST_DIR}") {
                    archiveArtifacts artifacts: '**/target/allure-single/**', fingerprint: true
                    junit '**/target/surefire-reports/*.xml'
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