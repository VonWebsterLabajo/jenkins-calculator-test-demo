pipeline {
    agent any

    stages {
        stage('Initialize') {
            steps {
                echo 'Webhook from GitHub received!'
            }
        }

        stage('Build') {
            steps {
                echo 'Building Repo B...'
            }
        }

        stage('Test') {
            steps {
                echo 'Yey! It\'s working!'
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline finished successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}