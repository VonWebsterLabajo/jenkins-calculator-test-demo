pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    parameters {
        string(name: 'ARTIFACT_NAME', defaultValue: '', description: 'Artifact from Repo A')
        string(name: 'ARTIFACT_DIR', defaultValue: '', description: 'Artifact directory from Repo A')
    }

    environment {
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

        stage('Checkout Repo B') {
            steps {
                checkout scm
            }
        }

        stage('Retrieve Artifact from Repo A') {
            steps {
                echo "Downloading artifact from Repo A build..."
                copyArtifacts(
                    projectName: 'RepoA_Build_Pipeline', // Jenkins job for Repo A
                    filter: "${params.ARTIFACT_DIR}/${params.ARTIFACT_NAME}",
                    fingerprintArtifacts: true,
                    optional: false
                )
            }
        }

        stage('Start App Server') {
            steps {
                sh '''
                    nohup npx http-server ${ARTIFACT_DIR} -p ${PORT} -a 0.0.0.0 -c-1 --silent > ${HTTP_LOG} 2>&1 &
                    echo $! > ${HTTP_PID_FILE}
                    echo "Waiting for app to start..."
                    for i in {1..10}; do
                        curl -fsS http://localhost:${PORT} && echo "App started!" && break || sleep 1
                    done
                '''
            }
        }

        stage('Run Tests') {
            steps {
                dir("${TEST_DIR}") {
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
                    sh 'kill $(cat ${HTTP_PID_FILE}) || true'
                }
            }
        }

        stage('Generate & Archive Reports') {
            steps {
                dir("${TEST_DIR}") {
                    sh '''
                        npm install -g allure-commandline@2
                        allure generate target/allure-results --single-file --clean -o target/allure-single
                    '''
                    archiveArtifacts artifacts: '**/target/allure-single/**', fingerprint: true
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Notify Result') {
            steps {
                script {
                    if (currentBuild.currentResult == 'SUCCESS') {
                        echo "✅ Tests Passed"
                    } else {
                        echo "❌ Tests Failed"
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f ${HTTP_PID_FILE} ${HTTP_LOG} || true'
        }
    }
}