properties([
    parameters([
        booleanParam(defaultValue: true, description: 'Do you want to run terrform apply', name: 'terraform_apply'),
        booleanParam(defaultValue: false, description: 'Do you want to run terrform destroy', name: 'terraform_destroy'),
        choice(choices: ['dev', 'qa', 'prod'], description: '', name: 'environment'),
        string(defaultValue: '', description: 'Provide AMI ID', name: 'ami_id', trim: false)
    ])
])
def aws_region_var = ''
if(params.environment == "dev"){
    aws_region_var = "us-east-1"
}
else if(params.environment == "qa"){
    aws_region_var = "us-east-2"
}
else if(params.environment == "prod"){
    aws_region_var = "us-west-2"
}
def tf_vars = """
    s3_bucket = \"terrafor-state-april-class-okan\"
    s3_folder_project = \"terraform_ec2\"
    s3_folder_region = \"us-east-1\"
    s3_folder_type = \"class\"
    s3_tfstate_file = \"infrastructure.tfstate\"
    environment = \"${params.environment}\"
    region      = \"${aws_region_var}\"
    public_key  = \"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtSOr+iGtMnmcf1jnQNx2wWuxBLN9qYYgEDWOp2QpCLnFxJ/y4R0Af0hFbuHlF6xi63nUcerYZb4Ad8o5HUm70O/6ng5j+6AGambDt4M6iV+OukdlPsVCiSUswXOIY1/d4mkwKQvSb1DMenKqqixHf48p47CmkncSDucsWEQZNHYSpxWBOUpEzxmVfBWXFrWGUttb//kIvPOAa0uFQM7QdN5J5Xcl0aeXyDY4PvijVYuVO8lFsYLKnqwRQXsfTc7FSOo+dTg/FLj0tNHoL6FtOlmxY6W1LJGlv7XXU95C4JtL9RQ61+OMppLq/i34BXqQhIJNbRiOhmfppgl6v6KBp okandamar@Okans-MacBook-Pro.local\"
    ami_id      = \"${params.ami_id}\"
"""
node{
    stage("Pull Repo"){
        cleanWs()
        git url: 'https://github.com/okandamar0610/terraform-ec2.git'
    }
    withCredentials([usernamePassword(credentialsId: 'jenkins-aws-access-key', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["AWS_REGION=${aws_region_var}"]) {
            stage("Terrraform Init"){
                writeFile file: "${params.environment}.tfvars", text: "${tf_vars}"
                sh """
                    bash setenv.sh ${environment}.tfvars
                    terraform-0.13 init
                """
            }        
            if (terraform_apply.toBoolean()) {
                stage("Terraform Apply"){
                    sh """
                        terraform-0.13 apply -var-file ${environment}.tfvars -auto-approve
                    """
                }
            }
            else if (terraform_destroy.toBoolean()) {
                stage("Terraform Destroy"){
                    sh """
                        terraform-0.13 destroy -var-file ${environment}.tfvars -auto-approve
                    """
                }
            }
            else {
                stage("Terraform Plan"){
                    sh """
                        terraform-0.13 plan -var-file ${environment}.tfvars
                    """
                }
            }
        }        
    }    
}