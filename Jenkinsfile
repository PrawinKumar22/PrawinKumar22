
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

        stage('Deploy to Kubernetes') {
            steps {
                dir('projects/java-app') {
                    withKubeConfig([credentialsId: 'kind-kubeconfig']) {
                        sh '''
                            echo "Deploying application to Kind Kubernetes..."

                            sed "s|\\${DOCKER_IMAGE}|${DOCKER_IMAGE}|g; s|\\${IMAGE_TAG}|${IMAGE_TAG}|g" deployment.yaml | kubectl apply -f -

                            kubectl rollout status deployment/java-app \
                              -n default --timeout=180s
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully! Published ${DOCKER_IMAGE}:${IMAGE_TAG}"
        }
        failure {
            echo 'Pipeline failed. Check the stage logs.'
        }
        always {
            echo "Build number: ${BUILD_NUMBER}"
        }
    }
}
