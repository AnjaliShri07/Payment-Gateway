pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git(
                    url: 'https://github.com/AnjaliShri07/Payment-Gateway.git',
                    credentialsId: 'githubtoken',
                    branch: 'main'
                )
            }
        }

        stage('Build Services') {
            parallel {
                stage('Build auth') {
                    steps {
                        dir('auth') {
                            sh 'mvn clean install'
                        }
                    }
                }
                stage('Build user') {
                    steps {
                        dir('user') {
                            sh 'mvn clean install'
                        }
                    }
                }
            }
        }

        stage('Test Services') {
            parallel {
                stage('Test auth') {
                    steps {
                        dir('auth') {
                            sh 'mvn test'
                        }
                    }
                }
                stage('Test user') {
                    steps {
                        dir('user') {
                            sh 'mvn test'
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
        					// Build and push Docker images
        					dir('auth') {
        						sh 'docker build -t myregistry/service-a:latest .'
        						sh 'docker push myregistry/service-a:latest'
        						sh 'kubectl apply -f k8s/deployment.yaml'
        					}
        					dir('user') {
        						sh 'docker build -t myregistry/service-b:latest .'
        						sh 'docker push myregistry/service-b:latest'
        						sh 'kubectl apply -f k8s/deployment.yaml'
        					}
        				}
            }
        }
    }
}
