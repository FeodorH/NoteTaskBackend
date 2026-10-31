pipeline {
    agent any

    environment {
        SPRING_PROFILE_DEPLOY = 'prod'
        SPRING_PROFILE_DEV = 'dev'
        DEPLOY_BRANCH = 'master'

        // Docker
        REGISTRY = 'docker.io'
        IMAGE = 'feodorh/notetasks'
        LATEST_TAG = 'latest'

        // Spring
        CONTAINER_NAME = 'notetask'
        HOST_PORT = '8001'
        CONTAINER_PORT = '8080'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('SetVersion') {
            steps {
                script {
                    env.VERSION = sh(script: './gradlew -q printVersion', returnStdout: true).trim()
                    echo "Version from Gradle: ${env.VERSION}"
                }
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew build'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'build/libs/*.jar'
            }
        }

        stage('Docker Build & Push') {
            when {
                expression { env.BRANCH_NAME == 'develop' || env.BRANCH_NAME == DEPLOY_BRANCH }
            }
            steps {
                script{
                    env.DOCKER_TAG = "v." + env.VERSION + "-" + env.BRANCH_NAME
                }
                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker build -t ${REGISTRY}/${IMAGE}:${env.DOCKER_TAG} .
                        docker push ${REGISTRY}/${IMAGE}:${env.DOCKER_TAG}

                        echo "Current branch: ${env.BRANCH_NAME}"
                        if [ "${env.BRANCH_NAME}" = "${DEPLOY_BRANCH}" ]; then
                            echo "Tagging and pushing latest"
                            docker tag ${REGISTRY}/${IMAGE}:${env.DOCKER_TAG} ${REGISTRY}/${IMAGE}:${LATEST_TAG}
                            docker push ${REGISTRY}/${IMAGE}:${LATEST_TAG}
                        fi
                    """
                }
            }
        }

        stage('Deploy') {
            when {
                expression { env.BRANCH_NAME == DEPLOY_BRANCH } //TODO !!!
            }
            steps {
                withCredentials([
                    string(credentialsId: 'notetask-api-key-id', variable: 'APP_API_KEY'),
                    string(credentialsId: 'gigachat-client-id', variable: 'GIGACHAT_CLIENT_ID'),
                    string(credentialsId: 'gigachat-client-secret', variable: 'GIGACHAT_CLIENT_SECRET')
                ]) {
                    sh """
                        docker pull ${REGISTRY}/${IMAGE}:${env.LATEST_TAG}
                        docker stop ${CONTAINER_NAME} || true
                        docker rm ${CONTAINER_NAME} || true
                        docker run -d \\
                            --name ${CONTAINER_NAME} \\
                            -p ${HOST_PORT}:${CONTAINER_PORT} \\
                            -e SPRING_PROFILES_ACTIVE=\${SPRING_PROFILES_ACTIVE} \\
                            -e APP_API_KEY=\${APP_API_KEY} \\
                            -e GIGACHAT_CLIENT_ID=\${GIGACHAT_CLIENT_ID} \\
                            -e GIGACHAT_CLIENT_SECRET=\${GIGACHAT_CLIENT_SECRET} \\
                            --restart unless-stopped \\
                            ${REGISTRY}/${IMAGE}:${env.LATEST_TAG}
                        sleep 5
                        docker ps | grep ${CONTAINER_NAME} || (echo "Container failed to start" && exit 1)
                    """
                }
            }
        }

    }

    post {
        always { cleanWs() }
        success { echo "✅ Build succeeded for branch ${env.BRANCH_NAME}" }
        failure { echo "❌ Build failed for branch ${env.BRANCH_NAME}" }
    }
}