pipeline {
    agent any

    environment {
        APP_REPO = 'https://github.com/VonWebsterLabajo/jenkins-calculator-demo.git'
        APP_DIR = 'app'
        TEST_DIR = 'tests'
        HEADLESS = 'true'
        JAVA_HOME = tool name: 'jdk17', type: 'jdk'
    }

    stages {

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
                        // Start a simple web server in background
                        sh '''
                            nohup npx http-server -p 8080 -c-1 --silent > /tmp/http.log 2>&1 &
                            echo $! > /tmp/http.pid
                            for i in {1..10}; do
                              curl -fsS http://127.0.0.1:8080 && break || sleep 1
                            done
                        '''
                    }
                }
            }
        }

        stage('Run UI Tests') {
            steps {
                script {
                    echo "🧪 Running Cucumber + Allure Tests..."
                    dir("${TEST_DIR}") {
                        withEnv(["BASE_URL=http://127.0.0.1:8080", "HEADLESS=${HEADLESS}"]) {
                            sh '''
                                mvn -B clean test \
                                  -DbaseUrl=$BASE_URL \
                                  -Dheadless=$HEADLESS
                            '''
                        }
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
                // Replace this with your actual deployment command
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
                        // Optionally send email or Slack message here
                    } else {
                        echo "❌ Build or test failed. Sending failure notification..."
                        // Optionally send failure email or Slack message
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