pipeline {
    options {
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr:'5'))
        disableConcurrentBuilds(abortPrevious: true)
    }
    agent any
    tools {
        maven 'apache-maven-latest'
        jdk 'temurin-jdk17-latest'
    }
    stages {
        stage('Build') {
            steps {
                sh """
                set -xe
                mvn clean verify
                ! test -r ./p2
                mv sites/org.eclipse.tea.repository/target/repository p2
                test -r ./p2/.
                """
            }
            post {
                success {
                    archiveArtifacts artifacts: 'p2/'
                }
                always {
                    recordIssues tool: mavenConsole()
                }
            }
        }
    }
}
