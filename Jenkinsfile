pipeline {
  agent any

  // 🔔 Trigger when GitHub sends a push event
  triggers {
    githubPush()
  }

  stages {
    stage('Clone Repo A') {
      steps {
        echo '📦 Cloning Repo A (app repo)...'
        git branch: 'main',
          url: 'https://github.com/VonWebsterLabajo/jenkins-calculator-demo.git',
          credentialsId: 'GITHUB_PAT'
      }
    }

    stage('Run Tests from Repo B') {
      steps {
        echo '🧪 Running tests from Repo B...'
        // Example commands:
        // sh 'npm install && npm test'
        // or python, maven, etc.
      }
    }
  }

  post {
    success {
      echo '✅ Tests passed successfully!'
    }
    failure {
      echo '❌ Tests failed!'
    }
  }
}