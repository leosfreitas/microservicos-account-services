pipeline {
    agent any
    
    environment {
        SERVICE = 'account'
        NAME = "leosfreitas/${env.SERVICE}"
        AWS_REGION = "us-east-2"
        EKS_CLUSTER = "eks-store"
        KUBE_NAMESPACE = "default"
    }
    
    stages {
        stage('Dependencies') {
            steps {
                build job: 'account', wait: true
            }
        }
        
        stage('Build') { 
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        
        stage('Build & Push Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credential', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                    sh "docker login -u $USERNAME -p $TOKEN"
                    sh "docker buildx create --use --platform=linux/arm64,linux/amd64 --node multi-platform-builder-${env.SERVICE} --name multi-platform-builder-${env.SERVICE}"
                    sh "docker buildx build --platform=linux/arm64,linux/amd64 --push --tag ${env.NAME}:latest --tag ${env.NAME}:${env.BUILD_ID} -f Dockerfile ."
                    sh "docker buildx rm --force multi-platform-builder-${env.SERVICE}"
                }
            }
        }
        
        stage('Deploy to EKS') {
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    script {
                        sh """
                            echo "Configurando AWS CLI..."
                            aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
                            aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
                            aws configure set region ${AWS_REGION}
                            
                            echo "Conectando ao cluster EKS..."
                            aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER}
                            
                            echo "Fazendo deploy da imagem ${env.NAME}:${env.BUILD_ID}..."
                            kubectl set image deployment/${SERVICE} ${SERVICE}=${env.NAME}:${env.BUILD_ID} -n ${KUBE_NAMESPACE}
                            
                            echo "Aguardando rollout completar..."
                            kubectl rollout status deployment/${SERVICE} -n ${KUBE_NAMESPACE} --timeout=600s
                            
                            echo "Verificando status dos pods..."
                            kubectl get pods -l app=${SERVICE} -n ${KUBE_NAMESPACE}
                        """
                    }
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    sh """
                        echo "=== STATUS DO DEPLOYMENT ==="
                        kubectl get deployment ${SERVICE} -n ${KUBE_NAMESPACE}
                        
                        echo "=== PODS EM EXECUÇÃO ==="
                        kubectl get pods -l app=${SERVICE} -n ${KUBE_NAMESPACE}
                        
                        echo "=== SERVIÇOS ==="
                        kubectl get services -n ${KUBE_NAMESPACE}
                        
                        echo "=== VERIFICANDO SAÚDE DA APLICAÇÃO ==="
                        kubectl describe deployment ${SERVICE} -n ${KUBE_NAMESPACE}
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo "✅ Deploy do ${SERVICE} realizado com sucesso!"
            echo "🚀 Imagem deployada: ${env.NAME}:${env.BUILD_ID}"
        }
        failure {
            echo "❌ Deploy do ${SERVICE} falhou!"
            sh "kubectl rollout undo deployment/${SERVICE} -n ${KUBE_NAMESPACE}"
            echo "🔄 Rollback executado para versão anterior"
        }
        always {
            cleanWs()
        }
    }
}