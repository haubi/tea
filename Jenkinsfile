pipeline {
    options {
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr:'5'))
        disableConcurrentBuilds(abortPrevious: true)
    }
    agent any
    tools {
        maven 'apache-maven-3.9.9'
        jdk 'temurin-jdk17-latest'
    }
    stages {
        stage('Build') {
            steps {
                sh '''
                set -xeu
                withOfficialSuffix=
                if [[ ${TAG_NAME-} == v* ]] \
                && [[ ${JOB_NAME-} == "continuous/${TAG_NAME-}" ]] \
                && [[ $(git rev-parse refs/tags/${TAG_NAME-}) == "$(git rev-parse HEAD)" ]] \
                ; then
                    withOfficialSuffix='-DunofficialSuffix='
                elif [[ ${BRANCH_NAME-} == master ]] \
                && [[ ${JOB_NAME-} == "continuous/${BRANCH_NAME-}" ]] \
                && [[ $(git rev-parse refs/remotes/origin/${BRANCH_NAME-}) == "$(git rev-parse HEAD)" ]] \
                ; then
                    withOfficialSuffix='-DunofficialSuffix='
                fi
                mvn -B clean verify ${withOfficialSuffix}
                ! test -r ./p2
                mv sites/org.eclipse.tea.repository/target/repository p2
                test -r ./p2/.
                '''
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
