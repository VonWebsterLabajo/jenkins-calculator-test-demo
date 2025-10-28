pipeline {
  agent any

  triggers {
    githubPush()
  }

  environment {
    // Optional: you can still reference this if needed
    REPO_URL = 'https://github.com/VonWebsterLabajo/jenkins-calculator-demo.git'
  }

  stages {
    stage('Build') {
      steps {
        echo '🔧 Building project...'
        // Build steps here
      }
    }

    stage('Test') {
      steps {
        echo '🧪 Running tests...'
        // Test steps here
      }
    }

    stage('Deploy') {
      steps {
        echo '🚀 Deploying...'
        // Deployment steps here
      }
    }
  }
}