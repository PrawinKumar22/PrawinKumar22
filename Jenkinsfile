
pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    tools {
        maven 'Maven3'
    }

    environment {
        DOCKER_IMAGE = 'prawinkumar22/java-app'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('SCM Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                dir('projects/java-app') {
                    sh 'mvn clean package -DskipTests=false'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('projects/java-app') {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar \
                              -Dsonar.projectKey=java-app \
                              -Dsonar.projectName=java-app
                        '''
                    }
                }
            }
        }

        stage('Container Build') {
            steps {
                dir('projects/java-app') {
                    sh 'docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} .'
                }
            }
        }

        stage('Container Upload') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        set +x
                        printf '%s' "$DOCKER_PASS" |
                            docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                        docker logout
                    '''
                }
            }
        }

        stage('Update GitOps Manifest') {
            steps {
                withCredentials([
                    gitUsernamePassword(
                        credentialsId: 'github-creds',
                        gitToolName: 'Default'
                    )
                ]) {
                    sh '''
                        echo "Updating GitOps deployment manifest..."

                        sed -i "s|^[[:space:]]*image:.*|          image: ${DOCKER_IMAGE}:${IMAGE_TAG}|" \
                            gitops/java-app/deployment.yaml

                        git config user.name "Jenkins CI"
                        git config user.email "jenkins@localhost"

                        git add gitops/java-app/deployment.yaml

                        if git diff --cached --quiet; then
                            echo "No GitOps manifest changes detected."
                        else
                            git commit -m "Update java-app image to ${IMAGE_TAG}"
                            git push origin HEAD:main
                        fi

                        echo "GitOps manifest update completed."
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully! Published ${DOCKER_IMAGE}:${IMAGE_TAG}"
            echo "GitOps manifest updated for Argo CD deployment."
        }
        failure {
            echo 'Pipeline failed. Check the stage logs.'
        }
        always {
            echo "Build number: ${BUILD_NUMBER}"
        }
    }
}
