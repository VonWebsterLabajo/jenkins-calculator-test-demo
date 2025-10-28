pipeline {
  agent any

  // Automatically trigger when a GitHub push event occurs
  triggers {
    githubPush()
  }

  environment {
    // Optional: expose repo name or other useful variables
    REPO_URL = 'https://github.com/VonWebsterLabajo/jenkins-calculator-demo.git'
  }

  stages {
    stage('Checkout') {
      steps {
        echo '📥 Checking out source code...'
        git(
          branch: 'main',
          url: "${REPO_URL}",
          credentialsId: 'GITHUB_PAT'
        )
      }
    }

    stage('Initialize') {
      steps {
        echo '⚙️ Initialize stage...'
      }
    }

    stage('Build') {
      steps {
        echo '🏗️ Building project...'
        // Example: sh 'npm install' or 'mvn clean package'
      }
    }

    stage('Test') {
      steps {
        echo '🧪 Running tests...'
        // Example: sh 'npm test' or 'pytest tests/'
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