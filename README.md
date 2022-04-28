## What is this repo doing
This repo spins up a WatchService to watch the file change under `/vault/secrets/`.
When any change happens, it will use `ContextRefresher` to refresh spring event.

## Prerequisites
- brew install colima
- brew install vault
- docker pull postgres

## Execution script
~~~shell
#start vault and login
vault server -dev

export VAULT_ADDR='http://127.0.0.1:8200'

vault login

#run database as docker
docker run --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=root -d postgres

vault secrets enable database

vault write database/config/postgresql \
    plugin_name=postgresql-database-plugin \
    connection_url="postgresql://{{username}}:{{password}}@localhost:5432/postgres?sslmode=disable" \
    allowed_roles=readonly \
    username="postgres" \
    password="root"
    
vault write database/roles/readonly \
      db_name=postgresql \
      creation_statements=@readonly.sql \
      default_ttl=30s \
      max_ttl=30s
      
vault policy write my-role-policy policy.hcl

vault auth enable kubernetes

#use colima to start k8s
colima start --cpu 2 --memory 4 --disk 10 --with-kubernetes

#replace the k8s host and cert to your local configuration
vault write auth/kubernetes/config \
   kubernetes_host=https://127.0.0.1:6443 \
   kubernetes_ca_cert=@/tmp/config/ca.crt 
   
vault write auth/kubernetes/role/myapp \
   bound_service_account_names='*' \
   bound_service_account_namespaces='*' \
   policies=my-role-policy \
   ttl=1h
   
helm repo add hashicorp https://helm.releases.hashicorp.com

helm upgrade -i -n kube-system vault hashicorp/vault \
    --set "injector.externalVaultAddr=http://host.docker.internal:8200"

kubectl create ns demo

#make sure image in built
./gradlew jibDockerBuild

kubect apply -f app.yaml -n demo
~~~

## How to build image
run below command to build the image to you local Docker daemon
~~~shell
./gradlew jibDockerBuild
~~~
- The docker image's name will be your gradle `rootProject.name` which is `play`.
- The docker image's tag will be your gradle `version` which is `0.0.1-SNAPSHOT`.