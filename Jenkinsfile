pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    triggers {
        githubPush() // Automatically triggered from Repo A via Webhook Relay
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

    options {
        timestamps()
    }

    stages {

        stage('🧱 Verify Java & Maven') {
            steps {
                echo "Checking Java & Maven versions..."
                sh '''
                    java -version
                    mvn -version
                '''
            }
        }

        stage('📦 Checkout Repositories') {
            steps {
                echo "Cloning App (Repo A) and Tests (Repo B)..."
                dir("${APP_DIR}") {
                    git branch: 'main', url: "${APP_REPO}"
                }
                dir("${TEST_DIR}") {
                    checkout scm // Repo B (this Jenkinsfile)
                }
            }
        }

        stage('🚀 Start Local App Server') {
            steps {
                script {
                    dir("${APP_DIR}/src") {
                        echo "Starting app at http://0.0.0.0:${PORT} ..."
                        sh '''
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
                script {
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
            }
            post {
                always {
                    echo "Stopping app server..."
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

        stage('📁 Archive Reports & Results') {
            steps {
                dir("${TEST_DIR}") {
                    echo "Archiving Allure reports and JUnit results..."
                    archiveArtifacts artifacts: '**/target/allure-single/**', fingerprint: true
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('🚀 Deploy to Staging') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo "Deploying to Staging environment..."
                sh 'echo "Staging deployment complete."'
            }
        }

        stage('🧭 Manual Approval: Production') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                input message: 'Approve deployment to Production?', ok: 'Deploy'
                echo "Deploying to Production..."
                sh 'echo "Production deployment complete."'
            }
        }

        stage('📢 Notify Result') {
            steps {
                script {
                    if (currentBuild.currentResult == 'SUCCESS') {
                        echo "✅ Build & Tests Passed — All good!"
                    } else {
                        echo "❌ Build or Tests Failed — Please check logs."
                    }
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning up temporary files..."
            sh '''
                rm -f ${HTTP_PID_FILE} ${HTTP_LOG} || true
            '''
        }
    }
}