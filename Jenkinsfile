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
                        echo "Starting local HTTP server for index.html..."
                        sh '''
                            npm install http-server
                            nohup npx http-server -p $PORT -a 0.0.0.0 -c-1 --silent > ${HTTP_LOG} 2>&1 &
                            echo $! > ${HTTP_PID_FILE}

                            echo "Waiting for app to start..."
                            for i in {1..10}; do
                                curl -fsS http://localhost:${PORT} && echo "App started!" && break || sleep 1
                            done
                        '''
                    }
                }
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
                        npm install -g allure-commandline@2
                        allure generate target/allure-results --single-file --clean -o target/allure-single
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