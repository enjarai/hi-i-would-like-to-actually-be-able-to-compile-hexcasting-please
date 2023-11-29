#!/usr/bin/env groovy

pipeline {
    agent any
    tools {
        jdk "jdk-17.0.1"
    }
    environment {
        discordWebhook = credentials('discordWebhook')
        CURSEFORGE_TOKEN = credentials('curseforgeApiKey')
        MODRINTH_TOKEN = credentials('modrinthApiKey')
    }
    stages {
        stage('Clean') {
            steps {
                echo 'Cleaning Project'
                sh 'chmod +x gradlew'
                sh './gradlew clean'
            }
        }
        stage('Build') {
            steps {
                echo 'Building'
                sh './gradlew build'
            }
        }
        stage('Run Datagen') {
            steps {
                echo 'Running datagen tasks'
                sh './gradlew runAllDatagen'
            }
        }
        stage('Check Datagen') {
            when {
                // cache isn't reproducible, so ignore modifications to it
                // https://stackoverflow.com/a/71878316
                changeset pattern: '^(?!.*generated/resources/.cache).*', comparator: 'REGEXP'
            }
            steps {
                error('Build contains changes after finishing the runAllDatagen task. Run the datagen locally and commit/push the updated files.')
            }
        }
        stage('Publish') {
            when {
                anyOf {
                    branch 'main'
                }
            }
            stages {
                stage('Deploy Previews') {
                    steps {
                        echo 'Deploying previews to various places'
                        sh './gradlew publish publishToDiscord'
                    }
                }
                stage('Deploy releases') {
                    steps {
                        echo 'Maybe deploy releases'
                        sh './gradlew publishCurseforge publishModrinth'
                    }
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts 'Common/build/libs/**.jar'
            archiveArtifacts 'Forge/build/libs/**.jar'
            archiveArtifacts 'Fabric/build/libs/**.jar'
        }
    }
}

