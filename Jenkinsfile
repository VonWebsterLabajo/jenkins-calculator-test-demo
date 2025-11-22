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
        TEST_REPO = 'https://github.com/avidcutlet/jenkins-calculator-test-demo.git'
        APP_DIR = 'app'
        TEST_DIR = 'tests'
        TEST_PAGES_URL = 'https://avidcutlet.github.io/jenkins-calculator-test-demo/'
        PROD_PAGES_URL = 'https://avidcutlet.github.io/jenkins-calculator-demo/'
        HEADLESS = 'true'
        SELENIUM_HUB = 'http://selenium-hub:4444/wd/hub'
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

        stage('🚀 Deploy App to Test GitHub Pages') {
            steps {
                script {
                    dir("${APP_DIR}") {
                        withCredentials([usernamePassword(credentialsId: 'GITHUB_PAT', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                            sh '''
                                git config user.email "jenkins@local"
                                git config user.name "Jenkins CI"

                                mkdir -p /tmp/deploy-src
                                cp -r * /tmp/deploy-src/

                                git init
                                git remote add origin ${APP_REPO}

                                # Handle gh-pages branch
                                if git ls-remote --exit-code --heads origin gh-pages; then
                                    git fetch origin gh-pages
                                    git checkout -b gh-pages origin/gh-pages
                                else
                                    git checkout --orphan gh-pages
                                fi

                                git rm -rf . || true
                                cp -r /tmp/deploy-src/* .

                                git add .
                                git commit -m "CD: Deploy to Test GH Pages from Jenkins build ${BUILD_NUMBER}" || true
                                git push -f https://${GIT_USER}:${GIT_PASS}@github.com/avidcutlet/jenkins-calculator-test-demo.git gh-pages
                            '''

                            echo "✅ Deployment triggered, waiting for GitHub Pages to serve new content..."

                            // Poll the deployed URL until it returns HTTP 200 with cache-busting query string
                            sh '''
                                MAX_WAIT=60
                                SLEEP_INTERVAL=5
                                URL=${TEST_PAGES_URL}

                                echo "Waiting for ${URL} to be available..."
                                for i in $(seq 1 $((MAX_WAIT / SLEEP_INTERVAL))); do
                                    CB=$(date +%s)
                                    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$URL?cb=$CB")
                                    if [ "$STATUS" == "200" ]; then
                                        echo "✅ GitHub Pages is live!"
                                        exit 0
                                    fi
                                    echo "Attempt $i: status $STATUS, waiting $SLEEP_INTERVAL seconds..."
                                    sleep $SLEEP_INTERVAL
                                done

                                echo "❌ GitHub Pages did not return HTTP 200 within $MAX_WAIT seconds."
                                exit 1
                            '''
                        }
                    }
                }
            }
        }

        stage('🧪 Run Automated Tests on Test GitHub Pages') {
            steps {
                dir("${TEST_DIR}") {
                    echo "Running Selenium + Cucumber tests on ${TEST_PAGES_URL}..."
                    sh """
                        set +e
                        mvn -B clean test \
                            -DbaseUrl=${TEST_PAGES_URL} \
                            -Dselenium.hub=${SELENIUM_HUB} \
                            -Dheadless=${HEADLESS}
                        TEST_EXIT_CODE=\$?
                        echo "Maven exit code: \$TEST_EXIT_CODE"
                        exit 0
                    """
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

        stage('🟢 Deploy to Production GitHub Pages') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                script {
                    timeout(time: 15, unit: 'MINUTES') {
                        input message: 'Deploy to Production GitHub Pages?', ok: 'Deploy Now'
                    }

                    dir("${APP_DIR}") {
                        withCredentials([usernamePassword(credentialsId: 'GITHUB_PAT', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                            sh """
                                git config user.email "jenkins@local"
                                git config user.name "Jenkins CI"

                                mkdir -p /tmp/deploy-src
                                cp -r * /tmp/deploy-src/

                                git init
                                git remote add origin ${APP_REPO}

                                if git ls-remote --exit-code --heads origin gh-pages; then
                                    git fetch origin gh-pages
                                    git checkout -b gh-pages origin/gh-pages
                                else
                                    git checkout --orphan gh-pages
                                fi

                                git rm -rf . || true
                                cp -r /tmp/deploy-src/* .

                                git add .
                                git commit -m "CD: Deploy to Production GH Pages from Jenkins build ${BUILD_NUMBER}" || true
                                git push -f https://${GIT_USER}:${GIT_PASS}@github.com/avidcutlet/jenkins-calculator-demo.git gh-pages
                            """
                        }
                    }
                }
            }
        }

    }

    post {
        always {
            echo "🧹 Cleanup..."
            sh 'rm -f ${HTTP_LOG} || true'
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